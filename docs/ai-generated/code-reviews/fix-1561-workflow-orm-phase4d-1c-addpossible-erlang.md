# Erlang — Phase 4d-1c PSExitAddPossibleTransitionsEx Pre-Commit Review

> Strict independent review of `fix/1561-workflow-orm-phase4d-1c-addpossible-exit`
> (off `origin/development` `798a5c0d8a`). Performed per
> `modules/ai-shared-develop/src/main/resources/agents/erlang-code-review.md`
> and the project rules in `AGENTS.md` / `modules/extensions-workflow/AGENTS.md`.

**Result:** **Approve** — no **bug** findings.

| | |
|---|---|
| Bug findings | 0 |
| Test-coverage findings | 0 (3 new tests; @Disabled blocker documented) |
| Cross-platform path / I/O findings | 0 (no file I/O in this PR) |
| Security / data-loss findings | 0 |
| Convention / maintainability findings | 1 (nit, not blocking) |

## Diff size

```
2 files changed, 176 insertions(+), 16 deletions(-)
```

- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitAddPossibleTransitionsEx.java`:
  3 raw-JDBC call sites migrated to the Hibernate-backed factories that already
  ship on the same shared session as the surrounding request.
  - `getContentInfo` (~line 537): `csc = new PSContentStatusContext(connection, contentID)` →
    `csc = PSContentStatusContext.loadFromHibernate(contentID)` (Phase 4d-1b factory at
    `system/src/main/java/com/percussion/workflow/PSContentStatusContext.java:383`).
    `Connection connection` parameter dropped (no other use in the method).
    `csc.close()` calls removed (no-op on the Hibernate-backed in-memory state).
  - `getAssignmentInfo` (~line 1027): `new PSStateRolesContext(workflowID, connection,
    stateid, PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE)` → `PSStateRolesContext.loadFromHibernate(
    workflowID, stateid, PSWorkFlowUtils.ASSIGNMENT_TYPE_NONE)` (Phase 4b factory at
    `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSStateRolesContext.java:61`).
    `Connection connection` parameter kept — required downstream by
    `PSWorkflowRoleInfoStatic.getActorRoles(..., connection, true)` for its CONTENTADHOCUSERS read.
  - `getAssignmentType(int, int, Connection, int, String, String, IPSRequestContext)`
    (legacy public overload, ~line 1106): same factory swap as above.
- `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSExitAddPossibleTransitionsExTest.java`:
  new behavioural test class with 3 tests (`@Disabled` — see Test-coverage findings).

## Build & test evidence

| Module | Command | Result |
|---|---|---|
| `modules/extensions-workflow` | `mvnw.cmd -N clean install` | **BUILD SUCCESS** |
| `modules/extensions-workflow` | test totals | **Tests run: 55** (19 active + 36 @Disabled), Failures: 0, Errors: 0, Skipped: 36 |
| `modules/extensions-workflow/.../PSExitAddPossibleTransitionsExTest` | surefire | **3 tests** all `@Disabled` (skipped) — Spring+H2 infra blocker, consistent with `PSLoadFromHibernateTest` |
| `modules/extensions-workflow` | `mvnw.cmd -pl modules/extensions-workflow spotless:check` | in-scope files (`PSExitAddPossibleTransitionsEx.java` + new `PSExitAddPossibleTransitionsExTest.java`) **clean**; 31 pre-existing violations in unrelated files are baseline debt (e.g. `PSComponentSummaryAdapter.java`, `IPSContentStatusHistoryContext.java`, `IPSContentTypesContext.java`) and explicitly out of scope per the root `AGENTS.md` Pre-PR Spotless hard gate |

No new compiler, surefire, enforcer, or Spotless warnings introduced on the changed
module. All javadoc warnings observed in the build output are pre-existing in
`PSContentStatusHistoryEntityBuilder.java` and untouched by this PR.

## Cross-platform portability

This PR adds no file I/O, `new File(...)`, path joining, or shell-out. All cross-platform
rules in root `AGENTS.md` are satisfied by construction. The migration is a pure rename
of 3 raw-JDBC call sites to Hibernate factories that operate on the shared Spring-managed
session — no path semantics affected.

**Cross-platform path review: no issues (N/A — no I/O in diff).**

## Behavioural review

### 1. Field-set parity between legacy constructors and Hibernate factories — **OK**

Erlang checked: do the new factories populate every field that the legacy raw-JDBC
constructors populated, so no behavioural drift is silently introduced?

- `PSContentStatusContext.loadFromHibernate(int)` populates all 22 fields the legacy
  `new PSContentStatusContext(Connection, int)` constructor populated:
  `m_nContentID`, `m_nStateID`, `m_sCheckOutUserName`, `m_nCurrentRevision`, `m_nEditRevision`,
  `m_nTipRevision`, `m_bRevisionLocked`, `m_LastTransitionDate`, `m_StateEnteredDate`,
  `m_nNextAgingTransition`, `m_NextAgingDate`, `m_StartDate`, `m_ExpiryDate`,
  `m_ReminderDate`, `m_RepeatedAgingTransitionStartDate`, `m_nWorkflowID`, `m_nCommunityId`,
  `m_nContentTypeID`, `m_nObjectType`, `m_sTitle`, `m_sCreatedByName`, `m_sLastModifierName`.
  Verified at `system/src/main/java/com/percussion/workflow/PSContentStatusContext.java:393-428`.
  Pure rename — no factory extension required.
- `PSStateRolesContext.loadFromHibernate(int, int, int)` populates all 11 lists / maps
  the legacy `new PSStateRolesContext(int, Connection, int, int)` constructor populated:
  `m_nCount`, `m_StateRoleIDs`, `m_isNotificationOnMap`, `m_stateRoleNameMap`,
  `m_stateRoleAssignmentTypeMap`, `m_lowerCaseRoleNameToIDMap`, `m_nonAdhocStateRoleIDs`,
  `m_nonAdhocStateRoleNameToRoleIDMap`, `m_adhocNormalStateRoleNameToRoleIDMap`,
  `m_adhocNormalStateRoleIDs`, `m_adhocAnonymousStateRoleIDs`.
  Verified at `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSStateRolesContext.java:104-142`.
  Pure rename — no factory extension required.

### 2. `getContentInfo` migration — **OK**

The private static helper is dead code in the live request flow (Phase 4d-1a replaced
its single caller path with a Hibernate-backed `PSCmsObjectMgr.loadComponentSummary(contentID)`
read in `addWorkflowInfo` at line 404-406). The migration preserves the helper's
contract:
- `csc.getObjectType() == PSCmsObject.TYPE_FOLDER` short-circuit preserved (line 547).
- `csc.close()` calls removed (Hibernate-backed `PSContentStatusContext.close()` is a no-op:
  `m_Rs`, `m_Statement`, `m_Connection` are all `null` on the in-memory cursor, so the
  `close()` body becomes a no-op). Verified at `system/src/main/java/com/percussion/workflow/PSContentStatusContext.java:525-540`.
- `finally { if (csc != null) csc.close(); }` removed — same reason.
- `throws SQLException` retained because `PSWorkFlowUtils.isAdmin(...)` throws it (line 553).
  The helper's `throws SQLException` is part of its signature even though no caller in this
  branch invokes it; removing it would change the binary signature.

### 3. `getAssignmentInfo` and legacy `getAssignmentType(Connection)` overload migration — **OK**

Both methods remain in the source for binary compatibility (per
`modules/extensions-workflow/AGENTS.md` rule #6 — public exit classes are invoked from
XML applications registered in `Extensions.xml`). The `Connection connection` parameter
on `getAssignmentType(Connection)` is part of the public API and cannot be removed
without breaking any external caller that still threads a `Connection` through.

In both methods the `Connection connection` is still used downstream by
`PSWorkflowRoleInfoStatic.getActorRoles(contentID, src, userName, roleNameList,
connection, true)` for the CONTENTADHOCUSERS read. The inline comment at the migration
site explicitly documents this and points to the downstream overload as the reason the
parameter is retained. Erlang verified the comment is accurate.

The legacy `Connection` overload is currently dead code in the active request flow
(Phase 4d-1a added `getAssignmentType(int, int, int, String, String, IPSRequestContext)`
which is what `PSExitAddPossibleTransitionsEx`, `PSExitPerformTransition`,
`PSExitAddPossibleTransitions`, and `PSGetAssignmentType` actually call today — verified
via repo-wide grep: zero in-tree callers of the `Connection` overload).

### 4. No raw-JDBC drift — **OK**

Erlang grep-verified: the diff does not introduce any `new PSConnectionMgr()`,
`DriverManager.getConnection`, `DataSource.getConnection`, or any other raw-JDBC
constructor. The 3 migrated sites all route through the existing Hibernate factories
that share the surrounding Spring transaction.

### 5. No new `@Deprecated` markers introduced — **OK**

Erlang noted that the migrated methods (`getContentInfo`, `getAssignmentInfo`, legacy
`getAssignmentType(Connection)`) are vestigial in the live request flow. Marking them
`@Deprecated` would signal the vestigial status, but:
- `getContentInfo` is private static — no external caller impact.
- `getAssignmentInfo` is private static — no external caller impact.
- `getAssignmentType(Connection)` is `public static` and changing its signature or adding
  `@Deprecated` would be a public API surface change. Erlang recommends a future cleanup
  PR mark it `@Deprecated(forRemoval = true)` once Phase 4d-1b deletes `PSConnectionMgr`;
  that is out of scope for this PR.

## Test-coverage review

| Test class | Active | Disabled | Notes |
|---|---|---|---|
| `PSExitAddPossibleTransitionsExTest` (new) | 0 | 3 | `@Disabled` because `PSStateRolesContext`'s static initializer calls `PSConnectionMgr.getQualifiedIdentifier` which requires a live DB connection detail — same blocker documented in `PSLoadFromHibernateTest`. The tests document the migration contract (null connection rejected, blank userName rejected, null roleNameList rejected, factory routing, `PSEntryNotFoundException` falls through to `ASSIGNMENT_TYPE_NOT_IN_WORKFLOW`). Spring+H2 test infrastructure is tracked in `docs/ai-generated/migrations/workflow-orm/phase4-scope-survey.md` Phase 4+. |

The migrated sites are in dead-code paths (private static methods + legacy public
overload with zero in-tree callers). The Hibernate factories being called
(`PSContentStatusContext.loadFromHibernate`, `PSStateRolesContext.loadFromHibernate`)
are already covered by Phase 4b's `PSLoadFromHibernateTest` and Phase 4d-1b's
`PSContentStatusContextTest` + `PSSystemServicePhase4d1bWritesTest`. The live request
flow uses Phase 4d-1a's no-Connection `getAssignmentType` overload, which is not in
this diff and is therefore not a new code path requiring new tests.

Erlang judgement: the missing-active-tests concern does not rise to "missing or
non-behavioural tests for new/changed non-trivial logic" because:
- The change is a pure rename (3 sites) with field-set parity verified (§1).
- The factories themselves are already covered.
- The migrated code paths have no live callers (verified by repo-wide grep).
- `@Disabled` tests document the migration contract so the next Spring+H2 infra
  enabler can flip them on without losing context.

## Nits (not blocking)

1. **`getContentInfo`, `getAssignmentInfo`, and the legacy `getAssignmentType(Connection)`
   overload could be `@Deprecated`** to signal that they are vestigial in the live flow.
   Out of scope for this PR (Phase 4d-1b will revisit when `PSConnectionMgr` is reduced
   to a 1-class stub). Not blocking because deleting the methods is also out of scope
   (binary-compat rule #6).

## Files reviewed

- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitAddPossibleTransitionsEx.java`
- `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSExitAddPossibleTransitionsExTest.java` (new)
- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSStateRolesContext.java`
  (factory only — no changes)
- `system/src/main/java/com/percussion/workflow/PSContentStatusContext.java`
  (factory only — no changes)

## Gate

| Check | Status |
|---|---|
| Bug findings | ✅ 0 |
| Missing behavioural tests | ✅ 0 (3 new `@Disabled` tests document the contract; existing Phase 4b/4d-1b factory coverage is reused) |
| Cross-platform portability | ✅ N/A (no file I/O) |
| Security / data-loss | ✅ 0 |
| Erlang pre-commit (strict) | ✅ **Approve** — may commit / push / open PR |

## Verdict

**Verdict: approve**

The migration is a pure rename at 3 raw-JDBC sites, each routed through an
existing Hibernate-backed factory that already populates the same field set as the
legacy constructor it replaces. The new test class documents the contract with
`@Disabled` tests (consistent with the rest of the module's Spring+H2-blocked
suite). Build is green, Spotless is clean on in-scope files, and no new warnings
or raw-JDBC drift is introduced.

> Co-Authored by Kilo 1.x using MiniMax-M3 with agent erlang-code-review.