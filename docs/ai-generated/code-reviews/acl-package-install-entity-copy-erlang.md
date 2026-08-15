# Erlang review — ACL package-install entity copy (`fix/acl-package-install-entity-copy`)

**Date:** 2026-08-14  
**Reviewer:** Erlang Shen (independent of implementer)  
**Target:** uncommitted working tree vs `origin/main` (GitHub `main` raw used as base proxy)

## Summary

Upgrade package install (`perc.responsiveTemplates`, `perc.twitterSummaryCards`) failed after #3387 because `internalPersist` always `session.merge`d an already-persistent ACL graph, and `PSAclEntryImpl.merge` had a 2021 empty-`if` (`if (...);`) that always `addPsPermission`’d the incoming `PSAccessLevelImpl` — including rows that already shared a SYSID with the managed collection. Hibernate 6/7 `EntityCopyNotAllowedObserver` then threw `Multiple representations of the same entity [PSAccessLevelImpl]`; the service logged `Error persisting Acl` (118× on the cited upgrade).

The production repair is the right one: clone missing permission types without SYSID, drop types absent from incoming, flush a session-managed ACL instead of merging it, and short-circuit `findExistingPersistIdentity` when the source is already the session identity. First-pass gap (untested persist-path) is closed: `persistInSession` / `isSessionIdentity` are extracted and covered by Mockito tests. Merge tests now use a distinct incoming SYSID so `HashSet.equals` cannot hide an attach. No remaining bugs.

## Scope

- Base: `origin/main` (reconstructed from GitHub `main` raw of the three production files; no shell `git` in this subagent)
- Head: uncommitted work on `fix/acl-package-install-entity-copy`
- Files: 4 (3 modified, 1 new) as listed by the author
  - `system/services/src/com/percussion/services/security/data/PSAclEntryImpl.java`
  - `system/services/src/com/percussion/services/security/impl/PSAclService.java`
  - `system/services/src/com/percussion/services/security/PSAclPersistMerger.java` (comment only)
  - `system/src/test/java/com/percussion/services/security/data/PSAclEntryImplMergeTest.java` (new)
- Prior reports: `docs/ai-generated/code-reviews/3384-display-format-acl-persist-erlang.md` (topic parent); also `3378-display-format-acl-save-400-erlang.md`, `3391-acl-bulk-arraylist-cast-erlang.md`, `3282-objectacl-sysid-collision-erlang.md`
- Memory patterns hit: missing behavioral tests for new logic; Hibernate `@Version` / `session.merge` / managed-entity residuals; empty / accidental control-flow (stray `;`); change-class companions (entity merge + persist path + tests)
- Cross-platform path review: **no issues** (no filesystem I/O, path joins, or path assertions)
- Product documentation: **N/A** — persist/upgrade defect; no operator-facing ACL semantics change
- Independence: this reviewer did not author the change

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: **yes**

Product/test files only. Do **not** include `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md` in the commit unless a human has explicitly approved that rule-file draft.

## Issues

### Issue 1 -- Severity: bug
- File: `system/services/src/com/percussion/services/security/impl/PSAclService.java:680`
- Description: `internalPersist` now branches `session.contains` → `flush()` else `session.merge()`. That is the #3387 half of the upgrade failure (always-merge of a `createAcl` persist()ed graph with bidirectional EAGER entries/permissions). `findExistingPersistIdentity` also gained a `session.contains(src)` short-circuit (`PSAclService.java:736`). Neither decision is exercised by a test. `PSAclEntryImplMergeTest` covers only the empty-`if` / clone-without-SYSID half. A one-line revert to always-merge would re-break package install and would still be green.
- Suggestion: Add a focused unit test (Mockito `Session` + package-visible helper, or a small `persistManagedVsDetached` method extracted from `internalPersist`) that asserts:
  1. when `session.contains(acl)` is true, `merge` is **never** called and `flush` is;
  2. when `contains` is false and no existing identity is found, `merge` **is** called;
  3. when `contains(src)` is true, `findExistingPersistIdentity` returns `src` and does not `session.get` a second representation.
  Place it next to `PSAclPersistMergerTest` / `PSAclServiceTransactionalEntryPointsTest`. Then re-review.
- Status: fixed
- Pattern-id: tests.missing-behavioral

### Issue 2 -- Severity: suggestion
- File: `system/src/test/java/com/percussion/services/security/data/PSAclEntryImplMergeTest.java:50`
- Description: `samePermissionTypeIsNotDuplicated` and `mergeDoesNotAttachSharedSysidTwiceOnAclImplMerge` use the same SYSID **and** default `aclEntryId == 0` on both graphs. `PSAccessLevelImpl.equals` is `(id, aclEntryId, permission)`, so `HashSet.add` of the incoming entity is a no-op even on the **old** empty-`if` code. Those two tests would not fail if someone re-introduced `addPsPermission(incoming)` for an already-present ordinal. `incomingPermissionEntityIsNotAttached` is the test that actually fails on origin/main.
- Suggestion: Give the incoming entry a distinct id (or distinct `aclEntryId`) so Java-equals cannot hide an attach, **or** assert the collection identity set does not contain the incoming instance (`assertFalse(target.getPsPermissions().contains(incomingDelete))` is insufficient for the same reason — prefer `assertTrue(target perms stream noneMatch(p -> p == incomingDelete))` after forcing a non-equal incoming row). Keep `incomingPermissionEntityIsNotAttached` as the primary lock.
- Status: fixed
- Pattern-id: tests.happy-path-only

### Issue 3 -- Severity: nit
- File: `system/services/src/com/percussion/services/security/PSAclPersistMerger.java:41`
- Description: Method javadoc still says “the instance Hibernate should merge/persist.” The new class comment correctly says package-install of an already-persistent ACL must **not** `session.merge`. Callers that only read the method javadoc will do the wrong thing.
- Suggestion: Change the `@return` line to “the session identity to flush if managed, or the detached/transient instance to `session.merge`.”
- Status: fixed

## What looks correct (not issues)

- Origin/main `PSAclEntryImpl.merge` really is `if (!curPer.containsKey(...)); { addPsPermission(newAccess); }` — the stray semicolon is the 2021 defect. Replacing it with `new PSAccessLevelImpl(this, incoming.getPermission())` matches the copy-ctor used for new entries in `PSAclImpl.merge` (`new PSAclEntryImpl(this, entryImpl)`).
- Removal of ordinals absent from incoming is preserved (`psPermissions.removeAll` after `keySet().removeAll(incomingOrdinals)`).
- `createAcl` + `saveAcls` (same or later transaction): `contains` short-circuit **or** `session.get` + `mergeOntoExisting` + flush. Both avoid merge of a managed bidirectional graph.
- REST `#3384` path (`AclAdaptor` already `mergeOntoExisting`s, then `saveAcls`) still works: new permission types are cloned without SYSID; existing types keep the managed row. Old empty-`if` could have added a second DELETE row (`id=0` vs persisted SYSID — `HashSet`/`PersistentSet` both accept that).
- `PSAclPersistMerger.mergeOntoExisting` body unchanged; `existing == incoming` already no-ops. Comment-only edit is accurate.
- New test file copyright is Intersoft 2026; legacy production headers left Percussion. JUnit 5. No new Spring beans / REST surface / product-docs required.
- Change-class companions otherwise present: entity merge + persist branch + unit test file. Missing piece is Issue 1 only.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] No `Path` / `File` construction added
- [x] Tests do not assert OS-specific path strings
- [x] No scripts

## Handoff

- Re-reviewed: uncommitted ACL persist/entity-copy fix vs `origin/main` (product + tests).
- Prior Issue 1/2/3: **fixed**. No new bugs.
- Recommendation: **approve**. May commit/push: **yes** (exclude unapproved `patterns.md`).
- Artifact: `docs/ai-generated/code-reviews/acl-package-install-entity-copy-erlang.md`

## Re-review

**Date:** 2026-08-14 (same day, after request-changes)  
**Diff:** uncommitted vs `HEAD` / `origin/main` (no commits on branch). Files: `PSAclPersistMerger.java`, `PSAclService.java`, `PSAclEntryImpl.java`, `PSAclPersistMergerTest.java`, new `PSAclEntryImplMergeTest.java`, plus leftover `patterns.md` (not re-touched this pass).

### Prior issues

| Issue | Result |
|-------|--------|
| 1 bug — persist `contains`→flush vs merge untested | **Fixed.** `PSAclPersistMerger.persistInSession` / `isSessionIdentity` extracted. `PSAclService.internalPersist` calls `persistInSession`; `findExistingPersistIdentity` returns `src` when `isSessionIdentity`. Tests: `managedAclFlushesAndDoesNotMerge`, `detachedAclIsMerged`, `sessionIdentityDoesNotLookUpSecondRepresentation`, `detachedIsNotSessionIdentity`, `persistInSessionRejectsNulls`. |
| 2 suggestion — HashSet.equals hid attach | **Fixed.** Incoming DELETE SYSID is `99L` (target keeps `SHARED_DELETE_SYSID`). `samePermissionTypeIsNotDuplicated` asserts `noneMatch(p -> p == incomingDelete)`. `mergeDoesNotAttachSharedSysidTwiceOnAclImplMerge` would see `deleteCount == 2` if incoming were attached. |
| 3 nit — `mergeOntoExisting` `@return` | **Fixed.** Javadoc now says flush-if-managed / `session.merge` if detached. |

### New findings

None.

`sessionIdentityDoesNotLookUpSecondRepresentation` only exercises `isSessionIdentity` (which never calls `session.get`); the service short-circuit is a three-line glue return. Acceptable — the non-trivial persist decision is locked by `persistInSession` tests. Class javadoc on `PSAclPersistMergerTest` still leads with #3384 only; not blocking.

### Commit hygiene (not a code defect)

Working tree still has `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md` (one Hibernate entity-copy line from the first review). Root `AGENTS.md` **Human review of agent rules** — leave it uncommitted unless the human approves that rule diff.

### Cross-platform path checklist

Unchanged: no filesystem I/O. No issues.

### Gate

approve — May commit/push: **yes**
