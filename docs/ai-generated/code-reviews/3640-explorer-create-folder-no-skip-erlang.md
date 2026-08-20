# Erlang review — #3640 Explorer create-folder product route

**Branch:** `fix/issue-3640-explorer-create-folder-no-skip`  
**Date:** 2026-08-20  
**Scope:** uncommitted vs `HEAD` (issue #3640 / parent #3102)  
**Memory patterns hit:** change-class closure (WebUI + Playwright + product-docs); behavioral tests; no-skip H2 UI proof; cross-platform path checklist (N/A — no filesystem I/O)

## Summary

Default Explorer Create Folder (`ReducedActions` / pathmanagement `addNewFolder`, flag off) succeeded on the server but the product shell did not refresh the detail list or tree. The shell now wraps `onCreateFolder` to bump `listEpoch` after success, and `ExplorerTree` reloads the selected folder’s children when `childrenEpoch` changes. Playwright on `spa.jsp?entry=explorer` (no `rxFolderMutations=1`) must not skip when a Sites/Assets parent exists. Gap-matrix Sites/folders tree stays **Partial**.

## Recommendation

approve

## Gate

**May commit/push: yes**

No bugs, missing behavioral tests, or non-portable path I/O in this diff.

## Issues

None.

## Cross-platform path checklist

- No new filesystem path joins
- URLs use `/` (pathmanagement / SPA query) — allowed
- Playwright helpers use `URL` searchParams, not OS separators
- No Unix-only roots or Windows-only drive letters

## Companions (change class: WebUI product screen)

| Companion | Status |
|-----------|--------|
| Shell wrap + tree reload | done |
| Vitest (`ContentExplorerShell.createFolder`, `ExplorerTree` epoch) | done |
| Playwright `explorer-create-folder.spec.js` no-skip | done |
| Helper unit tests | done |
| `product-docs/8.2/admin/content-explorer.md` Create Folder note | done |
| gap-matrix stays Partial + #3640 citation | done |

## Test evidence

- `WebUI` `../mvnw.cmd clean install` — BUILD SUCCESS (Vitest 2897 passed; Java Tests run: 63, Failures: 0)
- `modules/perc-qa-automation` `../../mvnw.cmd clean install` — BUILD SUCCESS
- `node --test tests/unit/explorer-create-folder.test.js` — 8 passed
- C5: `perc-devctl qa-up` TEST_CMS_URL=http://127.0.0.1:9993; hot-copy `cm/modern/assets`; Playwright `explorer-create-folder.spec.js` 2 passed 0 skipped; golden 2 passed; console-clean=yes; server.log-clean=yes (passing run)
