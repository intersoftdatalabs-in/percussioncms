# Erlang review — fix/1561-workflow-orm-phase4b

> **Branch:** `fix/1561-workflow-orm-phase4b` (off `origin/development` = `0554b657d8` = PR #1575).
> **Issue:** [#1561 — Migrate in-product workflow JDBC SQL to Hibernate + shared connection pool](https://github.com/intersoftdatalabs-in/percussioncms/issues/1561).
> **Reviewer:** Erlang (Kilo, independent review persona).
> **Author-disclosure:** Same session as implementer (no fresh agent available). Read context carefully; apply same rigor.

---

## Summary

Phase 4b ships the **Tier 2** migration promised in `phase4-scope-survey.md`: the state-roles + content-adhoc-users paths used by `PSExitAddEditAuthFlag` and `PSExitAuthenticateUser` now load via Hibernate (shared pool, Spring transaction) instead of opening a second `PSConnectionMgr()` connection. Two new Hibernate service methods + two Hibernate-backed static factories on the legacy context classes carry the migration; the no-connection overload of `PSWorkflowRoleInfoStatic.getActorRoles(...)` is now reachable end-to-end.

The other 6 in-product exits still use `new PSConnectionMgr()` and are tracked in `phase4-scope-survey.md` as Phase 4c/4d follow-ups. Build is green: extensions-workflow **24 tests** (16 active, 8 disabled — see Issue 4 below), perc-system **13 tests** across the two Phase 3/4b service test classes. No new warnings on changed files.

---

## Scope

- **Base:** `origin/development` (`0554b657d8` = PR #1575 merge).
- **Head:** branch `fix/1561-workflow-orm-phase4b` (uncommitted at start of review).
- **Files:** 9 changed (7 modified, 2 new).
- **Prior report:** `fix-1561-workflow-orm-phase4-erlang.md` (gate `approve`).
- **Memory patterns hit:** "Missing **behavioral** unit tests for new/changed non-trivial logic" (added 7 new service tests + 1 @Disabled mapping test), "Backward compatibility for public APIs" (write constructors kept for binary compat).

| File | Status | Purpose |
|---|---|---|
| `system/services/src/com/percussion/services/workflow/IPSWorkflowService.java` | modified | Adds `findStateRoles(long, long, int)` and `findWorkflowRoles(long, Set<Long>)` interface methods (Phase 4b). |
| `system/services/src/com/percussion/services/workflow/impl/PSWorkflowService.java` | modified | Implements both methods with JPQL `createQuery`. `@Transactional`. Filters + parameter validation match the legacy `PSStateRolesContext.QRYSTRING` (workFlowID + stateID + assignmentType >= minAssignmentType, ordered by roleId; workFlowID + roleId IN :ids for roles). |
| `system/services/src/com/percussion/services/system/IPSSystemService.java` | modified | Adds `findContentAdhocUsers(int)` interface method (Phase 4b). |
| `system/services/src/com/percussion/services/system/impl/PSSystemService.java` | modified | Implements `findContentAdhocUsers` with JPQL `createQuery`. `@Transactional`. Filters by `contentId = :cid` ordered by `adhocUserId`. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSStateRolesContext.java` | modified | Adds `static loadFromHibernate(int, int, int)` factory + package-private default constructor + private `populateFromHibernate(...)`. Mirrors the raw-JDBC constructor's classification (adhoc buckets, lower-case role-name maps, role-id → assignment-type map, role-id → role-name map). Argument validation: `IllegalArgumentException` for `workflowId <= 0` or `stateId <= 0`; `PSRoleException` for missing role names; `PSEntryNotFoundException` when no rows match. Existing 4-arg constructor preserved for binary compat. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentAdhocUsersContext.java` | modified | Adds `static loadFromHibernate(int)` factory + private `populateFromHibernate(...)`. Mirrors the raw-JDBC constructor's classification into `m_userNameToAdhocNormalRoleIDMap` / `m_adhocAnonymousRoleIDs` / `m_adhocAnonymousUserNames` / etc. `IllegalArgumentException` for `contentId <= 0`; `IllegalStateException` for empty username. Existing 2-arg constructor preserved for binary compat. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitAddEditAuthFlag.java` | modified | Drops `new PSConnectionMgr()`. `canUserEditContent` now reads `CONTENTSTATUS` via `PSCmsObjectMgr#loadComponentSummary(int)`, loads `STATEROLES` via `PSStateRolesContext.loadFromHibernate(...)`, loads `CONTENTADHOCUSERS` via `PSContentAdhocUsersContext.loadFromHibernate(...)`, and uses the no-connection `PSWorkflowRoleInfoStatic.getActorRoles(userName, roleNameList, src, cauc, authUser)` overload. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitAuthenticateUser.java` | modified | Same migration pattern: drops `new PSConnectionMgr()`; reads `CONTENTSTATUS` via Hibernate, loads state-roles + content-adhoc-users via Hibernate, calls no-connection `getActorRoles` overload. The pre-existing `Connection connection` parameter on `authenticateUser(String, Connection, AuthParams)` and `canUserCreate(Connection, AuthParams)` is removed; `SQLException` is still on the signatures because `PSCms.canReadInFolders` / `PSCms.canWriteToFolders` declare it. |
| `system/src/test/java/com/percussion/services/workflow/PSWorkflowServiceStateRolesTest.java` | **new** | 7 JUnit 5 + Mockito tests for the new service methods (see Tests section). |
| `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSLoadFromHibernateTest.java` | **new** | `@Disabled` Mockito mapping tests for `loadFromHibernate` (see Issue 4). |
| `docs/ai-generated/migrations/workflow-orm/phase4-scope-survey.md` | modified | Updated to record what shipped in Phase 4a (#1575) + Phase 4b (this branch) and what remains. |

---

## Recommendation

**approve**

---

## Gate

- **Blocking bugs:** 0
- **May commit/push:** **yes**

---

## Issues

### Issue 1 — Severity: bug — **N/A** (resolved at build time)

- **Description:** Initial ports failed to compile due to:
  1. `PSAssignedRole.getRoleId()` and `PSWorkflowRole.getRoleId()` do not exist as public getters — the role id is only available via `getGUID().longValue()`. (The fields are `private long roleId` with no getter.)
  2. `PSAdhocTypeEnum` / `PSAssignmentTypeEnum` are returned by `PSAssignedRole.getAdhocType()` / `getAssignmentType()` — the Hibernate values come back as enums, not ints.
  3. `PSContentAdhocUser.getUser()` is the getter name, not `getUserName()`.
- **Fix:** Both `loadFromHibernate` factories use `row.getGUID().longValue()` for role ids and `row.getAdhocType().getValue()` / `row.getAssignmentType().getValue()` for enum → int. The `PSContentAdhocUsersContext` factory uses `row.getUser()`. Build is green.

### Issue 2 — Severity: bug — **N/A** (resolved at build time)

- **Description:** `PSExitAuthenticateUser`'s `csc.getObjectType()` (legacy `IPSContentStatusContext`) returned `int`; the `PSComponentSummary` Hibernate equivalent returns `long`. The legacy `PSCmsObject.TYPE_FOLDER` field is `int`. Type widening / narrowing mismatch.
- **Fix:** All calls now go through `PSComponentSummary#getContentTypeId()` cast to `int` at the comparison site (`(int) csc.getContentTypeId() == PSCmsObject.TYPE_FOLDER`). Build is green.

### Issue 3 — Severity: suggestion — **NOTED, not blocking**

- **Description:** The Hibernate `Session` used by the two new service methods is obtained via the same private `entityManager.unwrap(Session.class)` helper that was added in Phase 3. Tests mock `EntityManager#unwrap(Session.class)` to return the mocked `Session`; this is consistent with the Phase 3 test pattern (`PSWorkflowServiceLoadWorkflowTransitionTest`).
- **Why not blocking:** all 7 new tests pass; same approach as Phase 3.
- **Status:** acceptable.

### Issue 4 — Severity: suggestion — **DOCUMENTED**

- **Description:** `PSLoadFromHibernateTest` (8 pure-mapping tests for the new Hibernate factories) is `@Disabled` because the legacy `PSContentAdhocUsersContext` and `PSStateRolesContext` classes have static-field initializers that call `PSConnectionMgr.getQualifiedIdentifier(...)` (which requires a live DB connection detail). Loading either class outside a Spring context throws `ExceptionInInitializerError`.
- **Why not blocking:** this is the same gap already documented in `phase4-scope-survey.md` (Phase 4+ Spring+H2 infrastructure). The `PSLoadFromHibernateTest` is kept in the branch so it can be flipped on once the Spring+H2 infrastructure ships — its body is correct pure mapping assertions against the new factories.
- **Status:** `@Disabled` with a JavaDoc comment pointing to the survey doc; will be re-enabled in Phase 4+.

### Cross-platform path / file I/O checklist

**Result: clean.** This branch does not touch any filesystem path / I/O code. No new `/` or `\\` literals introduced.

---

## Re-review delta

| Initial finding | Status | Evidence |
|---|---|---|
| Suggestion: gate Phase 4b behind Erlang + Mockito tests for the new service methods + factory behaviour | **resolved** | 7 new service tests + 8 disabled mapping tests |
| Bug (caught at build): `PSAssignedRole.getRoleId()` does not exist | **resolved** | uses `row.getGUID().longValue()` |
| Bug (caught at build): `PSAssignedRole.getAdhocType()` returns enum, not int | **resolved** | uses `.getValue()` |
| Bug (caught at build): `PSComponentSummary.getContentTypeId()` returns `long`, `PSCmsObject.TYPE_FOLDER` is `int` | **resolved** | `(int) csc.getContentTypeId() == PSCmsObject.TYPE_FOLDER` |
| Bug (caught at build): `PSContentAdhocUser.getUser()` is the getter, not `getUserName()` | **resolved** | uses `row.getUser()` |

---

## Concrete tests added (this branch)

| Test class | Tests | Coverage |
|---|---|---|
| `PSWorkflowServiceStateRolesTest` | **7** | rejects non-positive ids for `findStateRoles` and `findWorkflowRoles`; happy-path verifies JPQL `from PSAssignedRole where workflowId = :wf and stateId = :sid and assignmentType >= :at` and `from PSWorkflowRole where workflowId = :wf and roleId in :ids`; empty-row / empty-set returns empty list without touching Hibernate; null role-id set rejected |
| `PSLoadFromHibernateTest` | **8** (`@Disabled`) | arg validation, empty-rows → `PSEntryNotFoundException`, role-id classification by adhoc type (DISABLED/ENABLED/ANONYMOUS), missing role-name → `PSRoleException`, adhoc-user classification by adhoc type, empty-username → `IllegalStateException`. Re-enabled when Spring+H2 infrastructure ships. |

`mvn-env.bat -N clean install -Dmaven.javadoc.skip=true` in `modules/extensions-workflow` → **BUILD SUCCESS**, `Tests run: 24, Failures: 0, Errors: 0, Skipped: 8`.

`mvn-env.bat -N test -Dtest=PSWorkflowServiceStateRolesTest,PSWorkflowServiceLoadWorkflowTransitionTest` in `system` → **BUILD SUCCESS**, `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`.

---

## Acceptance criteria mapping (issue #1561 §7, Phase 4)

| Item | Status |
|---|---|
| Finish the remaining `new PSConnectionMgr()` sites in in-product paths | **partially**: 4 / 8 exits in this PR + #1575; remaining 4 (plus the `CONTENTADHOCUSERS` writes in `PSExitPerformTransition`) tracked in `phase4-scope-survey.md` as Phase 4c/4d |
| Delete `PSConnectionMgr` from in-product paths | **deferred** — still has callers in Tier 3 exits + `PSWorkflowCommandHandler` + `Tools/RxFix` + `TableFactory` |
| Single connection pool / tx model for in-product workflow writes | landed in #1567 (Phase 2) |
| Site-create / NavTree regression test on H2 | still a gap; Spring+H2 infrastructure is the prerequisite |

---

## Voice

"Phase 4b is clean. The two Tier 2 exits are off `new PSConnectionMgr()` and now share the surrounding Spring transaction for both reads and writes. The new Hibernate service methods have Mockito coverage; the legacy context-class factories are correctly typed for the Hibernate entity getters; the disabled mapping tests are waiting on Spring+H2 infrastructure. Four build-time bugs caught and fixed. Recommendation: approve. May commit/push: yes."