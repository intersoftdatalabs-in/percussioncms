# Erlang review — #3656 Explorer copy selected item

**Branch:** `fix/issue-3656-explorer-copy-item`  
**Base:** `origin/main`  
**Recommendation:** approve (routing slice)  
**May commit/push:** yes  
**Gate:** pass for WebUI routing; C5 live copy/item still 500s on newCopies clone (`stateId must be > 0`) — residual, not a routing regression.

## Summary

ReducedActions default Copy always posted `POST /folders/copy/folder`. Non-folder
Copy now uses `copyFolderItem` (`POST /folders/copy/item`) with the same
`CopyFolderItemRequest` wrap. Folder Copy (#3647) stays on `copy/folder`.
`onMove` / `moveItem` are unchanged (#3655). Dest list refresh remains the
existing shell wrap (open dest + `listEpoch` / dest tree children).

## Issues

None.

## Tests

- Vitest: `pathApi` copyFolder vs copyFolderItem URLs; `folderMutations.copyFolderItem`;
  `defaultReducedActionHandlers` folder vs asset; `ContentExplorerShell.copy-item`
  dest list after copy/item.
- Playwright: `explorer-copy-item.spec.js` no-skip H2 (disposable asset under Assets).
- Helper unit: `explorer-copy-item.test.js`.

## Cross-platform path checklist

N/A for filesystem I/O. REST/URL paths correctly use `/`. No OS temp or
separator concatenation.

## Memory patterns hit

Copy envelope (#3362): `CopyFolderItemRequest` (`itemPath` + `targetFolderPath`),
never a bare `sourcePath` root or invented `copy` on `MoveFolderItem`.
