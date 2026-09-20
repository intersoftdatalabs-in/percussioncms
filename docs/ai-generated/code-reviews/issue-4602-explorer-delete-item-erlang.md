# Erlang review — #4602 Explorer delete/recycle selected item

**Scope:** uncommitted slice on `fix/issue-4601-explorer-move-tree-item` (absorb same-parent #4629) plus #4602 files.

**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** REST 403/404/409 mapping; Playwright companion for Explorer; product-docs for operator surface; recycle vs purge (`shouldPurge=false`).

## Summary

Vertical increment: `DELETE /rest/folders/item/{path}` recycles a leaf via `folderHelper.removeItem(..., false)`; maps 403/404/409; SPA enables Delete for writable items; Playwright `explorer-delete-item.spec.js`; product-docs Content Explorer.

## Issues

None blocking. Paths use REST `/` URL join + `encodeURIComponent` (not OS file I/O). Cross-platform path checklist: N/A (no filesystem joins).

Behavioral tests: FoldersTest, FolderAdaptorDeleteItemTest, Vitest pathApi/reducedActions/deleteItemErrors, Playwright helper unit.

## C1 evidence (reviewer)

rest, sitemanage, WebUI, perc-qa-automation `mvnw clean install` BUILD SUCCESS after testCompile fix (`setFolderType` removed).
