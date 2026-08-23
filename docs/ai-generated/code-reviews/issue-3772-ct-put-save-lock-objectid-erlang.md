# Erlang review — issue 3772 Content Type PUT save lock objectId mismatch

**Scope:** unique commits vs stacked `feat/issue-3744-content-type-lock-save-chrome` (lock REST + PUT-requires-lock + chrome). Unique work: packed NODEDEF lock objectId alignment (`PSGuidUtils.toFullLong`, `PSContentDesignWs.contentTypeLockObjectId`), restore strict PUT 409 when no lock, Playwright save/restore/unlock. Base for product: `origin/main`.
**Reviewer:** Erlang (independent of implementer).
**Date:** 2026-08-23
**Memory patterns hit:** behavioral tests for new/changed lock lookup; change-class closure (system + sitemanage + Playwright + product-docs); no extra agent-rule commit.

## Summary

`IPSObjectLockService` stores `PSDesignGuid.getValue()` (type bits included). `PSGuidUtils.toFullLongList` used `IPSGuid.longValue()`, which is uuid-only when host is 0, so `findLockByObjectId` missed the lock created by `loadContentTypes(..., lock=true)`. PUT then reported `OBJECT_NOT_LOCKED` for packed id `8589935593` (NODEDEF + percPage 1001). Lookup now uses packed longs. Adaptor no longer recreates a second lock; `isLocked` + save share the same objectId. Other-user lock still 409.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

N/A — no new filesystem path joins, temp dirs, or path-string assertions. Tests use GUID longs only.

## Issues

None that block. Stacked chrome/REST from #3744/#3749 is already on this branch; unique logic is the packed objectId contract plus restoring fail-closed PUT without a held lock.

## Tests

- `PSGuidUtilsToFullLongTest` — host-0 NODEDEF uuid-only `longValue()` vs packed `toFullLong` / `toFullLongList`.
- `PSContentDesignWsContentTypeLockObjectIdTest` — load GUID and save lookup share packed objectId; persisted `PSObjectLock.objectId` matches save lookup.
- `ContentTypeAdaptorUpdateTest.update_conflictWhenNoLockHeld` — PUT 409 when `isLocked` summary missing (other-user 409 already covered).
- Playwright: `developer-content-type-lock-save.spec.js` (lock → save description → restore → unlock).
