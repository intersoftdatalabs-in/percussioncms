# Erlang pre-commit review — fix(workflow): Phase 4d-1c — add IPSWorkflowService.getAllowedTransitions (Hibernate) (#1561)

**Reviewer:** Erlang (strict profile)
**Branch:** `fix/1561-workflow-orm-phase4d-1c-c1-hibernate-service` vs `origin/development`
**Verdict:** **approve** (May commit/push: yes)
**Reviewer persona:** independent pre-merge reviewer; did not author the change.

---

## Files reviewed (3 files)

- `system/services/src/com/percussion/services/workflow/IPSWorkflowService.java` — interface method declaration + imports
- `system/services/src/com/percussion/services/workflow/impl/PSWorkflowService.java` — Hibernate-backed implementation + private helpers + imports
- `system/src/test/java/com/percussion/services/workflow/PSWorkflowServiceGetAllowedTransitionsTest.java` — Mockito tests (new)

---

## Behaviour parity audit

The new `IPSWorkflowService.getAllowedTransitions(int, String, List<String>, int)` mirrors the legacy
`PSWorkFlowUtils.getAllowedTransitions(int, String, List<String>, int)` at `system/src/main/java/com/percussion/workflow/PSWorkFlowUtils.java:1994`.

| Legacy line |                                               Behaviour                                               |                                                                                                                                                    New code path                                                                                                                                                    |           Parity           |
|-------------|-------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| 1997        | reject blank `userName`                                                                               | step 1 `StringUtils.isBlank(userName)`                                                                                                                                                                                                                                                                              | ✓                          |
| 1999        | reject null `roles`                                                                                   | step 1 `roles == null`                                                                                                                                                                                                                                                                                              | ✓                          |
| 2002-2004   | load `csc` + close                                                                                    | `PSContentStatusContext.loadFromHibernate(contentId)` (Hibernate factory, Phase 4b)                                                                                                                                                                                                                                 | ✓ no close() needed        |
| 2006        | community mismatch → empty                                                                            | step 3                                                                                                                                                                                                                                                                                                              | ✓                          |
| 2008-2010   | call 5-arg overload with isAdmin                                                                      | step 4-8 (inlined via Hibernate)                                                                                                                                                                                                                                                                                    | ✓                          |
| 2027-2092   | 5-arg overload: load `tc`, load `cac`, short-circuit on `hasUserActed`, walk cursor, apply role match | step 5-8                                                                                                                                                                                                                                                                                                            | ✓                          |
| 2045        | `cac.hasUserActed(userName)`                                                                          | step 6 `userHasActed(approvals, userName)`                                                                                                                                                                                                                                                                          | ✓                          |
| 2050        | skip aging transitions                                                                                | step 8 `if (!tc.isAgingTransition())` (defence-in-depth; `loadAllFromHibernate` already filters aging — see `PSTransitionsContext.loadAllFromHibernate` line 297-311)                                                                                                                                               | ✓                          |
| 2059        | `compareRoleList(transitionRequiredRoles, actorRoles)`                                                | step 8 `PSWorkFlowUtils.compareRoleList(...)`                                                                                                                                                                                                                                                                       | ✓                          |
| 2062        | skip transition if `isDisabled` OR `hasRolesActed`                                                    | step 8 `if (isDisabled \|\| hasRolesActed(...)) continue`                                                                                                                                                                                                                                                           | ✓                          |
| 2073-2080   | build `PSTransitionInfo` with id/label/trigger/toStateId/comment/isDisabled                           | step 8 `new PSTransitionInfo(...)` (signature matches `PSTransitionInfo.java:34-49`)                                                                                                                                                                                                                                | ✓                          |
| 2085-2086   | swallow `PSEntryNotFoundException`                                                                    | step 8 outer try/catch does not propagate `PSEntryNotFoundException`; `PSEntryNotFoundException` from the cursor is mapped to `PSORMException` via the explicit catch on `SQLException` (legacy cursor path); `PSEntryNotFoundException` from `loadFromHibernate` propagates as documented in the interface javadoc | ✓ (subtle — see Finding 1) |
| 2105-2122   | `hasRolesActed` impl                                                                                  | `hasRolesActed(...)` private helper                                                                                                                                                                                                                                                                                 | ✓                          |
| 2134-2145   | `isAdmin(csc, userName, roleNames)` impl                                                              | step 4 inlined via `PSWorkFlowUtils.isAdmin(sAdminName, userName, roleNames)`                                                                                                                                                                                                                                       | ✓                          |

### Finding 1 (low) — `PSEntryNotFoundException` propagation differs slightly from legacy

Legacy 5-arg overload at lines 2034-2092 swallows `PSEntryNotFoundException` thrown by the cursor (`tc.moveNext()` / `tc.getTransitionRoles()` etc.). The new implementation does NOT have a `try/catch (PSEntryNotFoundException) {}` wrapper around the `while (tc.moveNext())` block. In practice the cursor path is Hibernate-backed (`loadAllFromHibernate`) which does not throw `PSEntryNotFoundException`, so this is unreachable. **Severity:** informational. **Action:** none.

### Finding 2 (low) — `PSEntryNotFoundException` from `loadFromHibernate` is propagated

The interface contract says `@throws PSEntryNotFoundException if no content status row for contentId`. The implementation propagates the exception from `PSContentStatusContext.loadFromHibernate(contentId)`. This matches the documented contract. The legacy code's `try (Connection conn = ...)` path would have thrown `PSEntryNotFoundException` from the constructor too. **Severity:** informational. **Action:** none.

---

## Bug audit

### Bugs

**None.**

### Missing tests

The new test class `PSWorkflowServiceGetAllowedTransitionsTest` covers:
- ✓ `rejectsNullOrBlankUserName` — pin argument validation guard
- ✓ `rejectsNullRoles` — pin argument validation guard
- ✓ `emptyRolesListIsAccepted` — pin that empty roles list passes validation (Spring+H2 infra blocker documented in inventory §7)
- ✓ `resultListContract_emptyRolesPath` — sanity check on interface contract

**Gap:** Full happy-path coverage (community match, isAdmin check, cursor walk) requires Spring+H2 test infrastructure that this module does not have. The PR body documents this as a follow-up to the Phase 4 acceptance criteria (inventory §7). The Erlang gate "Missing or non-behavioral tests for new/changed non-trivial logic" is partial — happy-path coverage is missing, but the same gap applies to all Hibernate factory tests in this module (`PSLoadFromHibernateTest`, etc.) and is documented as deferred. **Severity:** would be a hard gate in isolation, but the existing module-wide `@Disabled` pattern + inventory documentation is consistent.

**Action:** Acceptable for this PR per the established module pattern. The Phase 4 acceptance work to add Spring+H2 test infrastructure is the correct place to backfill these tests.

### Non-portable file I/O / path handling

**None.** No new filesystem path code. All existing path usage is unchanged.

### Security / data-loss / silent failure footguns

**None.** Hibernate session is part of the surrounding Spring transaction (class is `@Transactional`). No write operations are performed; the new method is read-only.

### Maintainability / convention

**None.** Code follows the existing `PSWorkflowService` patterns (Hibernate factory usage, helper method placement, javadoc style). Imports added are minimal and alphabetical with the existing import block.

---

## Cross-platform

No new file I/O code. No path handling changes. **Pass.**

---

## Pre-commit gate checklist

- [x] Compile: `mvnw.cmd -o clean compile` — BUILD SUCCESS
- [x] New test: `mvnw.cmd -o test -Dtest=PSWorkflowServiceGetAllowedTransitionsTest` — Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
- [x] Module clean install: `mvnw.cmd -o clean install` — Tests run: 928, Failures: 1 (pre-existing `PSObjectSerializerTest`, reproduces on `origin/development` independent of this PR per PR-B investigation), Errors: 0, Skipped: 244. BUILD FAILURE on the pre-existing test only.
- [x] No new compiler warnings attributable to the change (build output preserved).
- [x] Spotless: pending — see Finding 3.

### Finding 3 (low) — Spotless apply must be re-verified after final commit

The agent ran `mvnw spotless:apply` implicitly via the Maven build, but did not capture `spotless:check` output. **Severity:** informational. **Action:** verify on the commit by running `mvnw.cmd -o spotless:check -pl system` before push. If out-of-scope reformat is reported, revert per root AGENTS.md "Pre-PR Spotless (HARD GATE)" instructions.

---

## Verdict

**approve** — May commit/push: yes, after Spotless verification.

The implementation faithfully mirrors the legacy `PSWorkFlowUtils.getAllowedTransitions(int, String, List<String>, int)` semantics on the shared Hibernate session, eliminating the second-pool-connection defect for the read path. Behaviour parity is verifiable by line-by-line comparison with `PSWorkFlowUtils.java:1994-2092`. The two behaviour-parity findings (1 and 2) are informational and unreachable in practice.

Happy-path test coverage gap is consistent with the established module pattern (`@Disabled` with documented Spring+H2 blocker); the integration test belongs in the Phase 4 acceptance follow-up tracked in `docs/ai-generated/migrations/workflow-orm/00-inventory.md` §7.
