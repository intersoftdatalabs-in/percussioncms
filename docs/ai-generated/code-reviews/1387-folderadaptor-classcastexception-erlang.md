# Erlang Review: Fix ClassCastException in FolderAdaptor (Issue #1387)

**Date**: 2026-07-21  
**Scope**: `projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java` (`fix/1387-folderadaptor-classcastexception` vs `origin/development`)  
**Intent**: Resolve `ClassCastException` on REST endpoints (`/rest/folders/by-path/`, `/rest/items/`) caused by an unsafe cast from `IPSItemSummary` to `PSItemSummary` (`com.percussion.services.content.data.PSItemSummary`).

## Summary

In `FolderAdaptor.java` line 298, `folderSummary` (returned by `folderHelper.findFolder` / `findItemById` as `IPSItemSummary`) was forcibly cast to `com.percussion.services.content.data.PSItemSummary`. At runtime, `folderHelper` returns `PSDataItemSummary` (`com.percussion.share.data.PSDataItemSummary`), causing `ClassCastException` and returning HTTP 500 across REST folder/item endpoints.

**Fix**:
- Replaced `String.valueOf(((PSItemSummary) folderSummary).getGUID().getUUID())` with `folderSummary.getId()`. The `IPSItemSummary` interface standardizes `getId()`, which both `PSDataItemSummary` and `PSItemSummary` implement safely.
- Removed unused import `com.percussion.services.content.data.PSItemSummary`.

**Cross-platform path review**: Clean. No local file paths constructed.

## Scope

- Base: `origin/development`
- Head: `fix/1387-folderadaptor-classcastexception`
- Files: `projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java`
- Memory patterns hit: Never cast Spring-injected service interfaces or abstract summary return types to concrete impls.

## Recommendation

**approve**

**May commit/push**: **yes**

## Gate

| Check | Result |
|-------|--------|
| Bugs blocking | None |
| Behavioral tests for new non-trivial logic | Existing `sitemanage` suite compiles and passes |
| Secrets | None |
| Cross-platform path handling | Clean |

## Issues

None open.
