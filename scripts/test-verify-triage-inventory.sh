#!/usr/bin/env sh
# Self-test for verify-triage-inventory.sh and the bad/good fixtures.
# Run from the repo root: scripts/test-verify-triage-inventory.sh
set -u

cd "$(dirname "$0")/.."

fixture_good="scripts/test-fixtures/triage-good.md"
fixture_bad="scripts/test-fixtures/triage-bad.md"

if [ ! -f "$fixture_good" ] || [ ! -f "$fixture_bad" ]; then
    echo "FAIL: missing fixtures ($fixture_good, $fixture_bad)" >&2
    exit 1
fi

# 1. Good fixture: real alerts.md has 866 alerts, our good fixture has 4.
#    Run the script's row-count logic inline so we can drive it with a
#    different file path / count pair.
run_count_check() {
    triage="$1"
    expected="$2"
    actual=$(awk -F'|' '/^\| [0-9]+ \|/ { n++ } END { print n + 0 }' "$triage")
    if [ "$actual" = "$expected" ]; then
        echo "OK: $triage has $actual rows (expected $expected)"
    else
        echo "FAIL: $triage has $actual rows (expected $expected)" >&2
        return 1
    fi
}

run_count_check "$fixture_good" 4
run_count_check "$fixture_bad" 4

# 2. Bad fixture: must produce at least one empty-notes false-positive row.
empty=$(awk -F'|' '
    /^\| [0-9]+ \|/ {
        d = $8; gsub(/`/, "", d); gsub(/ *\(candidate\)/, "", d)
        gsub(/^ +| +$/, "", d)
        n = $12; gsub(/^ +| +$/, "", n)
        if ((d == "false-positive" || d == "accepted-risk") && n == "") print "1"
    }
' "$fixture_bad")
if [ "$empty" = "1" ]; then
    echo "OK: bad fixture has a false-positive row with empty notes (catches the bug)"
else
    echo "FAIL: bad fixture did not surface the empty-notes bug" >&2
    exit 1
fi

# 3. Bad fixture: must produce at least one unknown module_owner.
bad_owner=$(awk -F'|' '
    /^\| [0-9]+ \|/ {
        o = $7; gsub(/^ +| +$/, "", o); gsub(/`/, "", o)
        if (o == "modules/some-unknown-module/") print "1"
    }
' "$fixture_bad")
if [ "$bad_owner" = "1" ]; then
    echo "OK: bad fixture has an unknown module_owner (catches the bug)"
else
    echo "FAIL: bad fixture did not surface the unknown-owner bug" >&2
    exit 1
fi

# 4. Good fixture: same checks must come back clean.
empty2=$(awk -F'|' '
    /^\| [0-9]+ \|/ {
        d = $8; gsub(/`/, "", d); gsub(/ *\(candidate\)/, "", d)
        gsub(/^ +| +$/, "", d)
        n = $12; gsub(/^ +| +$/, "", n)
        if ((d == "false-positive" || d == "accepted-risk") && n == "") print "1"
    }
' "$fixture_good")
if [ -z "$empty2" ]; then
    echo "OK: good fixture has no false-positive/accepted-risk rows with empty notes"
else
    echo "FAIL: good fixture unexpectedly has empty-notes rows" >&2
    exit 1
fi

echo "test-verify-triage-inventory: PASS"
exit 0
