#!/usr/bin/env sh
# check-no-glob-deletes.sh — Static assertion that the install/upgrade ANT
# script does not delete integrator-supplied JDBC drivers via glob patterns.
#
# Inspects modules/perc-distribution-tree/src/main/resources/distribution/
# rxconfig/Installer/install.xml — specifically the <delete> block inside the
# <target name="install_jdbc_drivers"> element — and asserts that every
# <include name="..."> value is an exact bundled-driver filename, not a glob.
#
# Companion to InstallXmlDeleteSetTest (which does the same check via JUnit
# XPath on the parsed XML). This script is wired into the Maven verify phase
# so a glob-based <delete> introduced by a future change fails the build
# before review, with a single-line error pointing to the offending entry.
#
# Exit codes:
#   0  no glob-based <delete> patterns found
#   1  invocation error / missing tool
#   7  one or more <include> entries inside install_jdbc_drivers <delete> are
#      glob patterns (contain '*' or '?') — this is the failure the script
#      exists to catch
#
# For feature 002-jdbc-drivers-cleanup (FR-003, FR-008.b, SC-006).

set -u

SCRIPT_NAME=$(basename "$0")
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE_DIR=$(cd "$SCRIPT_DIR/.." && pwd)

# Defaults
INSTALL_XML="$MODULE_DIR/src/main/resources/distribution/rxconfig/Installer/install.xml"

usage() {
    cat <<EOF
Usage: $SCRIPT_NAME [--install-xml <path>]

Options:
  --install-xml <path>   Path to install.xml
                         (default: $INSTALL_XML)

Exit codes:
  0  ok
  1  invocation error
  7  glob-based <delete> patterns found in install_jdbc_drivers
EOF
}

# Arg parsing
while [ $# -gt 0 ]; do
    case "$1" in
        --install-xml) INSTALL_XML="$2"; shift 2 ;;
        -h|--help) usage; exit 0 ;;
        *) echo "ERROR: unknown argument: $1" >&2; usage >&2; exit 1 ;;
    esac
done

# Required tools
for tool in awk grep sed; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "ERROR: required tool '$tool' not found on PATH" >&2
        exit 1
    fi
done

if [ ! -f "$INSTALL_XML" ]; then
    echo "ERROR: install.xml not found: $INSTALL_XML" >&2
    exit 1
fi

# Strategy: scope to ONLY the <delete> block inside the
# <target name="install_jdbc_drivers"> element — not the whole target. The
# target also contains <copy> blocks (which legitimately use globs to copy
# legacy AppServer drivers and the bundled set), and we don't want to fail
# the build on those. We use a two-pass extraction:
#   1. find the <delete> element that lives inside the install_jdbc_drivers
#      target (not the other <delete>s elsewhere in install.xml);
#   2. within that <delete> block, find every <include name="..."> value
#      and assert none contain '*' or '?'.
#
# Pure POSIX (no python/jq) so it works in the same minimal environment as
# verify-jdbc-drivers.sh.

START=$(grep -n '<target name="install_jdbc_drivers"' "$INSTALL_XML" | head -1 | cut -d: -f1)
if [ -z "$START" ]; then
    echo "ERROR: <target name=\"install_jdbc_drivers\"> not found in $INSTALL_XML" >&2
    exit 1
fi
END=$(awk -v start="$START" 'NR > start && /<\/target>/ { print NR; exit }' "$INSTALL_XML")
if [ -z "$END" ]; then
    echo "ERROR: closing </target> not found after line $START in $INSTALL_XML" >&2
    exit 1
fi

# Extract the slice for the install_jdbc_drivers target and find the first
# <delete ...> open tag within it. Then everything up to its </delete> close
# tag is the block to inspect. This is deliberately a narrow grep rather than
# an XML parser, to keep the script's dependencies to awk/grep/sed.
TARGET_RANGE=$(sed -n "${START},${END}p" "$INSTALL_XML")
DELETE_START_LINE=$(printf '%s\n' "$TARGET_RANGE" | grep -nE '<delete[[:space:]]' | head -1 | cut -d: -f1)
if [ -z "$DELETE_START_LINE" ]; then
    echo "ERROR: no <delete> element found inside <target name=\"install_jdbc_drivers\">" >&2
    exit 1
fi
# Adjust line offset: TARGET_RANGE is the slice starting at $START in the
# original file; DELETE_START_LINE is its line within the slice. Convert to
# the absolute line number in the original file.
ABS_DELETE_START=$((START + DELETE_START_LINE - 1))
ABS_DELETE_END=$(awk -v start="$ABS_DELETE_START" 'NR > start && /<\/delete>/ { print NR; exit }' "$INSTALL_XML")
if [ -z "$ABS_DELETE_END" ]; then
    echo "ERROR: closing </delete> not found after line $ABS_DELETE_START" >&2
    exit 1
fi

DELETE_BLOCK=$(sed -n "${ABS_DELETE_START},${ABS_DELETE_END}p" "$INSTALL_XML")

# Extract <include name="..."> values from the <delete> block and flag any
# containing '*' or '?'. Permissive regex tolerates single or double quotes
# and extra whitespace, matching exactly how ANT XML is written here.
GLOBS=$(printf '%s\n' "$DELETE_BLOCK" \
    | grep -E '<include[[:space:]]+name=' \
    | sed -E 's/.*<include[[:space:]]+name=["'"'"']([^"'"'"']+)["'"'"'].*/\1/' \
    | grep -E '[\*\?]' || true)

if [ -n "$GLOBS" ]; then
    echo "ERROR: glob-based <delete> patterns found in install_jdbc_drivers target of install.xml:" >&2
    # Print GLOBS line-by-line, preserving every byte. We do NOT use
    # 'printf '  %s\n' $GLOBS' here: unquoted $GLOBS undergoes word-splitting
    # AND pathname expansion, which would corrupt values containing '*' or '?'
    # (the very characters that triggered this failure) by matching files in
    # the current working directory.
    printf '%s\n' "$GLOBS" | while IFS= read -r g; do printf '  %s\n' "$g"; done >&2
    echo "Fix: replace each glob with the exact bundled-driver filename (see BundledJdbcDrivers constant in the test sources)." >&2
    exit 7
fi

echo "OK: install_jdbc_drivers <delete> uses exact filenames only; no glob patterns found"
exit 0
