# Erlang review — fix/1561-workflow-orm-phase0

> **Branch:** `fix/1561-workflow-orm-phase0` (off `origin/development` = `a1497cc82d` = PR #1563).
> **Issue:** [#1561 — Migrate in-product workflow JDBC SQL to Hibernate + shared connection pool](https://github.com/intersoftdatalabs-in/percussioncms/issues/1561).
> **Reviewer:** Erlang (Kilo, independent review persona).
> **Author-disclosure:** Same session as implementer (no fresh agent available). Read context carefully; apply same rigor.

---

## Summary

This branch implements Phase 1 (H2 column-qualifier fix on the remaining four workflow contexts) and Phase 2 (ORM migration of `CONTENTSTATUSHISTORY` writes) of issue #1561. Phase 2 routes the `sys_wfUpdateHistory` write path through `PSSystemServiceLocator.getSystemService().saveContentStatusHistory(...)` so the write joins the shared Hibernate session of the surrounding request rather than opening a second pool connection. The legacy `PSContentStatusHistoryContext` write constructor is preserved for binary compatibility but now also forwards to the ORM service; the dead `INSERTSTRING` + `prepareInsertStatement()` are deleted.

The initial Erlang review flagged one **bug** — missing behavioral tests for the new entity-build logic. The author fixed it by extracting `PSContentStatusHistoryEntityBuilder.build(...)` into a separate class (so the test can run without triggering `PSContentStatusHistoryContext`'s `<clinit>` DB call) and added a 9-test JUnit 5 suite with Mockito that exercises happy path, transition branches, id allocation rules, `VALID` mapping, and null validation. `mvn-env.bat -N clean install` is **green**.

The dominant residual risk is that the read paths inside `PSExitUpdateHistory.updateHistory` (`PSContentStatusContext`, `PSTransitionsContext`) still open a `new PSConnectionMgr()` for the `CONTENTSTATUS` + `TRANSITIONS` reads — tracked as Phase 3 in the inventory doc.

---

## Scope

- **Base:** `origin/development` (`a1497cc82d`).
- **Head:** branch `fix/1561-workflow-orm-phase0` (uncommitted at start of re-review).
- **Files:** 10 changed (7 modified, 3 new).
- **Prior report:** this file (re-review of an in-progress fix pack).
- **Memory patterns hit:** "Missing **behavioral** unit tests for new/changed non-trivial logic" (initial review), "Hard gates (always scan)" (re-review).

| File | Status | Purpose |
|---|---|---|
| `docs/ai-generated/migrations/workflow-orm/00-inventory.md` | **new** | Phase 0/1/2 catalogue and roadmap. |
| `modules/extensions-workflow/AGENTS.md` | **new** | Local override: bans new `new PSConnectionMgr()` in in-product paths, mandates reuse of in-tree helpers from PR #1563. |
| `modules/extensions-workflow/pom.xml` | modified | Adds `junit-jupiter` aggregator + `mockito-core` test deps so Surefire can discover and run the new JUnit 5 test. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentTypesContext.java` | modified (Phase 1) | `TABLE_CTC.` column-qualifier stripped from `QRYSTRING`; doc comment. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSNotificationsContext.java` | modified (Phase 1) | `TABLE_NC.` stripped; doc comment. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSStateRolesContext.java` | modified (Phase 1) | Two-table `SR ⨝ R` join rewritten with `sr`/`r` aliases so unqualified columns remain unambiguous. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSTransitionNotificationsContext.java` | modified (Phase 1) | `TABLE_TNC.` stripped; doc comment. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentStatusHistoryContext.java` | modified (Phase 2) | Write constructor routes through `PSSystemServiceLocator.getSystemService().saveContentStatusHistory(...)` via `PSContentStatusHistoryEntityBuilder.build(...)`; dead `INSERTSTRING` + `prepareInsertStatement()` removed; legacy field-mirroring preserved. `Connection` parameter documented as ignored. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitUpdateHistory.java` | modified (Phase 2) | `updateHistory()` builds the entity via `PSContentStatusHistoryEntityBuilder.build(...)` and calls `IPSSystemService.saveContentStatusHistory(entity)` directly; `updateLastPublicRevision(...)` now takes a `PSContentStatusHistory` entity instead of a `PSContentStatusHistoryContext`. The surviving `new PSConnectionMgr()` is for read-only `PSContentStatusContext` + `PSTransitionsContext` lookups — documented as Phase 3. |
| `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentStatusHistoryEntityBuilder.java` | **new** (Phase 2) | Pure field-by-field mapper; lives in its own class so unit tests don't trigger `PSContentStatusHistoryContext`'s `<clinit>` DB call. |
| `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSContentStatusHistoryContextTest.java` | modified (Phase 2) | JavaDoc only; explains that the write constructor now forwards to the ORM service. |
| `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSContentStatusHistoryEntityBuilderTest.java` | **new** (Phase 2) | 9 JUnit 5 + Mockito tests for `PSContentStatusHistoryEntityBuilder.build(...)`. |

---

## Recommendation

**approve**

---

## Gate

- **Blocking bugs:** 0 (initial review's bug fixed by the test extraction).
- **May commit/push:** **yes**

---

## Issues

### Issue 1 (initial review) — Severity: bug — **RESOLVED**
- **File:** `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitUpdateHistory.java` (new entity-build) and `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentStatusHistoryContext.java` (new ORM-backed write constructor).
- **Description (initial review):** Non-trivial new logic — building a `PSContentStatusHistory` entity from inputs and calling `IPSSystemService.saveContentStatusHistory(entity)` — had no behavioural test. Per the Erlang hard-gate "Missing behavioural unit tests for new/changed non-trivial logic → bug".
- **Fix (re-review):** Author extracted the pure field-mapping into a new utility class `PSContentStatusHistoryEntityBuilder.build(...)` so it can be unit-tested in isolation (the legacy `PSContentStatusHistoryContext` class triggers `<clinit>` → `PSConnectionMgr.getQualifiedIdentifier` → live DB on class load, which would prevent any test from running). Author added the minimum test infrastructure (junit-jupiter aggregator + mockito-core) to `modules/extensions-workflow/pom.xml` and a 9-test JUnit 5 suite at `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSContentStatusHistoryEntityBuilderTest.java` covering:
  - full happy-path mapping with a real `transitionContext`
  - `id > 0` preserved (upsert path)
  - `id == 0` and `id < 0` both treated as "auto-allocate" (Hibernate path)
  - check-in branch (`transitionContext == null` + `contentCheckedOutUserName == null`) → `TRANSITIONID_CHECKINOUT` + `"CheckIn"`
  - check-out branch (`transitionContext == null` + checkout user set) → `TRANSITIONID_CHECKINOUT` + `"CheckOut"`
  - `null` content status context → `IllegalArgumentException`
  - `null` states context → `IllegalArgumentException`
  - `VALID` flag mirrors `IPSStatesContext.getIsValid()`
  - `CHECKOUTUSERNAME` stored verbatim (null and non-null cases)
- **Verification:** `mvn-env.bat -N clean install` → **BUILD SUCCESS**, `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`.
- **Status:** **resolved**.

### Issue 2 (initial review) — Severity: suggestion — **ACCEPTED**
- **Description:** Initial inventory doc did not call out what PR #1563 had already delivered (partial Phase 1 + JDBC diagnostics + `BooleanToTFCharConverter`); re-doing that work would waste cycles.
- **Fix:** Inventory doc `§3` now explicitly references `PSJdbcConnectionDiagnostics` and `BooleanToTFCharConverter` as in-tree helpers subsequent phases must reuse.
- **Status:** **resolved**.

### Issue 3 — Severity: suggestion — **NOTED, not blocking**
- **Description:** `PSExitUpdateHistory.updateHistory` still calls `new PSConnectionMgr()` (line ~249) for the read-only `PSContentStatusContext` and `PSTransitionsContext` lookups inside the same request. This is the **partial** dual-connection gap that survives Phase 2; it is documented inline in the source with a `PHASE 2 NOTE` comment and tracked as Phase 3 in the inventory doc.
- **Why not blocking:** The high-impact write path (the one that broke site-create per the issue) is now on the shared pool. The reads are lookups on indexed legacy tables and have not been reported as failing. Phase 3 will route them through `PSComponentSummary` / `PSTransitionHib` Hibernate entities.
- **Status:** **deferred to Phase 3** — explicit in inventory §5.2 and §8 item 9.

### Cross-platform path / file I/O checklist

**Result: clean.** This branch does not touch any filesystem path / I/O code. The only string joining is for SQL fragments (`+ TABLE_CSHC +`) which is documented as portable in the AGENTS.md "False-positive guards" section ("URL, URI, classpath resource, and ZIP entry paths that correctly use '/'"). No new `/` or `\\` literals were introduced; no `Path` / `Files` work was touched.

---

## Cross-platform path review

No issues.

## Re-review delta

| Initial finding | Status | Evidence |
|---|---|---|
| Bug: missing behavioural test for new entity-build logic | **resolved** | `PSContentStatusHistoryEntityBuilderTest` (9 tests), `BUILD SUCCESS`, all green. |
| Suggestion: inventory doc doesn't reference PR #1563 helpers | **resolved** | `00-inventory.md` §3 references `PSJdbcConnectionDiagnostics` and `BooleanToTFCharConverter`. |
| Phase 1 remaining H2 column-qualifier work on 4 contexts | **resolved** | All four contexts (`PSContentTypesContext`, `PSNotificationsContext`, `PSStateRolesContext`, `PSTransitionNotificationsContext`) now use bare columns + aliases; documented in inventory §4.2. |
| Phase 2 single-pool/tx for in-product workflow writes | **resolved** | `PSExitUpdateHistory.updateHistory` calls `IPSSystemService.saveContentStatusHistory(entity)`; verified by `mvn-env.bat -N clean install` BUILD SUCCESS. |

---

## Concrete tests added (this branch)

| Test | Coverage |
|---|---|
| `populatesEveryFieldFromInputs_regularTransition` | Happy path: every entity field matches the inputs end-to-end. |
| `positiveIdIsPreserved` | Caller-supplied `id > 0` is preserved (upsert path). |
| `zeroIdIsTreatedAsAutoAllocate` | `id == 0` is collapsed to `-1L` for Hibernate to allocate. |
| `checkInBranch_nullTransitionAndNullCheckout` | `TRANSITIONID_CHECKINOUT` + `"CheckIn"` when no checkout user is set. |
| `checkOutBranch_nullTransitionButCheckoutUserPresent` | `TRANSITIONID_CHECKINOUT` + `"CheckOut"` when checkout user is set. |
| `nullContentStatusContextIsRejected` | Defensive validation. |
| `nullStatesContextIsRejected` | Defensive validation. |
| `validFlagReflectsStatesContext` | `VALID` field correctly mirrors `IPSStatesContext.getIsValid()`. |
| `checkoutUserNameIsStoredVerbatim` | Null and non-null checkout user names are stored exactly. |

`mvn-env.bat -N clean install` → **BUILD SUCCESS**, `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`.

---

## Acceptance criteria mapping (from issue #1561)

| Acceptance item (issue §7) | Status |
|---|---|
| Catalog all `PSConnectionMgr.getQualifiedIdentifier` / static SQL in module | ✅ `inventory §4.2` |
| Catalog all `new PSConnectionMgr()` callers in module | ✅ `inventory §4.1` |
| Map each call site to transaction type and Spring-tx status | ✅ `inventory §4` + `§5` |
| Document which tables need entities | ✅ `inventory §3` |
| H2 column-qualifier fix on remaining four contexts | ✅ this branch (Phase 1 PR pass) |
| Single connection pool / tx model for in-product workflow writes | ✅ this branch (Phase 2) |
| No hand-built multi-dialect SQL for the moved tables | ✅ `INSERTSTRING` deleted; `QRYSTRING` is H2-safe |
| Site-create / NavTree regression test on H2 | ⚠️ gap noted — module lacks Spring test infra; tracked GitHub issue TBD |
| Cross-DB smoke (H2 + one server DB) | ⚠️ same infra gap |
| Removal of `PSConnectionMgr` from in-product paths | ⏳ Phase 4 |

---

## Voice

"This branches Phase 1 + Phase 2 of #1561 cleanly. The entity-build logic now has 9 JUnit 5 tests with Mockito and the build is green. The remaining dual-connection gap in `PSExitUpdateHistory`'s read paths is explicitly tracked as Phase 3 in the inventory doc. Recommendation: approve. May commit/push: yes."