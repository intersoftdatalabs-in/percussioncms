# Phase 4 scope survey — what's actually involved

> Status: Tier 1 (2 read-only exits) migrated and compile clean. Tier 2 (4 read-only exits) and Tier 3 (2 complex exits + writes) require deeper refactors than can fit in one Phase 4 PR. Captured here so the next Phase 4 PRs can be sized realistically.

## What ships in this PR (Phase 4a — Tier 1)

| Exit | Reads | Writes | Done |
|---|---|---|---|
| `PSExitDisallowUpdatePublished` | `CONTENTSTATUS` + state via `cms.loadWorkflowState` | — | ✅ |
| `PSGetCheckoutStatus` | `CONTENTSTATUS.checkoutUserName` only | — | ✅ |

Both exits no longer call `new PSConnectionMgr()` for the `CONTENTSTATUS` read — they use
`PSCmsObjectMgr#loadComponentSummary(int)` which returns a Hibernate `PSComponentSummary` on
the same Spring-managed datasource as the surrounding request.

## What remains in this PR (Tier 2 / Tier 3)

### Tier 2 exits — state-roles helpers are the blocker

| Exit | Read paths | Status |
|---|---|---|
| `PSExitAddEditAuthFlag` | `CONTENTSTATUS` + `PSStateRolesContext` + `PSWorkflowRoleInfoStatic.getActorRoles(...)` | partial only — `csc` migrated, `PSStateRolesContext` and `PSWorkflowRoleInfoStatic` still raw-JDBC |
| `PSExitAuthenticateUser` | `CONTENTSTATUS` + `PSStateRolesContext` + `PSWorkflowRoleInfoStatic.getActorRoles(...)` + `PSStateRolesContext` (in `canUserCreate`) | partial only — `csc` migrated, state-roles helpers still raw-JDBC |

Both Tier 2 exits share the same dependency chain:

```
PSStateRolesContext(int workflowId, Connection conn, int stateId, int assignmentType)
   ↑  raw JDBC: SELECT ... FROM STATEROLES sr, ROLES r WHERE sr.WORKFLOWAPPID=? AND sr.STATEID=? AND sr.ASSIGNMENTTYPE>=? AND sr.ROLEID=r.ROLEID AND sr.WORKFLOWAPPID=r.WORKFLOWAPPID
PSWorkflowRoleInfoStatic.getActorRoles(contentId, src, userName, roleNames, connection, true)
   ↑  raw JDBC over backend security tables — separate concern from state roles
```

`PSStateRolesContext` is a 300-line raw-JDBC class with no Hibernate equivalent on the
classpath today. Migrating it requires:

1. A new Hibernate entity for `STATEROLES` + `ROLES` (or a JPQL query through `PSState`
   which already owns the `STATEROLES` collection via `@OneToMany`).
2. A new `IPSWorkflowService#loadStateRoles(workflowId, stateId, assignmentType)` method
   that returns the state-roles data DTO that `PSStateRolesContext` exposes (currently a
   `Map<Integer,Integer>` assignment type map + a list of role names).
3. A wrapper / replacement class — either refactor `PSStateRolesContext` itself to use
   Hibernate internally (touching every caller in the codebase), or introduce a parallel
   Hibernate-backed class.

This is a self-contained sub-PR, but it's bigger than the Tier 1 PR by itself.

### Tier 3 exits — notifications, transitions for state, writes

| Exit | Read paths | Write paths | Notes |
|---|---|---|---|
| `PSExitNotifyAssignees` | `PSTransitionsContext` + `PSNotificationsContext` + `PSStateRolesContext` | none (mails only) | Two more raw-JDBC contexts to migrate, plus the email-assembly helper chain |
| `PSExitAddPossibleTransitions{,Ex}` | transitions for a state + adhoc users + state roles | none | Most complex read query in the module — joins multiple tables |
| `PSExitPerformTransition` | state roles + transitions + adhoc users + transitions `CONTENTSTATUS` | `CONTENTADHOCUSERS` + transitions `CONTENTSTATUS.STATEID` | The only exit with writes. `CONTENTADHOCUSERS` Hibernate entity already exists; the writes route through `PSContentAdhocUsersContext` which is a separate raw-JDBC class. |

The transitions-for-state query (used by `PSExitAddPossibleTransitions{,Ex}`) has
no Hibernate equivalent today. The simplest path is a new `IPSWorkflowService#findTransitionsByState(workflowId, stateId)` method backed by a JPQL query through the existing
`PSTransitionHib`/`PSStateHib` graph, but writing that graph traversal correctly is
non-trivial (`PSTransitionHib` is owned by `PSStateHib` via `@OneToMany`).

`PSExitPerformTransition` is the only exit with writes. The `CONTENTADHOCUSERS`
Hibernate entity already exists (`PSContentAdhocUser`) and is used elsewhere; the
writes route through `PSContentAdhocUsersContext` which still needs a Hibernate-backed
rewrite.

### `PSConnectionMgr` deletion

Cannot delete `PSConnectionMgr` while the state-roles + transitions + adhoc-users
helpers still need a JDBC `Connection`. After Tier 2 + Tier 3 land, `PSConnectionMgr`'s
remaining in-product callers will be limited to:

- `system/src/main/java/com/percussion/cms/handlers/PSWorkflowCommandHandler.java:299` (legacy workflow command handler — separate concern, Phase 4+)
- `system/Tools/RxFix/...` — explicitly out of scope per issue #1561's "Non-goals"
- `modules/TableFactory/...` — explicitly out of scope

So `PSConnectionMgr` will still be present after Phase 4. Removing it requires a
follow-up Phase 5 (or the same Phase 4 PR after Tier 3 lands).

## Recommended split

| PR | Scope | Estimated effort |
|---|---|---|
| **Phase 4a (this branch)** | Tier 1: `PSExitDisallowUpdatePublished`, `PSGetCheckoutStatus` | small |
| **Phase 4b** | Tier 2: `PSExitAddEditAuthFlag`, `PSExitAuthenticateUser` + add `IPSWorkflowService#loadStateRoles` (Hibernate-backed) and migrate `PSStateRolesContext` callers | medium |
| **Phase 4c** | Tier 3a: `PSExitNotifyAssignees` + add Hibernate equivalents for `NOTIFICATIONS` + `PSNotificationsContext` | medium |
| **Phase 4d** | Tier 3b: `PSExitAddPossibleTransitions{,Ex}` + `PSExitPerformTransition` + `CONTENTADHOCUSERS` writes + `PSConnectionMgr` deletion | large |

This is the only realistic path: each tier is a self-contained sub-PR with its own build
gate, Erlang review, and review-thread cycle.

## Current state

- Branch: `fix/1561-workflow-orm-phase4` off `origin/development` (`36cabf5c89`)
- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitDisallowUpdatePublished.java`: migrated, compile clean
- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSGetCheckoutStatus.java`: migrated, compile clean
- `mvn-env.bat -N compile -DskipTests -Dmaven.javadoc.skip=true` → BUILD SUCCESS