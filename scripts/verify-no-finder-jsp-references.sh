#!/usr/bin/env sh
# verify-no-finder-jsp-references.sh (T029b, feature 992-react-content-explorer).
#
# CI-gate artifact-grep for FR-019a: after US6's hard cut of the
# modern ContentExplorerShell onto the primary-nav shells, the
# production-built WebUI WAR must contain ZERO references to
# finder.jsp as a *navigation entry point* in the modern Track B
# shell (cm/app/webmgt.jsp).
#
# Rationale: US6's hard cut replaced the miller-column Finder
# chrome on the modern Track B shells with the React Content
# Explorer via the PercModernUI bridge. The legacy `finder.jsp`
# standalone include would re-introduce the classic fallback if a
# future contributor accidentally re-adds it. This script makes
# that regression fail loudly in CI rather than silently
# re-introducing the legacy chrome.
#
# Scope (per T029b task description and the US6 cutover inventory):
#
#   cm/app/webmgt.jsp           -- modern Track B primary-nav shell
#                                  (hard-cut in PR #1390). MUST be
#                                  empty grep result.
#
# Carve-outs (explicitly out of scope for T029b; tracked elsewhere):
#
#   cm/pages/app/webmgt.jsp     -- legacy Track A primary-nav shell.
#                                  Still references finder.jsp as a
#                                  navigation entry; migration is
#                                  deferred to the Track A workstream
#                                  (WebUI/AGENTS.md Track A:
#                                  "Dojo→jQuery migration planned").
#                                  When Track A migration completes
#                                  the cm/pages/app/webmgt.jsp
#                                  reference will be removed; the
#                                  gate's `target_jsp` list can be
#                                  extended at that point.
#
#   finder_js.jsp (the `_js` shared-library include, NOT the
#                  navigation entry) -- this is loaded as a
#                  shared JS library include by cm/app/webmgt.jsp
#                  (line 162) and remains required for non-Finder
#                  functionality (e.g. PercComponentWrapper,
#                  PercViewReadyManager). The T029b gate does NOT
#                  match `finder_js.jsp` -- only `finder.jsp` as a
#                  navigation entry. See the regex below.
#
# Usage: scripts/verify-no-finder-jsp-references.sh
# Returns 0 if clean, 1 if a navigation entry to finder.jsp remains.
#
# Self-test: scripts/test-verify-no-finder-jsp-references.sh
set -eu

repo_root="$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_root"

fail=0

# The regex matches the literal navigation-entry forms:
#   <jsp:include page="includes/finder.jsp" ...>
#   <%@include file="includes/finder.jsp" ...>
# It does NOT match:
#   finder_js.jsp (the shared-library include; explicit carve-out)
#   References inside JSP comments (<%-- ... --%>) -- stripped before
#     grep via the `sed` pipe below to avoid false positives from
#     explanatory comments at the cutover boundary.
target_jsp="WebUI/src/main/webapp/cm/app/webmgt.jsp"

echo "==> checking ${target_jsp} for finder.jsp navigation entries"
for path in $target_jsp; do
    if [ ! -f "$path" ]; then
        echo "  FAIL: target JSP does not exist: $path" >&2
        fail=1
        continue
    fi
    matches=$(perl -0777 -pe 's/<%--.*?--%>//gs' "$path" | \
              grep -nE '<jsp:include[[:space:]]+page="includes/finder\.jsp"|<%@include[[:space:]]+file="includes/finder\.jsp"' || true)
    if [ -n "$matches" ]; then
        echo "  FAIL: ${path} contains finder.jsp navigation entry:" >&2
        echo "$matches" | sed 's/^/    /' >&2
        fail=1
    fi
done

if [ "$fail" -ne 0 ]; then
    echo "verify-no-finder-jsp-references: FAIL" >&2
    exit 1
fi
echo "verify-no-finder-jsp-references: PASS"
exit 0