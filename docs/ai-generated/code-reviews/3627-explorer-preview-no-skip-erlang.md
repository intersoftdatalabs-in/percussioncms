# Erlang review: #3627 Explorer preview selected page no-skip on H2

**Branch:** `fix/issue-3627-explorer-preview-h2-noskip`  
**Base:** `origin/main` (`527a9f240d`)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path checklist:** N/A for OS filesystem I/O. CMS/URL paths use `/` (correct). `encodeCmsRelPath` encodes URL segments; does not join OS paths.

## Summary

Slice of parent #3102: Explorer Preview on `spa.jsp?entry=explorer` must open a listed H2 page (HTTP 200 editor-or-preview host) without Playwright `test.skip` when a previewable row exists. Folders stay non-previewable. Gap-matrix Open/preview stays Partial.

Product: `openPreviewItem` / `resolvePreviewTarget` open `spa.jsp?entry=editor&mode=view` when a content id is present (H2 FastForward site-path assembly 500s: nav types unregistered / assembly NPE). Path-only pages still use Finder site-path. Assets unchanged.

Playwright: unwrap `PSPathItemList` / single `PathItem`; never skip on H2 or when a previewable row exists; open the site via explorer-tree; assert popup HTTP 200.

## Issues

None that block commit.

## Memory patterns hit

- False soft-skip when REST unwrap misses `PSPathItemList` / single `PathItem` (#3575 peer).
- H2 QA must fail, not skip, when demo-sites have the fixture.
- Do not flip gap-matrix to Present from agent merge.

## Evidence

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests 2893 passed.
- `npm run test:unit` (perc-qa-automation helper file) — 18 passed.
- C5: `npm run test:surface -- --path tests/explorer-preview-view.spec.js` — 2 passed, 0 skipped. console-clean=yes (pageerror). server.log-clean=yes for the test window (pre-existing FastForward binary import ERRORs are install-time, BUG:#3592).
