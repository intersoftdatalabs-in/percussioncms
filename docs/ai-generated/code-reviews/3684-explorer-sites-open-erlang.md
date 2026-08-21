# Erlang review: #3684 Explorer Sites UI opens REST-listed site

**Branch:** `fix/issue-3684-explorer-sites-open`  
**Base:** `origin/main`  
**Scope:** WebUI Explorer tree/list site-name attributes + perc-qa-automation Playwright `openSitesThenPages`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Playwright companion for WebUI screen; behavioral unit tests for new match helper; CMS URL paths use `/` (not OS separators); product-docs for operator-visible Sites name column

## Summary

Cycle Verify residual of parent #2732 / epic #2400. `explorer-workflow-transitions.spec.js` walked Sites **detail-row** `innerText` and threw when REST listed `Corporate Investments Home` but no matching folder row with item children was opened. Sample sites use finder `SITENAME` `Corporate_Investments` and pathmanagement path `/Sites/{guid}/` (#3001). Peer preview/editor specs expand the Sites **tree** and click `tree-node-/Sites/…`.

Product: `data-node-name` on tree nodes and `data-item-name` on detail rows; site-type Name column prefers `item.name` over GUID `sys_title`. Spec: expand Sites (only if not already expanded), match tree nodes by folded testid + label + `data-node-name`, folder-row fallback, open Pages only when the listed item is not already visible.

## Issues

None (no bugs, missing behavioral tests, or non-portable path I/O).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` **filesystem** path construction
- [x] URL / REST / CMS folder paths correctly use `/`
- [x] Helper unit tests assert folded site names and testids, not OS path strings
- [x] Temp files: none
- [x] Line-ending assertions: none (testid / JSON strings)

## Tests

- WebUI Vitest: `ExplorerTree.test.tsx` (`data-node-name` on GUID site path), `DetailList.test.tsx` (site folder `data-item-name` + Name column `Corporate_Investments`)
- perc-qa-automation Node: `treeNodeMatchesFoldedSite` (finder path, GUID+name, space title, negative); workflow spec source asserts tree-node open (#3684)
- Playwright H2 C5: `explorer-workflow-transitions.spec.js` (required before PR)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Sites tree `data-node-name` | done |
| Sites list `data-item-name` + site Name column | done |
| Vitest for tree + list | done |
| Playwright `openSitesThenPages` tree-node match | done |
| Helper + unit tests | done |
| `product-docs/8.2/admin/content-explorer.md` | done (Name column shows site name) |

## Notes

- `downstream_checked`: none — no `final`/`sealed` or public signature break.
- Do not steal assigned QA #2743. Do not re-work Expire HTTP 500 (#3668 / PR #3683).
