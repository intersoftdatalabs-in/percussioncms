#!/usr/bin/env sh
# verify-jdbc-drivers.sh — Verify that the assembled Percussion distribution artifact
# ships a non-empty jetty/base/lib/jdbc/ directory with valid JDBC driver JARs.
#
# Exit codes:
#   0  all checks passed
#   1  invocation error / missing tool
#   2  jdbc/ missing or empty
#   3  one or more JARs are zero-byte
#   4  one or more JARs are not valid Java archives
#   5  artifact could not be unpacked
#   6  --expected-driver-set does not match what's shipped

set -u

SCRIPT_NAME=$(basename "$0")

usage() {
    cat <<EOF
Usage: $SCRIPT_NAME [--artifact <path>] [--workdir <dir>]
                   [--expected-driver-set <csv>] [--expected-driver-glob <csv>]

Options:
  --artifact <path>             Path to perc-distribution-tree.jar
                                (default: modules/perc-distribution-tree/target/perc-distribution-tree.jar
                                 relative to repo root, resolved from script location)
  --workdir <dir>               Scratch directory for unpacking (default: mktemp -d)
  --expected-driver-set <csv>   Comma-separated exact driver filenames that must be present
                                (default: empty = any non-empty valid set is acceptable)
  --expected-driver-glob <csv>  Comma-separated shell globs; for each glob at least one
                                matching JAR must be present under jetty/base/lib/jdbc/.
                                Version-resilient — use this in CI when driver versions may
                                bump between releases.

Exit codes: see scripts/README.md
EOF
}

# Defaults
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
# scripts/ lives at modules/perc-distribution-tree/scripts/, so the owning module
# is one level up. The repo root is three levels up from scripts/.
MODULE_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../../.." && pwd)
DEFAULT_ARTIFACT="$MODULE_DIR/target/perc-distribution-tree.jar"
ARTIFACT="$DEFAULT_ARTIFACT"
WORKDIR=""
EXPECTED=""
EXPECTED_GLOBS=""

# Arg parsing
while [ $# -gt 0 ]; do
    case "$1" in
        --artifact) ARTIFACT="$2"; shift 2 ;;
        --workdir) WORKDIR="$2"; shift 2 ;;
        --expected-driver-set) EXPECTED="$2"; shift 2 ;;
        --expected-driver-glob) EXPECTED_GLOBS="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "ERROR: unknown argument: $1" >&2; usage >&2; exit 1 ;;
    esac
done

# Required tools
for tool in unzip stat find; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "ERROR: required tool '$tool' not found on PATH" >&2
        exit 1
    fi
done

# Artifact existence
if [ ! -f "$ARTIFACT" ]; then
    echo "ERROR: artifact not found: $ARTIFACT" >&2
    exit 1
fi

# Workdir
if [ -z "$WORKDIR" ]; then
    WORKDIR=$(mktemp -d 2>/dev/null) || { echo "ERROR: mktemp failed" >&2; exit 1; }
    CLEANUP_WORKDIR=1
else
    mkdir -p "$WORKDIR" || { echo "ERROR: cannot create workdir: $WORKDIR" >&2; exit 1; }
    CLEANUP_WORKDIR=0
fi
DIST_ROOT="$WORKDIR/dist"

cleanup() {
    if [ "$CLEANUP_WORKDIR" = "1" ] && [ -n "$WORKDIR" ] && [ -d "$WORKDIR" ]; then
        rm -rf "$WORKDIR"
    fi
}
trap cleanup EXIT

# Unpack
mkdir -p "$DIST_ROOT"
if ! unzip -q "$ARTIFACT" -d "$DIST_ROOT" 2>/dev/null; then
    echo "ERROR: failed to unpack artifact: $ARTIFACT" >&2
    exit 5
fi

JDBC_DIR=""
for candidate in "$DIST_ROOT/jetty/base/lib/jdbc" "$DIST_ROOT/distribution/jetty/base/lib/jdbc"; do
    if [ -d "$candidate" ]; then
        JDBC_DIR="$candidate"
        break
    fi
done
if [ -z "$JDBC_DIR" ]; then
    echo "ERROR: jdbc directory missing: jetty/base/lib/jdbc/ (also tried distribution/jetty/base/lib/jdbc/)" >&2
    exit 2
fi

# Enumerate JARs
JAR_COUNT=0
ZERO_BYTE_COUNT=0
INVALID_COUNT=0
for jar in "$JDBC_DIR"/*.jar; do
    [ -e "$jar" ] || continue   # glob matched nothing
    JAR_COUNT=$((JAR_COUNT + 1))
    name=$(basename "$jar")
    size=$(stat -c '%s' "$jar" 2>/dev/null || stat -f '%z' "$jar" 2>/dev/null || echo 0)
    if [ "$size" = "0" ]; then
        echo "  [FAIL] $name — zero bytes"
        ZERO_BYTE_COUNT=$((ZERO_BYTE_COUNT + 1))
        continue
    fi
    if ! unzip -t "$jar" >/dev/null 2>&1; then
        echo "  [FAIL] $name — not a valid JAR"
        INVALID_COUNT=$((INVALID_COUNT + 1))
        continue
    fi
    echo "  [ OK ] $name — $size bytes"
done

if [ "$JAR_COUNT" -eq 0 ]; then
    echo "ERROR: no JARs found under jetty/base/lib/jdbc/" >&2
    exit 2
fi
if [ "$ZERO_BYTE_COUNT" -gt 0 ]; then
    echo "ERROR: $ZERO_BYTE_COUNT zero-byte JAR(s) found" >&2
    exit 3
fi
if [ "$INVALID_COUNT" -gt 0 ]; then
    echo "ERROR: $INVALID_COUNT invalid JAR(s) found" >&2
    exit 4
fi

# Expected set
if [ -n "$EXPECTED" ]; then
    MISSING=""
    OLD_IFS=$IFS
    IFS=','
    for expected_name in $EXPECTED; do
        IFS=$OLD_IFS
        expected_name=$(echo "$expected_name" | tr -d ' ')
        if [ ! -f "$JDBC_DIR/$expected_name" ]; then
            MISSING="$MISSING $expected_name"
        fi
        IFS=','
    done
    IFS=$OLD_IFS
    if [ -n "$MISSING" ]; then
        echo "ERROR: expected driver(s) missing from jetty/base/lib/jdbc/:$MISSING" >&2
        exit 6
    fi
fi

# Expected glob set (version-resilient). Comma-separated shell globs; for each
# glob at least one matching JAR must be present under the jdbc/ directory.
# Use this instead of --expected-driver-set when version bumps are expected.
if [ -n "$EXPECTED_GLOBS" ]; then
    MISSING_GLOB=""
    OLD_IFS=$IFS
    IFS=','
    for pattern in $EXPECTED_GLOBS; do
        IFS=$OLD_IFS
        pattern=$(echo "$pattern" | tr -d ' ')
        # shellcheck disable=SC2086
        if ! [ -f "$(ls $JDBC_DIR/$pattern 2>/dev/null | head -1)" ]; then
            MISSING_GLOB="$MISSING_GLOB $pattern"
        fi
        IFS=','
    done
    IFS=$OLD_IFS
    if [ -n "$MISSING_GLOB" ]; then
        echo "ERROR: no JAR matched any of expected driver globs:$MISSING_GLOB" >&2
        exit 6
    fi
fi

echo "OK: $JAR_COUNT JDBC driver JAR(s) verified under jetty/base/lib/jdbc/"
exit 0