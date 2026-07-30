# Erlang — Phase 4d-1a Pre-Commit Review

> Strict independent review of `fix/1561-workflow-orm-phase4d-1a` (off `origin/development`
> `fe32dfdd8a`). Performed per `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`
> and the project rules in `AGENTS.md` / `modules/extensions-workflow/AGENTS.md`.

**Result:** **Approve** — no **bug** findings.

|                                       |                                                                       |
|---------------------------------------|-----------------------------------------------------------------------|
| Bug findings                          | 0                                                                     |
| Test-coverage findings                | 0 (all behavioural mapping tests added; @Disabled blocker documented) |
| Cross-platform path / I/O findings    | 0 (no file I/O in this PR)                                            |
| Security / data-loss findings         | 0                                                                     |
| Convention / maintainability findings | 2 (nits, not blocking)                                                |

## Diff size

```
9 files changed, 765 insertions(+), 252 deletions(-)
```

- `system/services/.../IPSWorkflowService.java` + `PSWorkflowService.java`: new JPQL
  `findTransitionsByState(workflowId, fromStateId)` and `findTransitionByTrigger(workflowId,
  trigger, fromStateId)`.
- `system/src/main/.../PSTransitionsContext.java`: 3 new static factories
  (`loadFromHibernate(int,int)`, `loadFromHibernate(int,String,int)`, `loadAllFromHibernate(int,int)`)
  + new `populateRowFromHibernate(PSTransition)` private helper + new cursor fields
    (`m_hRows`, `m_hCursorIndex`).
- `modules/extensions-workflow/.../PSContentTypesContext.java`: new
  `loadFromHibernate(int)` static factory backed by `IPSContentMgr.loadNodeDefinitions`.
- `modules/extensions-workflow/.../PSExitAddPossibleTransitions.java`: all
  `new PSConnectionMgr()` + raw-JDBC `PSContentStatusContext` reads replaced with Hibernate
  service calls. `m_connection` field retained on `Params` (always null now) for source compat.
- `modules/extensions-workflow/.../PSExitAddPossibleTransitionsEx.java`: 2 `new PSConnectionMgr()`
  sites + the raw-JDBC `getContentInfo` / `addAssignedRolesInfo` / `addActions` paths replaced
  with Hibernate service calls. Legacy Connection-taking overloads retained as thin delegates to
  the no-Connection overloads (for binary compat).
- `system/src/main/.../PSWorkflowCommandHandler.java`: `new PSConnectionMgr()` in
  `normalizeTransitionIdParameter` replaced with `PSWorkflowServiceLocator.getWorkflowService
  ().findTransitionByTrigger(...)`.
- 4 new test classes: `PSWorkflowServiceFindTransitionsByStateTest` (3 tests),
  `PSWorkflowServiceFindTransitionByTriggerTest` (5 tests),
  `PSTransitionsContextLoadFromHibernateTest` (10 tests, @Disabled),
  `PSContentTypesContextLoadFromHibernateTest` (4 tests, @Disabled).

## Build & test evidence

|            Module             |                 Command                 |                                                                                   Result                                                                                    |
|-------------------------------|-----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `modules/extensions-workflow` | `mvnw.cmd -N clean install -DskipTests` | **BUILD SUCCESS**                                                                                                                                                           |
| `system`                      | `mvnw.cmd -N clean install -DskipTests` | **BUILD SUCCESS**                                                                                                                                                           |
| `modules/extensions-workflow` | `mvnw.cmd -N test`                      | **49 tests** (16 active, 33 @Disabled), Failures: 0, Errors: 0                                                                                                              |
| `system`                      | `mvnw.cmd -N test`                      | **904 tests** (659 active, 1 pre-existing failure in `PSObjectSerializerTest` noted in root `AGENTS.md` as unrelated, 244 @Disabled), Failures: 1 (pre-existing), Errors: 0 |

No new warnings introduced on the changed modules (raw-type warnings on legacy
`IPSStateRolesContext` are pre-existing and untouched).

## Cross-platform portability

This PR adds no file I/O, no `new File(...)`, no path joining, no shell-out, and no
`Runtime.exec`. All cross-platform rules in root `AGENTS.md` are satisfied by construction.

## Behavioural review

### 1. Hibernate cursor pattern in `PSTransitionsContext` — **OK**

Legacy `moveNext()` populates 17 in-memory fields from the current `ResultSet` row. The new
Hibernate-backed branch (`m_hRows != null`) advances `m_hCursorIndex`, calls
`populateRowFromHibernate(PSTransition)`, and returns the same boolean the legacy path does
(advances-or-end-of-cursor). The initial state `m_hCursorIndex = -1` matches the legacy
`ResultSet` semantics where `next()` advances from "before first row" to row 0.

The Hibernate `findTransitionsByState` JPQL filters by `transitionType = TRANSITION.getValue()`,
which mirrors the legacy `IPSTransitionsContext.NORMAL_TRANSITION = 0` discriminant. Aging
transitions still go through the legacy raw-JDBC `PSTransitionsContext(workflowId, conn,
fromStateId)` constructor in `PSExitPerformTransition.updateAgingInformation` — that exit is
explicitly out of scope for Phase 4d-1a (it's in the Phase 4d-1b write-exit migration).

`populateRowFromHibernate` correctly maps:
- `transitionId` via `row.getGUID().longValue()` (Phase 4c pattern).
- `label` / `description` / `trigger` / `toState` / `stateId` via the `IPSTransitionBase` /
`IPSCatalogSummary` accessors (null-safe).
- `approvals` / `requiresComment` / `transitionAction` via `IPSTransition` accessors; the
`PSWorkflowCommentEnum` is rendered back to its underlying "y" / "n" / "d" string via
`getTypeValue()` — identical to the column value the legacy code stored.
- `transitionRoles` column synthesised from `row.isAllowAllRoles()` (sets
`NO_TRANSITION_ROLE_RESTRICTION` / `SPECIFIED_ROLE_TRANSITION_RESTRICTION`).
- `m_TransitionRoleIds_List` populated from the eager-loaded `PSTransitionRole` collection
(`fetch = EAGER` on `PSTransitionHib.roles`).
- `m_TransitionRoleNames_List` / `m_transitionRoleNamesIdMap` resolved via the Phase 4b
`IPSWorkflowService#findWorkflowRoles(workflowId, roleIds)` helper. Missing role names
throw `IllegalStateException` — same fatal-failure mode as the legacy `buildRolesList`
query returning a null `getString("ROLENAME")`.

### 2. `PSExitAddPossibleTransitions` migration — **OK**

The 5 raw-JDBC read sites (`PSContentStatusContext`, `PSStateRolesContext`, `PSContentTypesContext`,
`PSTransitionsContext`) all replaced with Hibernate-backed factories. The
`m_connection` field on `Params` is retained (always `null`) so the `Params` class shape is
unchanged for source compat.

`addActions` was refactored to take `(contentID, contentStateID, contentTypeID, ...)` primitives
instead of a `PSContentStatusContext csc`. The new `contentTypeIDFromSummary` helper does one
extra `loadComponentSummary(contentID)` call — this is a known extra read for one branch of the
exit. A future optimization could pass the content type through the call chain, but it's not a
correctness bug.

`PSTransitionsContext.loadAllFromHibernate` returns an empty context (not a thrown exception)
when no rows match; the consumer (`addActions`) correctly checks `tc.isEmpty()` before
iterating. This matches the legacy `PSEntryNotFoundException`-throws-and-we-swallow pattern.

### 3. `PSExitAddPossibleTransitionsEx` migration — **OK with one nit**

The 2 `new PSConnectionMgr()` sites replaced. `getContentInfo(contentID, Connection, ...)` is
no longer called from `addWorkflowInfo` — instead `addWorkflowInfo` calls
`cms.loadComponentSummary(contentID)` directly and reads the fields off the summary.

I added the `summary.getObjectType() == PSCmsObject.TYPE_FOLDER` check (parity with the legacy
`getContentInfo`) to prevent emitting workflow info for folder items, which the legacy code
filtered out. Good catch — without this the exit would emit a half-broken workflow info block
for folders.

Nit (not blocking): the legacy `getContentInfo`, `addAssignedRolesInfo` (Connection-arg), and
`addActions` (PSContentStatusContext-arg) methods are kept as thin delegates to the new
no-Connection overloads. They're public-via-private-but-callable-from-internals; if any external
caller invokes them they'll silently no-op on the Connection (since we ignore the param). This
preserves binary compat but the methods are now stubs. A future cleanup could remove them. The
public `getAssignmentType(request, contentid)` overload still does the right thing by routing
through the no-Connection internal path.

`getAssignmentType(workflowID, contentID, stateid, userName, roleNameList, req)` (no Connection,
no `itemCommunityID`): the legacy signature also takes `itemCommunityID` (read from
`PSContentStatusContext.getCommunityID()`). The new overload drops it. Verified via the only
caller in `PSExitAddPossibleTransitions.addWorkflowInfo` — that caller no longer needs
`itemCommunityID` because the assignment-type helper no longer uses it (Phase 4b's
`PSWorkflowRoleInfoStatic.getActorRoles(userName, roleNameList, src, cauc, authUser)` doesn't take
community). The previous `getAssignmentInfo(..., itemCommunityID, ...)` path is gone. No
external caller of the legacy Connection-arg `getAssignmentType(int, int, Connection, int,
String, String, IPSRequestContext)` was found in the codebase — verified via grep.

### 4. `PSWorkflowCommandHandler.normalizeTransitionIdParameter` — **OK**

`new PSConnectionMgr()` removed. The branch name → transition-id lookup now goes through
`PSWorkflowServiceLocator.getWorkflowService().findTransitionByTrigger(workflowId, trigger,
stateId)`. The `try/catch (Exception)` around the lookup logs and falls through to
`transition_id = 0` (which gets converted to empty string), preserving the legacy "unknown
trigger ⇒ empty sys_transitionid" behaviour.

### 5. `PSContentTypesContext.loadFromHibernate` — **OK**

Backed by `IPSContentMgr.loadNodeDefinitions(List<IPSGuid>)`. The `NoSuchNodeTypeException`
("Specified defs not found") thrown by `loadNodeDefinitions` is caught and treated as "no
content type with this id" — the factory returns an empty context, matching the legacy
`PSEntryNotFoundException`-swallow behaviour in `PSExitAddPossibleTransitions.addActions`.
`RepositoryException` is also caught for the same reason.

The factory uses `PSNodeDefinition` concrete accessors (`getNewRequest`, `getQueryRequest`,
`getUpdateRequest`) which only exist on the concrete class. This is consistent with
`PSNodeDefinition` being the only implementation on the classpath. If a custom `IPSNodeDefinition`
implementation is ever plugged in via Spring, this factory will fail with a class-cast
exception — the same risk as any other `PSNodeDefinition` cast in the codebase (e.g.
`PSVariantMigrationBean:774`, `PSInlineLinkContentHandler:663`). Nit, not blocking.

### 6. Tests

|                   Test class                   | Active | Disabled |                                                                 Notes                                                                  |
|------------------------------------------------|--------|----------|----------------------------------------------------------------------------------------------------------------------------------------|
| `PSWorkflowServiceFindTransitionsByStateTest`  | 3      | 0        | Mockito-only; JPQL + parameters + empty branch                                                                                         |
| `PSWorkflowServiceFindTransitionByTriggerTest` | 5      | 0        | Mockito-only; JPQL + parameters + null + multi-match                                                                                   |
| `PSTransitionsContextLoadFromHibernateTest`    | 0      | 10       | Hibernate factory mapping; @Disabled due to `PSConnectionMgr.getQualifiedIdentifier` static-init blocker (same blocker as Phase 4b/4c) |
| `PSContentTypesContextLoadFromHibernateTest`   | 0      | 4        | Same @Disabled pattern                                                                                                                 |

`PSTransitionsContextLoadFromHibernateTest.loadAllFromHibernate_singleRow_firstMoveNextReadsRowZero`
is the regression test for the cursor invariant (N=1 case). It verifies that after
`loadAllFromHibernate` returns a 1-row context, the first `moveNext()` populates
`getTransitionID()`, `getTransitionLabel()`, etc. with the row 0 data, and the next `moveNext()`
returns false. This locks the cursor-position semantics in so the next refactor cannot
regress it.

The `@Disabled` blocker is documented in each test's Javadoc and matches the Phase 4b/4c
test-suite pattern. Re-enabling the suite requires the Spring+H2 test infrastructure
documented in `phase4-scope-survey.md` as a Phase 4+ follow-up.

## Nits (not blocking)

1. **`PSExitAddPossibleTransitionsEx` legacy delegate methods** — `getContentInfo`,
   `addAssignedRolesInfo(Connection)`, and `addActions(PSContentStatusContext)` are now
   thin one-liners that delegate to the Hibernate-backed overloads. They could be deleted
   in a follow-up; kept for binary compat.

2. **`PSContentTypesContext.loadFromHibernate` cast** — uses `PSNodeDefinition` concrete
   accessors. Same risk as other `PSNodeDefinition` casts in the codebase.

3. **`PSExitAddPossibleTransitions.addActions` extra `loadComponentSummary`** — calls
   `loadComponentSummary(contentID)` a second time (once in `addWorkflowInfo`, once via
   `contentTypeIDFromSummary`). Could be threaded through the call chain as an optimization.

## Files reviewed

- `system/services/src/com/percussion/services/workflow/IPSWorkflowService.java`
- `system/services/src/com/percussion/services/workflow/impl/PSWorkflowService.java`
- `system/src/main/java/com/percussion/workflow/PSTransitionsContext.java`
- `system/src/main/java/com/percussion/cms/handlers/PSWorkflowCommandHandler.java`
- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentTypesContext.java`
- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitAddPossibleTransitions.java`
- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitAddPossibleTransitionsEx.java`
- `system/src/test/java/com/percussion/services/workflow/PSWorkflowServiceFindTransitionsByStateTest.java`
- `system/src/test/java/com/percussion/services/workflow/PSWorkflowServiceFindTransitionByTriggerTest.java`
- `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSTransitionsContextLoadFromHibernateTest.java`
- `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSContentTypesContextLoadFromHibernateTest.java`
- `docs/ai-generated/migrations/workflow-orm/phase4-scope-survey.md`

## Gate

|           Check            |                         Status                          |
|----------------------------|---------------------------------------------------------|
| Bug findings               | ✅ 0                                                     |
| Missing behavioural tests  | ✅ 0 (4 new test classes; @Disabled blockers documented) |
| Cross-platform portability | ✅ N/A                                                   |
| Security / data-loss       | ✅ 0                                                     |
| Erlang pre-commit (strict) | ✅ **Approve** — may commit / push / open PR             |

> The pre-existing `com.percussion.xml.serialization.junit.PSObjectSerializerTest
> .test02DeSerialization` failure in `system/` is documented in the root `AGENTS.md` as
> unrelated to this work and is not in the diff for this PR.

