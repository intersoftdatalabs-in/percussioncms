# Issue #1561 — Workflow JDBC → Hibernate (Phase 0 Inventory)

> **Status:** Phase 0 + Phase 1 + Phase 2 — inventory, H2 column-qualifier fix
> on the remaining four contexts, and ORM migration of `CONTENTSTATUSHISTORY`
> writes. No commit / push / PR per agreed scope. Phase 3 (migrate the
> remaining exit-class reads, the read constructor, the inner
> `putLastPublicRev` request, and the other contexts) and Phase 4 (delete
> `PSConnectionMgr`) are still open and tracked under this issue.
>
> **Last refreshed:** after PR **#1563** (`a1497cc82d`) merged into
> `origin/development`. That PR delivered a partial Phase 1 (H2 column-qualifier
> fix for two of the six workflow contexts, plus connection diagnostics and the
> `BooleanToTFCharConverter` for site flags). This branch finishes Phase 1 on
> the remaining four contexts and implements Phase 2 for `CONTENTSTATUSHISTORY`.

---

## 1. Why this doc exists

Issue #1561 proposes moving **in-product** workflow persistence (`CONTENTSTATUSHISTORY`,
`CONTENTADHOCUSERS`, `NOTIFICATIONS`, `TRANSITIONNOTIFICATIONS`, `STATEROLES`, `ROLES`,
`CONTENTTYPES`) from the legacy `PSConnectionMgr` + hand-built multi-dialect SQL path onto
the same Spring-managed datasource + Hibernate session already used by the rest of the CMS.

This document is the **inventory + call-graph** the issue calls for in its Phase 0 and is
the single source of truth the subsequent phases will refer to. It does not modify any
production code.

---

## 2. Repository context (verified)

| Item                                | Value                                                                     | Verified at                                            |
|-------------------------------------|---------------------------------------------------------------------------|--------------------------------------------------------|
| Branch / version                    | `origin/development`, 8.2.0-SNAPSHOT, JDK 21                              | `git log origin/development -1` → `a1497cc82d fix(h2): enable Demo site create — JDBC/ORM and post-save reload (#1563)` |
| Module analysed                     | `modules/extensions-workflow` (legacy Java/XML-extension module)           | `modules/extensions-workflow/pom.xml`                  |
| Module's Maven dependencies         | `perc-security-utils`, `perc-legacy`, `perc-system`, `utils`, `tablefactory` | `modules/extensions-workflow/pom.xml` lines 12–54    |
| Existing module `AGENTS.md`         | Present (added in this branch) — enforces the migration direction locally | `modules/extensions-workflow/AGENTS.md`                |
| Spring/Hibernate config in module   | **None.** No `applicationContext*.xml`, no `hibernate*.xml` under the module. | `glob 'modules/extensions-workflow/src/**/applicationContext*.xml'` and `...hibernate*.xml` |
| `@Transactional` / `@Repository` / `@Service` usage in module | **None** in the workflow exit/context classes (a few hits in unrelated test infra only). | `grep` for those annotations in `modules/extensions-workflow` |

**Implication:** `extensions-workflow` is a legacy XML-extension module. It must
inherit the Spring tx context that the rest of the request already runs under; it
cannot open a parallel stack.

---

## 3. Existing ORM service this migration **reuses**, not rebuilds

The Hibernate stack for the workflow tables is **already present** in the same
artifact (`perc-system`) that `extensions-workflow` depends on. The migration in
Phase 2 does **not** introduce a new ORM stack; it routes legacy raw-JDBC writers
onto the existing service.

| Layer             | Class                                                                                              | Notes                                                                                  |
|-------------------|----------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| Entity (`CONTENTSTATUSHISTORY`) | `com.percussion.services.system.data.PSContentStatusHistory` | `system/services/src/.../system/data/PSContentStatusHistory.java` — `@Entity @Cache(READ_WRITE, region="PSContentStatusHistory") @Table(name="CONTENTSTATUSHISTORY")`. Jakarta persistence. |
| Entity (`CONTENTADHOCUSERS`)    | `com.percussion.services.workflow.data.PSContentAdhocUser`     | `@Entity @IdClass(PSContentAdhocUserPK.class) @Table(name="CONTENTADHOCUSERS")`         |
| Entity (`TRANSITIONNOTIFICATIONS`) | `com.percussion.services.workflow.data.PSNotification`       | `@Entity @IdClass(PSNotificationPK.class) @Table(name="TRANSITIONNOTIFICATIONS")`       |
| Read/Write façade | `com.percussion.services.system.IPSSystemService`             | `saveContentStatusHistory(PSContentStatusHistory)` (interface line 249).                |
| Service locator   | `com.percussion.services.system.PSSystemServiceLocator`        | `getSystemService()` resolves bean `sys_systemService` from the global ctx.             |
| Implementation    | `com.percussion.services.system.impl.PSSystemService`          | `saveContentStatusHistory(...)` at file line 439.                                       |
| Proven in-flight user (today) | `com.percussion.services.legacy.impl.PSCmsObjectMgr`     | Already calls `PSSystemServiceLocator.getSystemService().saveContentStatusHistory(...)` in `convertToInt` (line ~1872) and `updateWorkflowAndState` (line ~2147). |

**Conclusion for Phase 2**: the target API is **`PSSystemServiceLocator.getSystemService().saveContentStatusHistory(new PSContentStatusHistory(...))`**, not a new entity or new service.

PR **#1563** also shipped new helpers that subsequent workflow-ORM work should reuse
instead of inventing local ones:

- `com.percussion.utils.jdbc.PSJdbcConnectionDiagnostics`
  (`modules/utils/src/main/java/.../PSJdbcConnectionDiagnostics.java`,
  test in `PSJdbcConnectionDiagnosticsTest.java`). This is the in-tree
  `NON_KEYWORDS` / `VALUE` JDBC probe. Any future workflow-debug logging that
  needs to confirm "which datasource / schema is this connection actually on?"
  should use it rather than building a one-off probe.
- `com.percussion.services.sitemgr.data.BooleanToTFCharConverter`
  (`system/services/src/.../sitemgr/data/BooleanToTFCharConverter.java`) and the
  matching `@Convert` annotation on `PSSite`. Workflow tables also use
  `CHAR(1)` `'Y'`/`'N'` style flags (e.g. `CONTENTSTATUSHISTORY.VALID`).
  When the ORM migration reintroduces a Hibernate mapping for those flags, the
  same converter pattern is the right model — do not invent another.

---

## 4. Raw-JDBC call sites in `modules/extensions-workflow` (current state, post-#1563)

After this branch's Phase 1 PR pass the **total count of `PSConnectionMgr` usages is 16**
(started at 17, PR #1563 had already stripped two `TABLE_X.COLUMN` patterns from
`PSContentStatusHistoryContext` / `PSContentAdhocUsersContext`, and this branch
strips the remaining four). The split is now:

- 6 `PSConnectionMgr.getQualifiedIdentifier(...)` (table-name only — these are
  still allowed by the new module AGENTS.md).
- 8 `new PSConnectionMgr()` constructors opening their own JDBC connection
  (forbidden in new in-product code by the new module AGENTS.md).
- 2 `PSConnectionMgr.getDebugConnection(...)` / `releaseDebugConnection(...)`
  in the test-only helper `PSAbstractWorkflowTest.java`. **Not in scope** — test
  infra, not product code.

The six contexts that previously concatenated `TABLE_X.COLUMN` (the actual H2
breakage) are now H2-safe: PR #1563 rewrote `PSContentStatusHistoryContext` /
`PSContentAdhocUsersContext` and this branch rewrote the remaining four
(`PSContentTypesContext`, `PSNotificationsContext`, `PSStateRolesContext`,
`PSTransitionNotificationsContext`) with bare column names and aliases. See §4.2.

### 4.1 Exit classes that **open their own connection** (`new PSConnectionMgr()`)

| File                                                                                      | Line(s)  | Runtime trigger (where this exit is invoked)                                            | Reads / writes                          | Hot path? |
|-------------------------------------------------------------------------------------------|----------|------------------------------------------------------------------------------------------|----------------------------------------|-----------|
| `PSExitUpdateHistory.java`                                                                | 237      | XML app `sys_wfUpdateHistory` (registered in `Extensions.xml`); runs after content actions | `CONTENTSTATUSHISTORY` (INSERT)         | **Yes** — runs on every check-in / check-out / transition. Same path the issue cites as the dual-connection break for site-create. |
| `PSExitPerformTransition.java`                                                            | 404      | Content editor transition (perform step)                                                 | Workflow transition rows                | **Yes** — every transition. |
| `PSExitNotifyAssignees.java`                                                              | 252      | After a successful transition                                                            | Notification dispatch                   | Hot during publish/triage flows. |
| `PSExitAddPossibleTransitionsEx.java`                                                     | 261, 339 | List/run the "allowed" transitions for an item                                            | Transition + adhoc users                | Hot.                                                            |
| `PSExitAddPossibleTransitions.java`                                                       | 139      | Legacy variant of the above                                                                | Transition + adhoc users                | Hot.                                                            |
| `PSExitAddEditAuthFlag.java`                                                              | 130      | Pre-check permission flag                                                                  | State roles                             | Hot (every edit attempt).                                       |
| `PSExitAuthenticateUser.java`                                                             | 234      | User authentication during login                                                          | State roles / roles                     | Hot (login).                                                    |
| `PSExitDisallowUpdatePublished.java`                                                      | 112      | Guards updates to already-published items                                                | (read-only)                             | Hot.                                                            |
| `PSGetCheckoutStatus.java`                                                                | 90       | Read-only checkout status query                                                           | (read-only)                             | Read, called on many UI pages.                                 |

### 4.2 Context classes that **hardcode the qualified table name** (`PSConnectionMgr.getQualifiedIdentifier(...)`)

**H2 safety status** — whether each context's SQL strings still concatenate
`<TABLE>.<COLUMN>` (the pattern H2 rejects as `Column "PUBLIC" not found`).

| File                                                                                       | Line of `getQualifiedIdentifier`  | H2 column-qualifier status (post-#1563)                                                                                                  |
|--------------------------------------------------------------------------------------------|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `PSContentStatusHistoryContext.java`                                                       | 692 (`TABLE_CSHC`)                 | ✅ **Fixed by #1563.** `QRYSTRING` and `INSERTSTRING` now use bare column names. Comment on line 686–691 documents the rule.              |
| `PSContentAdhocUsersContext.java`                                                          | 572 (`TABLE_CAU`)                  | ✅ **Fixed by #1563.** `QRYSTRING`, `INSERTSTRING`, `DELETESTRING` now use bare columns. Comment on line 568–572 documents the rule.       |
| `PSContentTypesContext.java`                                                               | 117 (`TABLE_CTC`)                  | ✅ **Fixed.** `QRYSTRING` (now lines 124–129) uses bare column names; the qualified table name is preserved as the `FROM` target only. Comment on lines 116–120 documents the rule. |
| `PSNotificationsContext.java`                                                              | 130 (`TABLE_NC`)                   | ✅ **Fixed.** `QRYSTRING` (now line 137–138) uses bare column names. Comment on lines 129–133 documents the rule.                            |
| `PSStateRolesContext.java`                                                                 | 361 (`SR`), 362 (`R`)              | ✅ **Fixed.** `QRYSTRING` (now lines 369–386) was a two-table join; rewritten with `sr` / `r` aliases so unqualified columns remain unambiguous across `STATEROLES` and `ROLES`. Comment on lines 360–366 documents the rule. |
| `PSTransitionNotificationsContext.java`                                                    | 223 (`TABLE_TNC`)                  | ✅ **Fixed.** `QRYSTRING` (now lines 230–234) uses bare column names. Comment on lines 221–225 documents the rule.                          |

> The class-level `@Deprecated` markers on `PSStateRolesContext` and `PSContentAdhocUsersContext`
> predate this issue and confirm the team direction.

**Implication for Phase 1 (now complete on this branch):** the column-qualifier
fix has been applied to all six contexts. Any workflow flow that touches
`NOTIFICATIONS`, `STATEROLES`, `ROLES`, `TRANSITIONNOTIFICATIONS`, or
`CONTENTTYPES` is now H2-safe at the SQL level. The remaining risk for H2 in
this module is **not** column qualification — it's the dual-connection /
`new PSConnectionMgr()` exits listed in §4.1.

---

## 5. Call-graph: exit → context → table (write paths)

This is the **`processResultDocument` / `performTransition` chain** that the
migration has rewired. Traced from `processResultDocument(Object[], IPSRequestContext, Document)`
entry points into the wrapper contexts that own the actual SQL.

### 5.1 After this branch's Phase 1 + Phase 2 changes

```
sys_wfUpdateHistory (XML app)
  └─ PSExitUpdateHistory#processResultDocument
     └─ new PSConnectionMgr()                                  ── line 249 (kept for reads only; see PHASE 2 NOTE in source)
        └─ updateHistory(...)
           ├─ new PSContentStatusContext(connection, id)       ── reads CONTENTSTATUS  (Phase 3 candidate)
           ├─ new PSTransitionsContext(...)                    ── reads TRANSITIONS    (Phase 3 candidate)
           ├─ (REMOVED: PSExitNextNumber.getNextNumber("CONTENTSTATUSHISTORY") — Hibernate now allocates)
           ├─ new PSContentStatusHistory(entity)              ── writes CONTENTSTATUSHISTORY
           │    └─ PSSystemServiceLocator.getSystemService()
           │         .saveContentStatusHistory(entity)         ── Hibernate-managed, single datasource
           └─ (REMOVED: new PSContentStatusHistoryContext(...)) 
              then optionally calls
                 updateLastPublicRevision(entity, sc, request, csc)
                 └─ PSInternalRequest("sys_ceSupport/putLastPublicRev")            ── internal request still uses legacy pool; Phase 3 candidate
```

```
PSExitPerformTransition#performTransition (entry)
  └─ new PSConnectionMgr()                                     ── line 404 (Phase 3 candidate)
     ├─ reads state roles, transitions, adhoc users
     ├─ transitions state on CONTENTSTATUS
     └─ new PSContentAdhocUsersContext(...)                    ── uses CONTENTADHOCUSERS via PSConnectionMgr.getQualifiedIdentifier line 572 (H2-safe)
```

```
PSExitNotifyAssignees#processResultDocument
  └─ new PSConnectionMgr()                                     ── line 252 (Phase 3 candidate)
     ├─ PSTransitionsContext / PSNotificationsContext         ── uses TRANSITIONNOTIFICATIONS (H2-safe) / NOTIFICATIONS (H2-safe)
     └─ mails assignees
```

### 5.2 Phase 2 deliverable

The CONTENTSTATUSHISTORY write that used to ride on a second pool connection
now goes through `PSSystemServiceLocator.getSystemService().saveContentStatusHistory(...)`
(`PSExitUpdateHistory.java` lines 398–432). That call:

1. Constructs a `com.percussion.services.system.data.PSContentStatusHistory` entity
   directly from the inputs that the legacy `PSContentStatusHistoryContext` write
   constructor used to assemble.
2. Calls Hibernate's `Session.persist(...)` (via the `@Transactional` service),
   which joins the surrounding Spring transaction if one is active on the request.
3. Allocates a new `CONTENTSTATUSHISTORYID` via the global GUID manager when the
   entity's `id` is `< 0`, replacing the previous `PSExitNextNumber.getNextNumber(...)`
   pre-allocation step.
4. Returns synchronously with the assigned id, which `PSExitUpdateHistory` then
   pushes back into the workflow context via `wfContext.setHistoryid(...)`.

The legacy `PSContentStatusHistoryContext` write constructor is preserved for
binary compatibility (it is `@Deprecated` and the `connection` argument is now
ignored). Its body routes through the same `PSSystemServiceLocator.getSystemService()`
call so that any caller — most notably the legacy `PSContentStatusHistoryContextTest`
harness — that still invokes it ends up on the shared pool. The dead `INSERTSTRING`
and `prepareInsertStatement()` were removed.

---

## 6. Out of scope for THIS branch (do not change here)

These are flagged for Phase 3+ but **must not** be touched in Phase 0/1/2 to avoid
scope creep and unreviewed behavioural change:

- `PSExitAddEditAuthFlag`, `PSExitAddPossibleTransitions{,Ex}`, `PSExitAuthenticateUser`,
  `PSExitDisallowUpdatePublished`, `PSExitNotifyAssignees`, `PSExitPerformTransition`,
  `PSGetCheckoutStatus` — separate PRs.
- `PSContentTypesContext`, `PSNotificationsContext`, `PSStateRolesContext`,
  `PSTransitionNotificationsContext` — Phase 1 column-qualifier fix already
  landed on this branch; do not change further in Phase 2.
- `PSContentAdhocUsersContext` — Phase 1 column-qualifier fix landed in PR #1563;
  Phase 3 will migrate its writes through Hibernate.
- `PSContentStatusHistoryContext` — Phase 1 column-qualifier fix landed in PR
  #1563; Phase 2 (this branch) rewrote the write constructor to route through
  `PSSystemServiceLocator.getSystemService().saveContentStatusHistory(...)` and
  removed the dead `INSERTSTRING` + `prepareInsertStatement()`. The read
  constructor still uses raw JDBC (H2-safe) — Phase 3 candidate.
- `system/src/main/java/com/percussion/workflow/PSConnectionMgr.java` — the pool
  façade. Removal is a Phase 4 cleanup after all callers are migrated.
- `system/src/main/java/com/percussion/cms/handlers/PSWorkflowCommandHandler.java:299`
  — yet another `new PSConnectionMgr()` caller (legacy command handler).
- All in-tool / installer paths under `system/Tools/RxFix/...` that use
  `{schema}.CONTENTSTATUSHISTORY` — different audit/fix tools, separate concern.
- `modules/TableFactory/...` — explicitly out of scope per the issue's "Non-goals".

---

## 7. Acceptance items this branch satisfies (from issue #1561)

Phase 0 callouts from the issue:

- [x] Catalog of all `PSConnectionMgr.getQualifiedIdentifier` / static SQL in
  `modules/extensions-workflow` — **§4.2** (and §4.1 for the connection sites).
- [x] Catalog of all `new PSConnectionMgr()` callers in the same module —
  **§4.1**.
- [x] Map each call site to a transaction type and whether it runs inside an
  existing Spring transaction — **§4 (Hot path?) + §5**; all sites run *outside*
  any Spring transaction because the module has none.
- [x] Document which tables need entities — only the three rows of §3 with
  "Yes" remaining gaps (ad hoc users + notifications + transition-notifications +
  state roles are already mapped; **only** `CONTENTSTATUSHISTORY` write path is
  currently being reached from raw JDBC in product code, the others are read-mostly
  via the legacy contexts).
- [x] Record what PR #1563 already delivered (partial Phase 1 + diagnostics +
  `BooleanToTFCharConverter`) so subsequent phases don't re-do it — **§3, §4.2
  H2 status column**.

Acceptance criteria that remain for later phases (not satisfied here):

- [x] ~~Single connection pool / tx model for in-product workflow writes~~ —
  **Phase 2 (this branch).** The `sys_wfUpdateHistory` CONTENTSTATUSHISTORY
  write now goes through
  `PSSystemServiceLocator.getSystemService().saveContentStatusHistory(...)`,
  sharing the same Spring-managed datasource as the surrounding Hibernate
  work. The legacy `PSContentStatusHistoryContext` write constructor was
  rewired to the same path; its `Connection` argument is now ignored.
- [x] ~~H2 column-qualifier fix for the remaining four contexts~~ (`PSContentTypesContext`,
  `PSNotificationsContext`, `PSStateRolesContext`,
  `PSTransitionNotificationsContext`) — **Fixed in this branch's Phase 1 PR
  pass.** All six contexts that use `PSConnectionMgr.getQualifiedIdentifier`
  are now H2-safe; only `getQualifiedIdentifier` for *table-name assembly*
  remains, as the new module AGENTS.md requires.
- [x] ~~No hand-built multi-dialect SQL for `CONTENTSTATUSHISTORY` writes~~ —
  **Phase 2 (this branch).** `PSContentStatusHistoryContext.INSERTSTRING` was
  deleted. The only remaining raw SQL for that table is the read constructor's
  `QRYSTRING` (H2-safe) and the `tablefactory` schema/installer tooling, which
  is out of scope.
- [ ] **Site-create / NavTree regression test on H2** — Phase 2 acceptance
  criterion. PR #1563 already provides a working `POST /services/sitemanage/site/`
  smoke test on `/opt/Percussion` H2. **Gap:** this module has no Spring-aware
  JUnit 5 + H2 test infrastructure yet, so the regression coverage is manual
  smoke. Recommended follow-up: add a JUnit 5 + Spring + H2 test in
  `system/src/test/java/com/percussion/services/workflow/` that exercises
  `PSSystemService#saveContentStatusHistory` end-to-end (entity-build → persist
  → read-back via `findContentStatusHistory`) and run it in CI. Tracking
  GitHub issue TBD.
- [ ] **Cross-DB smoke (H2 + one server DB)** — Phase 2 acceptance criterion.
  Same infrastructure gap as the site-create test.
- [ ] Removal of `PSConnectionMgr` from in-product paths — Phase 4 (and from
  reads inside `PSExitUpdateHistory` itself — Phase 3).

---

## 8. Open questions for follow-up PRs (recorded, not blocking Phase 0)

1. **`PSExitUpdateHistory#updateLastPublicRevision` (now ~line 459).** Issues an
   internal request `sys_ceSupport/putLastPublicRev`. That internal request may
   itself open a second pool connection. Phase 3 must verify the inner call
   does not re-introduce the dual-connection break.
2. **`PSContentStatusHistoryContext` read constructor.** Still uses raw JDBC.
   Its only in-tree caller is the legacy `PSContentStatusHistoryContextTest`
   `main(String[])` harness; no production code uses it. Phase 3 candidate:
   reimplement as an in-memory cursor backed by
   `PSSystemServiceLocator.getSystemService().findContentStatusHistory(...)`.
3. **Tx propagation.** `saveContentStatusHistory` is `@Transactional`. If the
   surrounding request runs under `@Transactional`, the history write joins it
   (good). If the request is not transactional (e.g. cleanup steps), the service
   still opens its own short tx — verify with the H2 integration test that this
   does not mark the outer scope rollback-only (the regression PR #1563 fixed
   was exactly this pattern). **Must be verified with an integration test
   before Phase 3 deletes the legacy write path entirely.**
4. **Hibernate 6 column-qualifier behaviour.** Verify with a JUnit on H2 that
   `PSContentStatusHistory` insert still produces a unique `CONTENTSTATUSHISTORYID`
   after the legacy `PSExitNextNumber` pre-allocation is removed. (The
   `m_guidMgr.createGuid(PSTypeEnum.ITEM_HISTORY)` path is already used by other
   in-flight callers per `PSCmsObjectMgr`, so this is low risk — but a test makes
   it explicit.)
5. **Backwards-compat for entity surfaces.** Per `system/AGENTS.md` "Backward
   Compatibility" rule, the deprecated `PSContentStatusHistoryContext` write
   constructor must keep its public signature. **Done in this branch:** the
   12-arg constructor still compiles, accepts `Connection` for binary compat,
   and forwards through `PSSystemServiceLocator`.
6. **`PSContentTypesContext` callers.** Confirmed: it is invoked from
   `PSExitUpdateHistory` only through the `PSContentStatusContext` chain, which
   itself is still on raw JDBC. Phase 3 candidate — at that point
   `PSContentTypesContext` collapses entirely.
7. **`PSStateRolesContext` join complexity.** **Fixed in this branch's Phase 1
   pass** — alias-based SQL (`SELECT sr.ROLEID, ..., r.ROLENAME FROM ... sr, ...
   r WHERE ...`) is the new form. The `sr` / `r` aliases make unqualified columns
   unambiguous.
8. **Module-level Spring test infrastructure.** Both the Site-create regression
   test and the Cross-DB smoke test require a Spring test context + H2 that
   this module does not currently have. Recommended home:
   `system/src/test/java/com/percussion/services/workflow/` (already has Spring
   test context per `PSWorkflowService` tests). Tracking GitHub issue TBD —
   should be opened as part of the Phase 2 acceptance follow-up.
9. **`PSExitUpdateHistory` still calls `new PSConnectionMgr()`.** That
   `Connection` is now used only for the `PSContentStatusContext` /
   `PSTransitionsContext` reads. Phase 3 must close the remaining dual-connection
   gap by routing those reads through Hibernate too (the `PSComponentSummary`
   / `PSTransitionHib` entities are already in `system/services`).

---

## 9. Cross-references

- Issue: <https://github.com/intersoftdatalabs-in/percussioncms/issues/1561>
- Partial Phase 1 already merged: PR **#1563** (`a1497cc82d`) — "fix(h2): enable
  Demo site create — JDBC/ORM and post-save reload".
- Branch: `fix/1561-workflow-orm-phase0` (off `origin/development`).
- New module rules: `modules/extensions-workflow/AGENTS.md` (added in this branch).
- JDBC diagnostics helper that subsequent phases should reuse:
  `modules/utils/src/main/java/com/percussion/utils/jdbc/PSJdbcConnectionDiagnostics.java`
  (test: `PSJdbcConnectionDiagnosticsTest.java`).
- `BooleanToTFCharConverter` for `CHAR(1)` `'Y'`/`'N'` flags:
  `system/services/src/com/percussion/services/sitemgr/data/BooleanToTFCharConverter.java`.
- Phase 2 Hibernate target service (already used by `PSCmsObjectMgr`):
  `system/services/src/com/percussion/services/system/IPSSystemService.java`
  → `saveContentStatusHistory(PSContentStatusHistory)` (interface line 249,
  impl `PSSystemService.java:439`).
- Future work items: tracked on the issue, not in this branch.