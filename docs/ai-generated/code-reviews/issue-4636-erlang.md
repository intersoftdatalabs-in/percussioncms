# Erlang review — issue 4636 Explorer rename selected item

Scope: branch `feat/issue-4636-explorer-rename-item` vs origin/main.

Recommendation: approve
Gate: May commit/push: yes (no hard-gate bugs found)

## Summary

Vertical slice adds `POST /rest/folders/rename/item` (`RenameFolderItemRequest`), sitemanage `FolderAdaptor.renameFolderItem` (403/404/409), WebUI ReducedActions routing for non-folders, Playwright H2 spec, product-docs.

## Issues

None blocking.

Cross-platform path checklist: item paths stay URL/finder `/` forms; parent folder derived with `substringBeforeLast(..., "/")` on repository paths (always `/`). No OS filesystem separators.

Behavioral tests: FoldersTest, FolderAdaptorRenameFolderItemTest, pathApi/folderMutations/reducedActions/renameItemErrors.

Memory patterns: REST 403/404/409 mapping, WRAP_ROOT_VALUE DTO, Explorer dual-run flag off.
