# Erlang review — issue #4626 copy dest 404

**Scope:** uncommitted branch `fix/issue-4626-copy-dest-404` vs `origin/main`.
**Recommendation:** approve
**Gate:** May commit/push: yes
**Memory patterns hit:** JAX-RS `Status` DTO vs HTTP status; map path-not-found to `FolderNotFoundException`.

## Summary

`FolderAdaptor.copyFolderItem` now `findFolder`s the destination before clone so missing dest raises `PSPathNotFoundServiceException` → `FolderNotFoundException` (404). `FoldersResource.copyFolderItem` rethrows `NotFoundException` instead of returning `new Status(404)` (HTTP 200). Tests cover dest-missing on the adaptor and JAX-RS rethrow on the resource. No filesystem I/O; path checklist N/A.

## Issues

None blocking.
