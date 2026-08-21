# Erlang review: #3684 Explorer Sites open for REST-listed Corporate Investments

**Branch:** `fix/issue-3684-sites-name-match`  
**Base:** `origin/main`  
**Scope:** WebUI Explorer tree/list site-name attributes + perc-qa-automation Playwright `openSitesThenPages`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Playwright companion for WebUI screen; behavioral unit tests for new match helper; CMS URL paths use `/` (not OS separators); product-docs for operator-visible Sites name column; do not treat sibling PR #3689 as done when Cycle Verify still failed

## Summary

Cycle Verify residual of parent #2732 / QA #2743. REST lists FastForward page `Corporate Investments Home` under finder `SITENAME` `Corporate_Investments` and repository `//Sites/CorporateInvestments`. PR #3689 added `data-node-name` and tree-node matching but Cycle Verify on `ea6184f` still failed `openSitesThenPages` (Sites expand used `tree-toggle` only; folder-icon open can leave Sites selected; Pages-first walk missed site-root `rffHome` items).

This change:

- Resolves operator-visible site names via `explorerSiteDisplayName` (finder name / folderPath leaf, never a content GUID).
- Exposes `data-node-name`, `data-folder-path`, and `tree-toggle-*` on Explorer tree nodes; site Name column + `data-item-name` on the list.
- Playwright expands Sites with tree-toggle **or** aria-hidden peer; matches site-root testids only; falls back to detail rows then every sample site until the listed page or an item row is visible.

## Issues

None (no bugs, missing behavioral tests, or non-portable path/file I/O).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` **filesystem** path construction
- [x] URL / REST / CMS folder paths correctly use `/`
- [x] Helper unit tests assert folded site names and testids, not OS path strings
- [x] Temp files: none
- [x] Line-ending assertions: none (testid / JSON strings)

## Tests

- WebUI Vitest: `sitePath.test.ts` (`explorerSiteDisplayName` / `isExplorerSiteRootItem`), `ExplorerTree.test.tsx` (GUID path + folderPath leaf + tree-toggle), `DetailList.test.tsx` (site Name column + `data-item-name` + a11y gate)
- perc-qa-automation Node: `listedPageSiteNames` page-title hint, `treeNodeMatchesFoldedSite` + `isExplorerSiteRootTestId`; workflow spec source lock
- Playwright H2 C5: `explorer-workflow-transitions.spec.js` 2 passed, 0 skipped; golden 2 passed

## Change-class closure

| Companion | Status |
|-----------|--------|
| Sites tree name attributes | done |
| Sites list Name column + `data-item-name` | done |
| Vitest for tree + list + sitePath | done |
| Playwright `openSitesThenPages` | done |
| Helper + unit tests | done |
| `product-docs/8.2/admin/content-explorer.md` | done |

## Notes

- `downstream_checked`: none — no `final`/`sealed` or public signature break.
- Do not steal assigned QA #2743. Do not re-work Expire HTTP 500 (#3668 / PR #3683).
- Leave #3684 open for later Cycle Verify.
