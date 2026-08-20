# Erlang review: #3646 Explorer delete selected folder (product route)

**Branch:** `fix/issue-3646-explorer-delete-folder`  
**Base:** `origin/main`  
**Reviewer:** Erlang (independent of implementer)  
**Date:** 2026-08-20  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Jackson WRAP_ROOT_VALUE envelopes; product Explorer flag-off pathmanagement vs `rxFolderMutations`; Playwright no-skip when Sites/Assets parent exists; change-class companions (Vitest + Playwright + product-docs).

## Summary

Product ReducedActions **Delete** posted to a non-existent `POST /pathmanagement/path/delete/{path}`. Live pathmanagement is `POST …/path/deleteFolder` with Jackson `DeleteFolderCriteria` (classic Finder). The change routes flag-off delete through that envelope (`shouldPurge=false`, guid never null), refreshes list+tree after success, and adds no-skip Playwright that only recycles a `qa3646_*` folder this test created. Empty-list copy contrast `#777`→`#555` so post-delete `detail-list-empty` meets WCAG AA.

Does not steal #3645 rename wrap, #3647 copy, or gap-matrix Present. Cluster #3644 is already on main; this slice only adds `onDelete` next to existing `onCreateFolder` refresh.

## Cross-platform path checklist

- [x] No new filesystem `"/" +` / `"\\" +` construction  
- [x] URL/REST paths correctly use `/`  
- [x] Tests match URL fragments, not OS file paths  
- [x] No Unix-only temp/root assumptions  

## Issues

None blocking.

### Suggestions (non-blocking)

- `createNamedFolder` in `folder-recycle-smoke.js` still POSTs unwrapped rename; this slice retries with `RenameFolderItem` wrap when names mismatch. A later recycle helper cleanup could wrap rename once (out of scope).

## Tests / companions

- Vitest: `pathApi.test.ts` (deleteFolder wrap), `folderMutations.test.ts` (flag off), `ContentExplorerShell.delete.test.tsx` (refresh)  
- Playwright: `explorer-delete-folder.spec.js` + helper + `node --test` unit  
- product-docs: Reduced actions Delete paragraph  
- Modules: `cd WebUI && ../mvnw.cmd clean install` BUILD SUCCESS (Surefire 63, Vitest 2932); `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` BUILD SUCCESS  

## Change-class closure

WebUI product Explorer action: production wrap + shell refresh, Vitest, Playwright surface, README, product-docs. REST/sitemanage unchanged (endpoint already existed).
