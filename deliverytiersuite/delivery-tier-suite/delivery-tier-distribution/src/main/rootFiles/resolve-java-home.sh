#!/bin/bash
# Shared runtime Java home resolver for Percussion CMS / DTS start, stop, and
# service install paths. See specs/991-system-java-home/contracts/java-home-resolution.md
# for the algorithm and contracts/java-properties-contract.md for the input format.
#
# Usage:  source resolve-java-home.sh [<install_root>]
#         # After sourcing: $JAVA_HOME, $JAVA, $RESOLVE_SOURCE are populated
#         # When resolution fails the script exits non-zero with an actionable
#         # error message that mentions the required major version (21) and the
#         # sources tried.
#
# Sourced (not executed) so callers can inherit JAVA_HOME / JAVA as shell
# variables. Intentionally minimal — no external commands beyond `java`,
# POSIX `read`, `command`, and `dirname`.

set +u

REQUIRED_MAJOR=21
RESOLVE_ERRORS=()
RESOLVE_SOURCE=""

# Determine install root. The caller may pass the install root as the first
# argument; otherwise we use the directory of the caller.
_resolve_install_root() {
    if [ -n "${1-}" ]; then
        echo "$1"
        return
    fi
    # Caller-of-caller directory (this script is sourced, so BASH_SOURCE[1] is the caller).
    local src="${BASH_SOURCE[1]:-$0}"
    local dir
    dir=$(cd "$(dirname "$src")" && pwd)
    # For start scripts under <install_root>/jetty, install root is the grandparent.
    echo "$(dirname "$dir")"
}

INSTALL_ROOT="$(_resolve_install_root "${1-}")"
if [ -z "$INSTALL_ROOT" ]; then
    echo "resolve-java-home: cannot determine install root" >&2
    RESOLVE_ERRORS+=("INSTALL_ROOT:<unset>")
    return 1 2>/dev/null || exit 1
fi

_launcher_name() {
    case "$(uname -s 2>/dev/null || echo Unknown)" in
        CYGWIN*|MINGW*|MSYS*) echo "java.exe" ;;
        *) echo "java" ;;
    esac
}

LAUNCHER="$(_launcher_name)"

# Validate a Java home by executing the launcher with -version and parsing major.
# Returns 0 on success and writes the major version to stdout (for diagnostics).
_validate_java_home() {
    local candidate="$1"
    local launcher="$candidate/bin/$LAUNCHER"
    if [ ! -x "$launcher" ] && [ ! -f "$launcher" ]; then
        echo "launcher missing: $launcher" >&2
        return 1
    fi
    local out
    # 2>&1 because java -version writes to stderr.
    out=$("$launcher" -version 2>&1) || true
    local major
    major=$(echo "$out" | tr '\r' '\n' | sed -n 's/.*"\([0-9][0-9]*\).*/\1/p' | head -n1)
    if [ -z "$major" ]; then
        # Legacy 1.8 style: java version "1.8.0_xxx"
        major=$(echo "$out" | tr '\r' '\n' | sed -n 's/.*"1\.\([0-9][0-9]*\).*/\1/p' | head -n1)
    fi
    if [ -z "$major" ]; then
        echo "could not parse major version from: $out" >&2
        return 1
    fi
    if [ "$major" != "$REQUIRED_MAJOR" ]; then
        echo "Java major version $major != required $REQUIRED_MAJOR" >&2
        return 1
    fi
    echo "$major"
    return 0
}

# Source 1: install-root java.properties.
_config_source() {
    local props="$INSTALL_ROOT/java.properties"
    if [ ! -f "$props" ]; then
        RESOLVE_ERRORS+=("PRODUCT_CONFIG:$props:not found")
        return 1
    fi
    local home launcher
    home=$(awk -F= '/^JAVA_HOME[[:space:]]*=/ {sub(/^[[:space:]]+|[[:space:]]+$/,"",$2); print $2; exit}' "$props" 2>/dev/null)
    launcher=$(awk -F= '/^[[:space:]]*JAVA[[:space:]]*=/ {sub(/^[[:space:]]+|[[:space:]]+$/,"",$2); print $2; exit}' "$props" 2>/dev/null)
    if [ -n "$home" ] && _validate_java_home "$home" >/dev/null 2>&1; then
        JAVA_HOME="$home"
        JAVA="${launcher:-$home/bin/$LAUNCHER}"
        RESOLVE_SOURCE="java.properties (PRODUCT_CONFIG)"
        return 0
    fi
    if [ -n "$home" ]; then
        RESOLVE_ERRORS+=("PRODUCT_CONFIG:$home:not a valid Java $REQUIRED_MAJOR home")
    else
        RESOLVE_ERRORS+=("PRODUCT_CONFIG:$props:JAVA_HOME missing")
    fi
    return 1
}

# Source 2: process env JAVA_HOME.
_env_source() {
    if [ -z "${JAVA_HOME:-}" ]; then
        RESOLVE_ERRORS+=("PROCESS_ENV:JAVA_HOME:<unset>")
        return 1
    fi
    if _validate_java_home "$JAVA_HOME" >/dev/null 2>&1; then
        JAVA="$JAVA_HOME/bin/$LAUNCHER"
        RESOLVE_SOURCE="env JAVA_HOME (PROCESS_ENV)"
        return 0
    fi
    RESOLVE_ERRORS+=("PROCESS_ENV:$JAVA_HOME:not a valid Java $REQUIRED_MAJOR home")
    return 1
}

# Source 3: legacy install-dir JRE / JRE64.
_legacy_source() {
    for name in JRE:INSTALL_DIR_JRE JRE64:INSTALL_DIR_JRE64; do
        local dir="${name%:*}"
        local label="${name#*:}"
        local candidate="$INSTALL_ROOT/$dir"
        if [ -d "$candidate" ] && _validate_java_home "$candidate" >/dev/null 2>&1; then
            JAVA_HOME="$candidate"
            JAVA="$candidate/bin/$LAUNCHER"
            RESOLVE_SOURCE="legacy install-dir $dir ($label)"
            return 0
        fi
        if [ -d "$candidate" ]; then
            RESOLVE_ERRORS+=("$label:$candidate:not a valid Java $REQUIRED_MAJOR home")
        fi
    done
    return 1
}

# Source 4: PATH discovery.
_path_source() {
    local saved_ifs="$IFS"
    IFS=':'
    for dir in $PATH; do
        IFS="$saved_ifs"
        [ -z "$dir" ] && continue
        local candidate="$dir/$LAUNCHER"
        if [ -x "$candidate" ]; then
            # Infer home from launcher (../.. of <home>/bin/java).
            local inferred
            inferred=$(cd "$dir/.." 2>/dev/null && pwd)
            if [ -n "$inferred" ] && _validate_java_home "$inferred" >/dev/null 2>&1; then
                JAVA_HOME="$inferred"
                JAVA="$candidate"
                RESOLVE_SOURCE="PATH ($dir)"
                return 0
            fi
        fi
    done
    IFS="$saved_ifs"
    RESOLVE_ERRORS+=("PATH:$LAUNCHER:no valid Java $REQUIRED_MAJOR launcher")
    return 1
}

# Apply precedence. First success wins. On total failure print an actionable
# error that explicitly names major version 21 (helps operator diagnose).
_config_source \
    || _env_source \
    || _legacy_source \
    || _path_source \
    || {
        echo "resolve-java-home: no compatible Java home found." >&2
        echo "Required Java major version: $REQUIRED_MAJOR" >&2
        echo "Install root: $INSTALL_ROOT" >&2
        echo "Sources tried:" >&2
        for e in "${RESOLVE_ERRORS[@]}"; do
            echo "  - $e" >&2
        done
        return 1 2>/dev/null || exit 1
    }

export JAVA_HOME
export JAVA
# RESOLVE_SOURCE is informational; callers can echo it for diagnostics.
export RESOLVE_SOURCE

# Always echo the resolved values for visibility when this script is sourced
# by an interactive StartJetty console wrapper.
echo "JAVA_HOME=$JAVA_HOME"
echo "JAVA=$JAVA"
echo "RESOLVE_SOURCE=$RESOLVE_SOURCE"
