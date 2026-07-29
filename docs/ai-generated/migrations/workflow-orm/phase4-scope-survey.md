# Phase 4 scope survey — what's actually involved

> Status: **Phase 4a (Tier 1) shipped in PR #1575.** **Phase 4b (Tier 2 — state-roles + auth-flag + authenticate-user exits) shipped in PR #1578.** **Phase 4c (Tier 3a — `PSExitNotifyAssignees` + NOTIFICATIONS + TRANSITIONNOTIFICATIONS) shipped in PR #1583.** **Phase 4d (Tier 3b — read exits + write exit + `PSConnectionMgr` deletion) split into 4d-1a (reads), 4d-1b (writes), 4d-1c (PSSystemWs + legacy overloads) and 4d-1d (delete PSConnectionMgr entirely).** 4d-1a, 4d-1b, 4d-1c are merged (#1630, #1632, #1640, #1645). 4d-1d is in progress.

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

### `PSConnectionMgr` deletion path (4d-1d, in progress)

`PSConnectionMgr.getQualifiedIdentifier(...)` was a static method that read
`PSConnectionHelper.getConnectionDetail(null)` once and cached DB metadata, but with all
catalog/schema flags hardcoded to `false` it only ever returned the input upper-cased —
i.e., the qualifier was effectively a no-op for the workflow tables. The 9 legacy context
classes have static-init lines that called `PSConnectionMgr.getQualifiedIdentifier("X")`
to resolve table names.

After 4d-1a + 4d-1b + 4d-1c, no in-product caller of `new PSConnectionMgr()` remains.
Decision taken in 4d-1d:

- **Delete `PSConnectionMgr` entirely.** Replace the 9 static-init lines with inlined
  `private static final String TABLE_X = "X";` constants (behaviour-preserving: upper-cased
  table names were the actual returned values). Replace `PSAbstractWorkflowContext` /
  `PSAbstractWorkflowTest` pool-connection call sites with `PSConnectionHelper.getDbConnection()`
  / `PSConnectionHelper.releaseDbConnection(...)`. The new `releaseDbConnection` helper is
  added to `PSConnectionHelper` and swallows `SQLException` on close (matching the legacy
  semantics). No static-init fragility remains — the legacy contexts' class initializers
  no longer touch the JDBC layer.

This is the path Phase 4d-1d is taking. The previous "keep as 1-method utility stub"
fallback (option 1) was rejected as a half-measure — it leaves a thin static-init
coupling that still drags in the connection-detail lookup at class-load time, which is
the original "Spring init order" / "hard to test" complaint the user flagged.

## Out of scope (issue #1561 "Non-goals")

- `system/Tools/RxFix/...` — explicitly out of scope
- `modules/TableFactory/...` — explicitly out of scope

## Current state

- Branch: `fix/1561-workflow-orm-phase4d-1a` off `origin/development` (`fe32dfdd8a`)
- Working tree: clean (prior review-modified WINDOWS-BUILD-GUIDE.md untouched)
- `mvnw.cmd -N clean install -DskipTests` to be run before first commit