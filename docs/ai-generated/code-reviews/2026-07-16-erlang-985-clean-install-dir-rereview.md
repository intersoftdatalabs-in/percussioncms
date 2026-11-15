# Erlang re-review: 985-clean-install-dir (post-fix)

**Date**: 2026-07-16  
**Prior**: `docs/ai-generated/code-reviews/2026-07-16-erlang-985-clean-install-dir.md` (request-changes)

## Summary

BUG-1 fixed: null-safe `Main.parseVersionPart`.  
BUG-2 fixed: NOFOLLOW consistency; top-level symlink deleted as link only; no symlink dir descent.  
BUG-3 fixed: `relativize` / no `..` in `isUnderInstallRoot`.  
BUG-4 fixed: failed deletes keep candidates in retained/failed with “still on disk” wording.

Tests: **27** green (`ObsoleteInstallDirCleanerTest` 22 + extract + exit-code).

## Recommendation

**`approve`**

## Gate

**May commit/push / open PR: yes**

## Residual (non-blocking)

- Do not stage `org/`
- Optional: force a real mid-tree delete failure in CI (chmod) for extra warn-and-continue coverage
