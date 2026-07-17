#!/usr/bin/env sh
# Self-test for verify-no-jqplot-vendor-refs.sh.
# Run from the repo root: scripts/test-verify-no-jqplot-vendor-refs.sh
#
# Follows the paired-self-test convention already used in this directory
# (e.g. test-verify-triage-inventory.sh for verify-triage-inventory.sh):
# drives the real guard script through its PASS case (current, already-
# clean repo state) and two FAIL cases (a reintroduced vendor directory;
# a stray "jqplot" reference in a tracked file), so a regression in the
# guard script's own detection logic is caught instead of silently
# passing forever.
#
# Any scratch files/directories this test creates are removed on exit
# (via trap, so cleanup still runs on failure/interrupt) and are never
# committed.
set -u

cd "$(dirname "$0")/.."

script="scripts/verify-no-jqplot-vendor-refs.sh"
scratch_dir="tmp/test-verify-no-jqplot-vendor-refs"
mkdir -p "$scratch_dir"
out_pass="$scratch_dir/pass-run.out"
out_reintroduced="$scratch_dir/reintroduced-run.out"
out_strayref="$scratch_dir/strayref-run.out"

reintroduced_dir="WebUI/src/main/webapp/cm/gadgets/repository/common/lib/jqplot"
stray_ref_file="WebUI/src/main/webapp/cm/plugins/perc_test_verify_jqplot_probe.js"

fail=0

cleanup() {
    rm -rf "$reintroduced_dir" 2>/dev/null
    if [ -f "$stray_ref_file" ]; then
        git reset -- "$stray_ref_file" >/dev/null 2>&1
        rm -f "$stray_ref_file"
    fi
    rm -rf "$scratch_dir"
}
trap cleanup EXIT INT TERM

# 1. Current repo state (jqplot genuinely removed) must PASS.
if sh "$script" >"$out_pass" 2>&1; then
    echo "OK: current repo state passes (jqplot vendor lib is actually gone)"
else
    echo "FAIL: $script reports failure against the real, already-clean repo state:" >&2
    cat "$out_pass" >&2
    fail=1
fi

# 2. Simulate a reintroduced vendor directory -> must FAIL.
mkdir -p "$reintroduced_dir"
echo "/* reintroduced for self-test only, removed on exit */" >"$reintroduced_dir/jquery.jqplot.js"
if sh "$script" >"$out_reintroduced" 2>&1; then
    echo "FAIL: $script did not detect a reintroduced vendor directory" >&2
    cat "$out_reintroduced" >&2
    fail=1
else
    echo "OK: $script correctly fails when the vendor directory reappears"
fi
rm -rf "$reintroduced_dir"

# 3. Simulate a stray reference in a tracked file -> must FAIL. `git add`
#    is required (not just writing the file) because git grep, as the
#    real script invokes it, only searches tracked files.
echo "// jqplot (self-test probe, not a real reference)" >"$stray_ref_file"
git add "$stray_ref_file" >/dev/null 2>&1
if sh "$script" >"$out_strayref" 2>&1; then
    echo "FAIL: $script did not detect a stray jqplot reference" >&2
    cat "$out_strayref" >&2
    fail=1
else
    echo "OK: $script correctly fails when a stray jqplot reference is (re)introduced"
fi
git reset -- "$stray_ref_file" >/dev/null 2>&1
rm -f "$stray_ref_file"

if [ "$fail" -ne 0 ]; then
    echo "test-verify-no-jqplot-vendor-refs: FAIL" >&2
    exit 1
fi
echo "test-verify-no-jqplot-vendor-refs: PASS"
exit 0
