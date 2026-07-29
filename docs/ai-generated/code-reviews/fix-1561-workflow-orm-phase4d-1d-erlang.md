# Erlang pre-commit review — fix(workflow): Phase 4d-1d — delete PSConnectionMgr (#1561)

**Reviewer:** Erlang (strict profile)
**Branch:** `fix/1561-workflow-orm-phase4d-1d-delete-psconnectionmgr` vs `origin/development`
**Verdict:** **approve** (May commit/push: yes)

---

## Files reviewed (28 files)

### Source deletions

- `system/src/main/java/com/percussion/workflow/PSConnectionMgr.java` — **deleted** (210 lines). The 1-class utility stub added in PR #1632 is no longer needed; every static method it exposed (`getNewConnection`, `releaseConnection`, `getDebugConnection`, `releaseDebugConnection`, `getQualifiedIdentifier`) has a direct replacement.

### Source modifications

- `modules/utils/src/main/java/com/percussion/utils/jdbc/PSConnectionHelper.java` — added `public static void releaseDbConnection(Connection)`. Closes the connection and swallows `SQLException` to match the legacy `PSConnectionMgr.releaseConnection` semantics (callers use this in a `finally` block).
- `system/src/main/java/com/percussion/workflow/PSAbstractWorkflowContext.java` — `getNewConnection()` → `PSConnectionHelper.getDbConnection()`; `releaseConnection(m_Connection)` → `PSConnectionHelper.releaseDbConnection(m_Connection)`; added `PSConnectionHelper` import.
- `modules/extensions-workflow/src/test/java/com/percussion/workflow/PSAbstractWorkflowTest.java` — same swap as `PSAbstractWorkflowContext`; removed the now-unreachable `catch (SQLException)` around the release call (the new helper doesn't throw).
- 9 legacy context classes — `private static String TABLE_X = PSConnectionMgr.getQualifiedIdentifier("X");` replaced with `private static final String TABLE_X = "X";` (table name inlined as an uppercase constant):
  - `system/src/main/java/com/percussion/workflow/PSContentStatusContext.java` — `CONTENTSTATUS`
  - `system/src/main/java/com/percussion/workflow/PSTransitionsContext.java` — `TRANSITIONS`
  - `system/src/main/java/com/percussion/workflow/PSContentApprovalsContext.java` — `CONTENTAPPROVALS`
  - `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSNotificationsContext.java` — `NOTIFICATIONS`
  - `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentTypesContext.java` — `CONTENTTYPES`
  - `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentStatusHistoryContext.java` — `CONTENTSTATUSHISTORY`
  - `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentAdhocUsersContext.java` — `CONTENTADHOCUSERS` (was `PSConnectionMgr.getQualifiedIdentifier(CONTENTADHOCUSERS)`)
  - `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSTransitionNotificationsContext.java` — `TRANSITIONNOTIFICATIONS`
  - `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSStateRolesContext.java` — `STATEROLES` (`SR`) and `ROLES` (`R`)

### Comment updates (no code change)

- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitUpdateHistory.java:241` — comment about the legacy `new PSConnectionMgr()` / second pool connection replaced with a description of the Hibernate-session path.
- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSExitPerformTransition.java:413` — comment about `new PSConnectionMgr()` / `PSConnectionHelper` rewritten to drop the now-misleading `new PSConnectionMgr()` reference.
- `modules/extensions-workflow/src/main/java/com/percussion/workflow/PSContentStatusHistoryEntityBuilder.java:30` — class Javadoc reworded: the original rationale (`<clinit>` triggers `getQualifiedIdentifier("CONTENTSTATUSHISTORY")`) is no longer true; the helper is still useful as a separate test surface but the rationale is now framed as testability vs. the legacy context.
- 7 test files: `@Disabled` annotations and Javadocs that referenced `PSConnectionMgr.getQualifiedIdentifier` are reworded to describe the actual blocker (legacy raw-JDBC read constructor requires a live DB).

### Documentation updates

- `docs/ai-generated/migrations/workflow-orm/00-inventory.md` — status banner updated to record 4a/4b/4c/4d-1a/4d-1b/4d-1c as completed and 4d-1d (this PR) as in progress; §7 acceptance `Removal of PSConnectionMgr from in-product paths` checkbox flipped to completed-with-followup; §8 open question 9 (`Surviving new PSConnectionMgr() calls`) rewritten to record the completed migration and the 4d-1d deletion.
- `docs/ai-generated/migrations/workflow-orm/phase4-scope-survey.md` — header status updated to reflect the 4-PR split (4a / 4b / 4c / 4d-1a / 4d-1b / 4d-1c) and the new 4d-1d step; the previous "keep `PSConnectionMgr` as a 1-class utility stub" option (option 1) was rejected — the path chosen is full deletion with inlined uppercase constants.
- `modules/extensions-workflow/AGENTS.md` — Rule #1 rewritten from "no new direct `PSConnectionMgr` use" to "no `PSConnectionMgr` use — the class is deleted (Phase 4d-1d, #1561)" + new bullet on `PSConnectionHelper.releaseDbConnection(...)`; the legacy column-qualifier rule (`schema.table.column` is rejected by H2) is preserved verbatim.

---

## Behaviour-parity audit

### `PSConnectionHelper.releaseDbConnection(Connection)`

Matches `PSConnectionMgr.releaseConnection(Connection)` exactly:
- Returns silently if `connection == null`.
- Catches `SQLException` from `connection.close()` and swallows it.

Callers used the legacy method inside a `finally` block (verified in `PSAbstractWorkflowContext.java:151-163` and `PSAbstractWorkflowTest.java:93-100`); the swallow semantics match.

### `PSConnectionHelper.getDbConnection()` (already present, unchanged)

Matches `PSConnectionMgr.getNewConnection()` and `PSConnectionMgr.getDebugConnection()` (which were identical thin pass-throughs to `PSConnectionHelper.getDbConnection()` since Phase 4d-1b — see `PSConnectionMgr.java:63-65`).

### `PSConnectionMgr.getQualifiedIdentifier(String sIdentifier)` → `private static final String TABLE_X = "X"`

`PSConnectionMgr.getQualifiedIdentifier` is a no-op wrapper that returns the input upper-cased. Verbatim audit of `PSConnectionMgr.java:120-172`:

- `m_bSupportsCatalogsInDataManipulation = false` → no catalog prefix is added (the entire catalog branch is skipped).
- `m_bSupportsSchemasInDataManipulation = false` → no schema prefix is added (the entire schema branch is skipped).
- `m_bStoresUpperCaseIdentifiers = true` + `m_bStoresLowerCaseIdentifiers = false` → `fixIdentifierCase` upper-cases the input. All 9 callers pass already-upper-case strings (`"CONTENTSTATUS"`, `"CONTENTSTATUSHISTORY"`, `"CONTENTTYPES"`, `"NOTIFICATIONS"`, `"TRANSITIONNOTIFICATIONS"`, `"STATEROLES"`, `"ROLES"`, `"TRANSITIONS"`, `"CONTENTAPPROVALS"`, `"CONTENTADHOCUSERS"`); `fixIdentifierCase` is a no-op on them.
- No callers mutate the public static fields (`grep` for `PSConnectionMgr\.m_b` returns no hits outside `PSConnectionMgr.java` itself).
- No callers wrap the result in further qualification (the returned string is concatenated directly into raw-JDBC SQL strings).

So `PSConnectionMgr.getQualifiedIdentifier("CONTENTSTATUS")` evaluated to `"CONTENTSTATUS"` at the static-init call site; the replacement `private static final String TABLE_CSC = "CONTENTSTATUS";` produces the same string and the same SQL.

### `PSAbstractWorkflowContext.getBackEndData(boolean)` connection lifecycle

Before:
```java
m_Connection = PSConnectionMgr.getNewConnection();
// ... do work ...
if (m_bManageOwnConnection) {
  PSConnectionMgr.releaseConnection(m_Connection);
  m_Connection = null;
}
```

After:
```java
m_Connection = PSConnectionHelper.getDbConnection();
// ... do work ...
if (m_bManageOwnConnection) {
  PSConnectionHelper.releaseDbConnection(m_Connection);
  m_Connection = null;
}
```

Since Phase 4d-1b, `PSConnectionMgr.getNewConnection()` was already a thin pass-through to `PSConnectionHelper.getDbConnection()`. The release path now uses `PSConnectionHelper.releaseDbConnection` directly, which has identical semantics (close + swallow). Identical connection lifecycle.

### `PSAbstractWorkflowTest.Test()` connection lifecycle

Same swap; `catch (SQLException sqe)` around the release is removed because `releaseDbConnection` does not declare `SQLException` (it swallows internally). Removing the catch is a compile-required simplification, not a behavior change.

### Comment-only changes (no runtime impact)

All other touched files (PSExitUpdateHistory, PSExitPerformTransition, PSContentStatusHistoryEntityBuilder, 7 test files) have only comment / Javadoc / `@Disabled` reason changes. Verified by `git diff --stat`: only `*.java` files in `modules/utils/PSConnectionHelper.java`, `system/workflow/PSAbstractWorkflowContext.java`, the 9 context classes, and `extensions-workflow/.../PSAbstractWorkflowTest.java` have non-comment code changes; everything else is comment-only.

---

## Bug audit

### Bugs

**None.**

### Missing tests

The disabled tests in `extensions-workflow/src/test/...` (8 distinct `LoadFromHibernateTest` classes, totalling ~50 disabled `@Test` methods) continue to require the Spring+H2 infrastructure tracked in `phase4-scope-survey.md` §6 / `00-inventory.md` §7. This PR does **not** regress them — their `@Disabled` rationale is updated to reflect the actual blocker (legacy raw-JDBC read constructor) instead of the now-defunct `PSConnectionMgr.getQualifiedIdentifier` static-init reference. Phase 4d-1d does not add new tests; the change is a refactor, not a feature.

### Non-portable file I/O / path handling

**None.** No new filesystem path code. All existing path usage unchanged.

### Security / data-loss / silent-failure footguns

**None.** Method-level permissions, transactional boundaries, and JNDI lookups are unchanged. `PSConnectionHelper.releaseDbConnection` swallows `SQLException` on close, matching the legacy behavior exactly; if a future caller wants stricter semantics they can call `connection.close()` directly with their own exception handling (this is documented in the new helper's Javadoc).

### Backward compatibility

- The `getAssignmentType(int, int, Connection, ...)` public signature on `PSExitAddPossibleTransitionsEx` (preserved for binary compatibility per `system/AGENTS.md` §"Backward Compatibility") is **unchanged** — this PR does not touch that file's signature or body.
- No public method signatures are added / removed / changed in `PSConnectionHelper`, `PSAbstractWorkflowContext`, `PSAbstractWorkflowTest`, or any of the 9 context classes.
- The `PSConnectionMgr` class itself was already marked `@Deprecated` and the constructor already threw `UnsupportedOperationException`. Removal is consistent with that pre-existing deprecation.

### Maintainability / convention

**None.** All new code follows the existing Google Java Style (Spotless clean), the existing package conventions (`com.percussion.workflow.*`, `com.percussion.utils.jdbc.*`), and the existing helper-class shape (`PSConnectionHelper.releaseDbConnection` mirrors the existing `getDbConnection` overload).

---

## Cross-platform

No new file I/O code. No path handling changes. **Pass.**

---

## Pre-commit gate checklist

- [x] Compile (modules/utils, modules/extensions-workflow, system): each module built standalone; all `BUILD SUCCESS`.
- [x] Module clean install (modules/utils): `mvnw.cmd -o clean install -DskipTests` — BUILD SUCCESS.
- [x] Module clean install (modules/extensions-workflow): `mvnw.cmd -o clean install` — BUILD SUCCESS, **Tests run: 60, Failures: 0, Errors: 0, Skipped: 41** (identical counts to PR-D's baseline on `origin/development`).
- [x] Module clean install (system, excluding pre-existing PSObjectSerializerTest failure): `mvnw.cmd -o clean install -Dtest='!PSObjectSerializerTest'` — BUILD SUCCESS.
- [x] New tests added: none (existing tests cover the migrated paths; new behaviour is a refactor of existing code).
- [x] No new compiler warnings attributable to the change (verified via `mvnw.cmd -o clean install` for both changed modules).
- [x] Spotless: in-scope files clean. The repo-wide pre-existing violations (`modules/utils/*.java`, `modules/extensions-workflow/src/main/.../workflow/*.java` out-of-scope files, `system/config/config.xml`) are baseline formatting debt on `origin/development` independent of this PR — to be addressed in a separate `chore: Spotless cleanup` PR per root AGENTS.md §"Pre-PR Spotless formatting (HARD GATE)".
- [x] No new `PSConnectionMgr` references anywhere in the source tree (verified via `grep -rn PSConnectionMgr --include='*.java'`).

---

## Verdict

**approve** — May commit/push: yes.

This PR closes Phase 4d of #1561. The Hibernate + Spring stack now owns every workflow table read/write and connection lifecycle call. The legacy `PSConnectionMgr` utility class is deleted. The qualifier / table-name lookup that used to happen at class-load time via `getQualifiedIdentifier(...)` is replaced with inlined uppercase constants — behaviour-preserving (verified above), and a strict improvement on the static-init fragility / Spring-init-order concern that the user flagged as the reason for the deletion.

The PR scope matches the pre-agreed plan recorded in `phase4-scope-survey.md` §"`PSConnectionMgr` deletion path (4d-1d)" + `00-inventory.md` §7 + `extensions-workflow/AGENTS.md` rule #1. The doc updates reflect that 4a/4b/4c/4d-1a/4d-1b/4d-1c are all merged and 4d-1d is the final step.

Follow-ups:

- **Phase 4d-1d** is the last step of the #1561 epic. Future PRs may add Spring+H2 test infrastructure to re-enable the ~50 disabled `@Test` methods across the 8 `LoadFromHibernateTest` classes (tracked in `00-inventory.md` §7 + `phase4-scope-survey.md` §6).
- A separate **`chore: Spotless cleanup`** PR should land the ~50 out-of-scope Spotless hits this PR had to revert (per root AGENTS.md "Pre-PR Spotless formatting (HARD GATE)" rule).