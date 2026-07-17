#!/usr/bin/env sh
# verify-no-jqplot-vendor-refs.sh (US2, feature 004-zero-code-scanning-alerts).
#
# Guards the removal of the dead jqplot vendor library
# (WebUI/.../gadgets/repository/common/lib/jqplot and
# WebUI/.../widgets/repository/common/lib/jqplot, plus the war/ copy)
# against regression: fails if any tracked source file (JSP, JS, HTML,
# XML) references "jqplot" outside of that library's own removed
# directories, or if a copy of the removed directory reappears in the
# tree.
#
# Rationale: the library was confirmed dead via `git grep` before
# removal — it backed Shindig-gadget-era dashboard charts that were
# replaced by the React dashboard (see WebUI/AGENTS.md Phase 1) — but a
# future contributor could re-vendor it or re-add a reference without
# realizing it is gone. This script makes that mistake fail loudly
# instead of silently reintroducing the removed CodeQL
# js/xss-through-dom / js/unsafe-jquery-plugin alerts.
#
# Usage: scripts/verify-no-jqplot-vendor-refs.sh
# Returns 0 if clean, 1 if a stray reference or reintroduced copy is found.
set -eu

repo_root="$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_root"

fail=0

echo "==> checking for reintroduced jqplot vendor directories"
for d in \
    "WebUI/src/main/webapp/cm/gadgets/repository/common/lib/jqplot" \
    "WebUI/src/main/webapp/cm/widgets/repository/common/lib/jqplot" \
    "WebUI/war/gadgets/repository/common/lib/jqplot"
do
    if [ -e "$d" ]; then
        echo "  FAIL: removed vendor directory reappeared: $d" >&2
        fail=1
    fi
done

echo "==> checking for stray jqplot references in tracked JSP/JS/HTML/XML"
matches=$(git grep -l -i "jqplot" -- "*.jsp" "*.js" "*.html" "*.xml" 2>/dev/null || true)
if [ -n "$matches" ]; then
    echo "  FAIL: found jqplot reference(s) outside the removed vendor library:" >&2
    echo "$matches" >&2
    fail=1
fi

if [ "$fail" -ne 0 ]; then
    echo "verify-no-jqplot-vendor-refs: FAIL" >&2
    exit 1
fi
echo "verify-no-jqplot-vendor-refs: PASS"
exit 0
