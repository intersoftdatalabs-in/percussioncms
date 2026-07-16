#!/usr/bin/env sh
# verify-distribution-archive.sh (T019, feature 004).
#
# Rebuilds modules/perc-distribution-tree and verifies that none of the
# files listed in the obsolete removal set (the "removed_files.txt"
# inventory) appear in the resulting JARs or the .ppkg installer archive.
#
# Usage:
#   scripts/verify-distribution-archive.sh [removed_files.txt]
#
# Default removed_files.txt:
#   tmp/gh-codeql-alerts/removed-files.txt
#
# Returns 0 if the build succeeds AND none of the removed file basenames
# appear in the rebuilt archives. Returns 1 on any failure.
#
# Requires: JDK 21 (the repo mandates JDK 21 for the build per
# AGENTS.md), mvn on PATH, unzip. The script honors the same
# environment overrides as the manual ./mvn-env.sh wrapper:
#   JAVA_HOME_21 — path to a JDK 21 install; overrides the JDK on PATH
#   MAVEN        — path to the mvn executable; defaults to "mvn"
set -eu

repo_root="$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_root"

removed_list="${1:-tmp/gh-codeql-alerts/removed-files.txt}"

if [ ! -f "$removed_list" ]; then
    echo "FAIL: removed-files inventory not found: $removed_list" >&2
    exit 1
fi

# Build the distribution tree and assemble the .ppkg. The repo's
# AGENTS.md mandates JDK 21 for the build; we honor the JAVA_HOME_21
# env var (the same override the manual ./mvn-env.sh wrapper uses)
# and fall back to whatever JDK is on PATH. We invoke `mvn` directly
# rather than ./mvn-env.sh because the wrapper has known issues on
# some Windows shells (mv: cannot move across filesystems).
mvn_cmd() {
    if [ -n "${JAVA_HOME_21:-}" ] && [ -d "${JAVA_HOME_21}" ]; then
        JAVA_HOME="${JAVA_HOME_21}" "${MAVEN:-mvn}" -Djava.io.tmpdir=tmp "$@"
    else
        "${MAVEN:-mvn}" -Djava.io.tmpdir=tmp "$@"
    fi
}

echo "==> clean package of modules/perc-distribution-tree (with deps)"
mvn_cmd -pl modules/perc-distribution-tree -am -DskipTests clean package

fail=0
# Check every JAR produced under modules/perc-distribution-tree/target/...
echo "==> checking JARs for any removed file basenames"
while IFS= read -r removed; do
    case "$removed" in
        ""|\#*) continue ;;
    esac
    base=$(basename "$removed")
    found_in=""
    for jar in $(find modules/perc-distribution-tree/target modules/perc-packages/target -name '*.jar' 2>/dev/null); do
        if unzip -l "$jar" 2>/dev/null | grep -F "$base" >/dev/null 2>&1; then
            found_in="$found_in $jar"
        fi
    done
    if [ -n "$found_in" ]; then
        echo "  FAIL: $base still present in:$found_in"
        fail=1
    fi
done < "$removed_list"

# Check the .ppkg (if produced) the same way.
echo "==> checking .ppkg for any removed file basenames"
for ppkg in $(find . -name '*.ppkg' 2>/dev/null); do
    while IFS= read -r removed; do
        case "$removed" in
            ""|\#*) continue ;;
        esac
        base=$(basename "$removed")
        if unzip -l "$ppkg" 2>/dev/null | grep -F "$base" >/dev/null 2>&1; then
            echo "  FAIL: $base still present in $ppkg"
            fail=1
        fi
    done < "$removed_list"
done

if [ "$fail" -ne 0 ]; then
    echo "verify-distribution-archive: FAIL" >&2
    exit 1
fi
echo "verify-distribution-archive: PASS"
exit 0
