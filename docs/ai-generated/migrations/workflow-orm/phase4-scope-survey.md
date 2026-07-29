# Phase 4 scope survey — what's actually involved

> Status: **Phase 4a (Tier 1) shipped in PR #1575.** **Phase 4b (Tier 2 — state-roles + auth-flag + authenticate-user exits) shipped in PR #1578.** **Phase 4c (Tier 3a — `PSExitNotifyAssignees` + NOTIFICATIONS + TRANSITIONNOTIFICATIONS) shipped in PR #1583.** **Phase 4d (Tier 3b — read exits + write exit + `PSConnectionMgr` deletion) split into 4d-1a (reads) + 4d-1b (writes + delete).** 4d-1a is in progress.

## What ships in Phase 4a (PR #1575, merged)

| Exit | Reads | Writes | Done |
|---|---|---|---|
| `PSExitDisallowUpdatePublished` | `CONTENTSTATUS` + state via `cms.loadWorkflowState` | — | ✅ |
| `PSGetCheckoutStatus` | `CONTENTSTATUS.checkoutUserName` only | — | ✅ |

Both exits no longer call `new PSConnectionMgr()` for the `CONTENTSTATUS` read — they use
`PSCmsObjectMgr#loadComponentSummary(int)` which returns a Hibernate `PSComponentSummary` on
the same Spring-managed datasource as the surrounding request.

## What ships in Phase 4b (this branch — PR to follow)

### New Hibernate service methods (in `system/services`)

- `IPSWorkflowService#findStateRoles(long workflowAppId, long stateId, int minAssignmentType)`
  returns `List<PSAssignedRole>`. JPQL: `from PSAssignedRole where workflowId = :wf and
  stateId = :sid and assignmentType >= :at order by roleId`. Replaces the raw SQL in
  `PSStateRolesContext.QRYSTRING`.
- `IPSWorkflowService#findWorkflowRoles(long workflowAppId, Set<Long> roleIds)` returns
  `List<PSWorkflowRole>`. JPQL: `from PSWorkflowRole where workflowId = :wf and roleId in :ids`.
  Replaces the raw SQL in `PSStateRolesContext.QRYSTRING` for the `ROLES` join.
- `IPSSystemService#findContentAdhocUsers(int contentId)` returns `List<PSContentAdhocUser>`.
  JPQL: `from PSContentAdhocUser where contentId = :cid order by adhocUserId`. Replaces the raw
  SQL in `PSContentAdhocUsersContext.QRYSTRING`.

### Hibernate-backed factories (in `modules/extensions-workflow`)

- `PSStateRolesContext.loadFromHibernate(int workflowId, int stateId, int assignmentType)`
  populates the existing in-memory state shape (the 11 maps / lists) from the Hibernate rows.
  Validates role-name hydration; throws `PSRoleException` on missing role names; throws
  `PSEntryNotFoundException` when no rows match.
- `PSContentAdhocUsersContext.loadFromHibernate(int contentId)` populates the existing in-memory
  state shape from `PSContentAdhocUser` rows; classifies by `adhocType` (DISABLED / ENABLED /
  ANONYMOUS).

### Exits migrated off `new PSConnectionMgr()`

| Exit | Reads | Writes | Done |
|---|---|---|---|
| `PSExitAddEditAuthFlag` | `CONTENTSTATUS` (Hibernate) + `STATEROLES` + `CONTENTADHOCUSERS` (Hibernate) | — | ✅ |
| `PSExitAuthenticateUser` | `CONTENTSTATUS` (Hibernate) + `STATEROLES` + `CONTENTADHOCUSERS` (Hibernate) | — | ✅ |

Both exits now use the no-connection overload of
`PSWorkflowRoleInfoStatic.getActorRoles(userName, roleNameList, src, cauc, authUser)` since
both `src` and `cauc` are populated from Hibernate (no second pool connection).

## What remains (Tier 3b)

### Phase 4d-1a — reads only (in progress)

| Exit / site | Read paths | Status |
|---|---|---|
| `PSExitAddPossibleTransitions` | `CONTENTSTATUS` (Hibernate) + `STATEROLES` (Hibernate) + `CONTENTTYPES` (Hibernate) + `TRANSITIONS` for state (Hibernate) | migrating |
| `PSExitAddPossibleTransitionsEx` | `CONTENTSTATUS` + `STATEROLES` + `CONTENTTYPES` + `TRANSITIONS` for state + `PSWorkflowRoleInfoStatic.getActorRoles(contentId, src, ...)` Connection-arg overload | migrating |
| `PSWorkflowCommandHandler.normalizeTransitionIdParameter` | `TRANSITIONS` by trigger name (in `system/cms/handlers`) | migrating |

### Phase 4d-1b — writes + `PSConnectionMgr` deletion (next)

| Exit / site | Write paths | Notes |
|---|---|---|
| `PSExitPerformTransition` | `CONTENTSTATUS.STATEID` + `CONTENTADHOCUSERS` (insert + delete) + `CONTENTAPPROVALS` (insert + delete) | The only exit with writes; needs new `IPSSystemService.updateContentStatusState`, `IPSWorkflowService.saveContentAdhocUsers`, `IPSWorkflowService.deleteContentAdhocUsers` services |
| `PSConnectionMgr` deletion | — | After 4d-1a + 4d-1b land, no in-product callers of `new PSConnectionMgr()` remain. The class still needs `getQualifiedIdentifier` static for the 9 legacy class static inits — either keep the class as a 1-method utility stub or migrate the static inits to use `PSConnectionHelper` directly. |

### In-scope Hibernate service methods for 4d-1a

- `IPSWorkflowService#findTransitionsByState(long workflowId, long stateId)` — list of transitions for a state (Hibernate JPQL `from PSTransitionHib where workflowId = :wf and stateId = :sid`).
- `IPSWorkflowService#findTransitionByTrigger(long workflowId, String trigger, long fromStateId)` — single transition by trigger (Hibernate JPQL).
- `IPSContentMgr` / `IPSSystemService` equivalent for `CONTENTTYPES` lookup (or use `PSNodeDefinition` directly via Hibernate session).

### In-scope Hibernate factory methods for 4d-1a

- `PSTransitionsContext.loadFromHibernate(int workflowId, int transitionId)` (single, by id)
- `PSTransitionsContext.loadFromHibernate(int workflowId, String trigger, int fromStateId)` (single, by trigger)
- `PSTransitionsContext.loadAllFromHibernate(int workflowId, int fromStateId)` (cursor, all transitions for a state)
- `PSContentTypesContext.loadFromHibernate(int contentTypeId)` (single, no cursor)

### In-scope Hibernate factory methods for 4d-1b

- `PSContentStatusContext.loadFromHibernate(int contentId)` (already added in Phase 4b) + `PSContentStatusContext.commit()` (new) — Hibernate-backed `UPDATE CONTENTSTATUS SET ...`
- `PSContentAdhocUsersContext.loadFromHibernate(int contentId)` (already added in Phase 4b) + `commit()` + `emptyAdhocUserEntries(...)` (new) — Hibernate-backed `INSERT INTO CONTENTADHOCUSERS` and `DELETE FROM CONTENTADHOCUSERS`
- `PSContentApprovalsContext.loadFromHibernate(int workflowId, int contentId, PSTransitionsContext)` (new) + `commit()` + `addContentApproval(...)` + `emptyApprovals()` (new) — Hibernate-backed `INSERT INTO CONTENTAPPROVALS` and `DELETE FROM CONTENTAPPROVALS`

### `PSConnectionMgr` deletion path (4d-1b)

`PSConnectionMgr.getQualifiedIdentifier(...)` is a static method that reads
`PSConnectionHelper.getConnectionDetail(null)` once and caches DB metadata. It does
NOT require a JDBC connection. The 9 legacy context classes have static-init lines
that call `PSConnectionMgr.getQualifiedIdentifier("X")` to resolve table names.

After 4d-1a + 4d-1b, no in-product caller of `new PSConnectionMgr()` remains. The
class can be reduced to:

1. Keep `PSConnectionMgr` as a 1-class utility stub with only the static `getQualifiedIdentifier` method (no `<init>` methods declared). This satisfies the 9 legacy static inits and is the smallest-blast-radius deletion.
2. OR delete `PSConnectionMgr` entirely and migrate the 9 static-inits to call `PSConnectionHelper.getConnectionDetail(null)` directly. This is a larger churn but eliminates the dependency on `PSConnectionMgr`.

Decision pending — picked option 1 by default for 4d-1b unless the Erlang review flags it.

## Out of scope (issue #1561 "Non-goals")

- `system/Tools/RxFix/...` — explicitly out of scope
- `modules/TableFactory/...` — explicitly out of scope

## Current state

- Branch: `fix/1561-workflow-orm-phase4d-1a` off `origin/development` (`fe32dfdd8a`)
- Working tree: clean (prior review-modified WINDOWS-BUILD-GUIDE.md untouched)
- `mvn-env.bat -N clean install -DskipTests` to be run before first commit