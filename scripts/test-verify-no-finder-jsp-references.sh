#!/usr/bin/env sh
# Self-test for verify-no-finder-jsp-references.sh.
# Run from the repo root: scripts/test-verify-no-finder-jsp-references.sh
#
# Follows the paired-self-test convention used in this directory
# (e.g. test-verify-no-jqplot-vendor-refs.sh for
# verify-no-jqplot-vendor-refs.sh): drives the real guard script
# through its PASS case (current, already-clean repo state for the
# modern Track B shell) and FAIL cases (re-introduced finder.jsp
# navigation entry; stray finder_js.jsp false-positive trap), so a
# regression in the guard script's own detection logic is caught
# instead of silently passing forever.
#
# The PASS case asserts the script returns 0 on the current tree
# (cm/app/webmgt.jsp is hard-cut per US6). The FAIL cases assert the
# script returns 1 when (a) a navigation entry is re-introduced and
# (b) the regex doesn't accidentally match the shared-library
# finder_js.jsp include (which is intentionally allowed as a
# carve-out, see the .sh header comment).
#
# Any scratch files/directories this test creates are removed on exit
# (via trap, so cleanup still runs on failure/interrupt) and are never
# committed.
#
# `set -e` exits on the first failure; `set -u` rejects unset
# variables. Without `set -e`, a silent `cp` failure inside the
# backup step would leave us mutating the target JSP without a
# restore path, potentially corrupting it. POSIX-portable: this
# script targets `sh` (not bash) so `set -o pipefail` is not used
# (bash extension; not supported by dash).
set -eu

cd "$(dirname "$0")/.."

script="scripts/verify-no-finder-jsp-references.sh"
target="WebUI/src/main/webapp/cm/app/webmgt.jsp"
scratch_dir="tmp/test-verify-no-finder-jsp-references"
mkdir -p "$scratch_dir"

backup="$scratch_dir/webmgt.jsp.bak"
cp "$target" "$backup"

cleanup() {
    if [ -f "$backup" ]; then
        cp "$backup" "$target"
    fi
    rm -rf "$scratch_dir"
}
trap cleanup EXIT INT TERM

fail=0

echo "==> PASS case: current tree must pass"
if sh "$script" > "$scratch_dir/pass.out" 2>&1; then
    if grep -q "PASS" "$scratch_dir/pass.out"; then
        echo "  ok: PASS case returned 0 with PASS message"
    else
        echo "  FAIL: PASS case returned 0 but no PASS message in output:" >&2
        cat "$scratch_dir/pass.out" >&2
        fail=1
    fi
else
    echo "  FAIL: PASS case unexpectedly returned non-zero on current tree:" >&2
    cat "$scratch_dir/pass.out" >&2
    fail=1
fi

echo "==> FAIL case: re-introduce finder.jsp navigation entry"
cat >> "$target" <<'PROBE'

<%-- probe: re-introduced finder.jsp navigation entry should fire the gate --%>
<jsp:include page="includes/finder.jsp" flush="true">
    <jsp:param name="probe" value="true"/>
</jsp:include>
PROBE
if sh "$script" > "$scratch_dir/fail.out" 2>&1; then
    echo "  FAIL: FAIL case unexpectedly returned 0 after re-introducing finder.jsp navigation entry:" >&2
    cat "$scratch_dir/fail.out" >&2
    fail=1
else
    if grep -q "FAIL" "$scratch_dir/fail.out"; then
        echo "  ok: FAIL case returned non-zero with FAIL message"
    else
        echo "  FAIL: FAIL case returned non-zero but no FAIL message:" >&2
        cat "$scratch_dir/fail.out" >&2
        fail=1
    fi
fi

# Restore before the next test.
cp "$backup" "$target"

echo "==> FAIL case: <%@include file=\"includes/finder.jsp\"> (alternate navigation form)"
cat >> "$target" <<'PROBE2'

<%@include file="includes/finder.jsp" %>
PROBE2
if sh "$script" > "$scratch_dir/fail2.out" 2>&1; then
    echo "  FAIL: FAIL case 2 unexpectedly returned 0:" >&2
    cat "$scratch_dir/fail2.out" >&2
    fail=1
else
    if grep -q "FAIL" "$scratch_dir/fail2.out"; then
        echo "  ok: FAIL case 2 returned non-zero with FAIL message"
    else
        echo "  FAIL: FAIL case 2 returned non-zero but no FAIL message:" >&2
        cat "$scratch_dir/fail2.out" >&2
        fail=1
    fi
fi

# Restore before the next test.
cp "$backup" "$target"

echo "==> PASS case: finder_js.jsp shared-lib include must NOT trigger the gate"
# Verify the regex carve-out for the shared-library finder_js.jsp
# include. This is the existing line 162 of cm/app/webmgt.jsp, which
# must remain allowed as it provides PercComponentWrapper /
# PercViewReadyManager / PercPathService etc. for non-Finder
# functionality. The T029b gate must NOT false-positive on it.
# (The line is already in the tree; this assertion is implicit in
# the PASS case above. The explicit probe below appends an extra
# finder_js.jsp include to confirm the regex is the navigation-entry
# form only.)
cat >> "$target" <<'PROBE3'

<%-- probe: extra finder_js.jsp shared-lib include must NOT fire the gate --%>
<%@include file="includes/finder_js.jsp" %>
PROBE3
if sh "$script" > "$scratch_dir/findjs.out" 2>&1; then
    if grep -q "PASS" "$scratch_dir/findjs.out"; then
        echo "  ok: finder_js.jsp shared-lib include did not trigger the gate"
    else
        echo "  FAIL: finder_js.jsp include unexpectedly changed the outcome:" >&2
        cat "$scratch_dir/findjs.out" >&2
        fail=1
    fi
else
    echo "  FAIL: gate false-positived on finder_js.jsp shared-lib include:" >&2
    cat "$scratch_dir/findjs.out" >&2
    fail=1
fi

# Restore before exit (also covered by trap).
cp "$backup" "$target"

if [ "$fail" -ne 0 ]; then
    echo "test-verify-no-finder-jsp-references: FAIL" >&2
    exit 1
fi
echo "test-verify-no-finder-jsp-references: PASS"
exit 0