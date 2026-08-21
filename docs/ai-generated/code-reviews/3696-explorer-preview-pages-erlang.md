# Erlang review: #3696 Explorer site select lists Pages

**Branch:** `fix/issue-3696-explorer-preview-pages`  
**Base:** `origin/main`  
**Scope:** WebUI Explorer site-root browse path + perc-qa-automation Playwright preview helper  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Playwright companion for WebUI screen; behavioral unit tests for new path helper; CMS URL paths use `/` (not OS separators); product-docs for operator-visible Sites → Pages listing

## Summary

Cycle Verify residual of explorer preview (`explorer-preview-view.spec.js`). H2 REST listed **Corporate Investments Home**, but selecting the site tree node left the detail list on site-root folders (`Pages`, `AboutCorporateInvestments`, `Briefs`). FastForward injects virtual Pages chrome (#3457); the product did not browse that path on site select.

Product: `resolveExplorerSiteBrowsePath` appends `/Pages` for site-root items (idempotent). `ContentExplorerShell.handleSelectFolder` uses that path. `ExplorerTree` expands a collapsed site on label click without calling `onActivate` (toggle would otherwise list the site root again). Playwright preview `openSitesThenPages` matches the workflow helper (tree-node fold + Pages fallback).

## Issues

None (no bugs, missing behavioral tests, or non-portable path I/O).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` **filesystem** path construction
- [x] URL / REST / CMS folder paths correctly use `/` (`appendExplorerPagesSegment`)
- [x] Tests assert CMS path strings, not OS file paths
- [x] Temp files: none
- [x] Line-ending assertions: none

## Tests

- WebUI Vitest: `sitePath.test.ts` (site-root Pages append, GUID path, nested folder unchanged), `ExplorerTree.test.tsx` (site click expands Pages), `ContentExplorerShell.test.tsx` (tree select lists Home `data-previewable=true`)
- perc-qa-automation Node: preview spec source asserts tree-node + Pages walk (#3696)
- Playwright H2 C5: `explorer-preview-view.spec.js` 3 passed; `golden-unattended-smoke.spec.js` 2 passed

## Change-class closure

| Companion | Status |
|-----------|--------|
| Site-select Pages list path | done |
| Tree expand-on-select without undoing Pages | done |
| Vitest sitePath + tree + shell | done |
| Playwright preview helper aligned with workflow | done |
| `product-docs/8.2/admin/content-explorer.md` | done |

## Notes

- `downstream_checked`: none — no `final`/`sealed` or public Java signature break.
- Toggle/expand still lists the site folder via `handleActivate`; label select lists Pages.
