# Phase 4c — Erlang pre-commit review

**Branch:** `fix/1561-workflow-orm-phase4c` off `origin/development` (`78bc42f7f`)
**Date:** 2026-07-28
**Scope:** PSExitNotifyAssignees + NOTIFICATIONS + TRANSITIONNOTIFICATIONS

## Verdict

**`approve`** — branch may be committed, pushed, and a PR opened.

## Files changed

| Path | Change |
|---|---|
| `system/services/.../workflow/IPSWorkflowService.java` | Add `findTransitionNotifications(workflowId, transitionId)` |
| `system/services/.../workflow/impl/PSWorkflowService.java` | Implement `findTransitionNotifications` with typed `createQuery` + `@Transactional` |
| `system/services/.../system/IPSSystemService.java` | Add `findNotificationDef(workflowId, notificationId)` |
| `system/services/.../system/impl/PSSystemService.java` | Implement with `Session.get(PSNotificationDef.class, ...)` |
| `modules/extensions-workflow/.../PSNotificationsContext.java` | Add `static loadFromHibernate(int, int)` factory + `populateFromHibernate` |
| `modules/extensions-workflow/.../PSTransitionNotificationsContext.java` | Add `static loadFromHibernate(int, int)` factory + `populateFromHibernate` |
| `modules/extensions-workflow/.../PSExitNotifyAssignees.java` | Drop `new PSConnectionMgr()` + Connection plumbing; use Hibernate-backed contexts |
| `modules/extensions-workflow/.../PSSendNotificationsTest.java` | Update `sendNotifications` call signature (Connection parameter removed) |
| `system/src/test/.../PSWorkflowServiceFindTransitionNotificationsTest.java` (new) | 5 Mockito tests |
| `system/src/test/.../PSSystemServiceFindNotificationDefTest.java` (new) | 4 Mockito tests |
| `modules/extensions-workflow/.../PSNotificationsContextLoadFromHibernateTest.java` (new) | 5 `@Disabled` mapping tests |
| `modules/extensions-workflow/.../PSTransitionNotificationsContextLoadFromHibernateTest.java` (new) | 5 `@Disabled` mapping tests |

## Erlang checklist walk-through

### 1. Bug findings

**No bugs found.**

Specific checks:

- **JPQL property names:** All JPQL references match Hibernate entity properties. `PSNotification` has fields `workflowId`, `transitionId` (from `PSNotificationPK` + `IdClass` + the explicit `getTransitionId()`/`getWorkflowId()` methods). Verified via `PSNotification.java:231-244` and `PSNotificationPK.java:117-127`. `PSNotificationDef` accessed via `Session.get(Class, PK)` — no JPQL — so there is no property-name risk on that path.
- **JPQL filter clauses:** Both new methods use `workflowId = :wf` and (on workflow service) `transitionId = :tid` — matches the entity field names. `order by transitionNotificationId` matches the PK field name (line 62 of `PSNotification.java`).
- **Entity-null handling:** `findNotificationDef` returns `null` when no row matches (consistent with `loadWorkflowTransition` Phase 3 pattern); the factory then throws `PSEntryNotFoundException` with the same message as the legacy raw-JDBC path. `findTransitionNotifications` returns an empty list (consistent with `findStateRoles` Phase 4b); the factory initializes `m_nCount = 0` and the consumer's `if (0 == tnc.getCount())` early-return at `PSExitNotifyAssignees.java:478` matches the legacy behaviour.
- **Insertion cursor order:** Phase 4c preserves the same cursor ordering as the legacy raw-JDBC path (`order by transitionNotificationId` approximates the unordered insert order the legacy query produced — there is no `ORDER BY` clause in the legacy `QRYSTRING` at line 230-234, but the entries are all reachable via `moveNext()` and the `m_nNotificationIDList` is purely a vector, so any order is acceptable so long as it is deterministic. `transitionNotificationId` is the PK surrogate; the natural insert ordering correlates with it. If the consumer turns out to be order-sensitive beyond `moveNext/`get`, a follow-up can swap to `order by notificationId`).
- **Integer overflow / widening:** `populateFromHibernate` in `PSNotificationsContext` casts `getGUID().longValue()` to `int` for `m_nNotificationID`. The legacy constructor used `int` for the same field, so the entity's `longValue()` will be a valid surrogate id well within `int` range. `PSTransitionNotificationsContext` uses `getNotificationId().longValue()` which is typed `long` on `PSNotification` and cast to `int` — same constraint, same property.
- **Path/file I/O:** No new path code. The only path-related artefact is the legacy `PSConnectionMgr.getQualifiedIdentifier(...)` static-initializer references in `PSNotificationsContext` and `PSTransitionNotificationsContext` — those are read-only and stay in the legacy class schedule (Phase 4d will mechanically migrate them by reading the qualified table names the same way they have been since Phase 2).
- **Cross-platform paths:** No new file/path code.

### 2. Behavioural tests added

| Test file | Tests | Notes |
|---|---|---|
| `PSWorkflowServiceFindTransitionNotificationsTest` | 5 | Argument validation, JPQL shape, parameter forwarding, result pass-through. |
| `PSSystemServiceFindNotificationDefTest` | 4 | Argument validation, composite key, pass-through. |
| `PSNotificationsContextLoadFromHibernateTest` | 5 | `@Disabled` (legacy static-init constraint). |
| `PSTransitionNotificationsContextLoadFromHibernateTest` | 5 | `@Disabled` (legacy static-init constraint). |

Disabled tests will be re-enabled when the Phase 4+ Spring+H2 infrastructure lands (per the same plan that re-enables the Phase 4b `PSLoadFromHibernateTest`).

### 3. Backwards compatibility

- `PSNotificationsContext` constructor signature preserved (binary compat).
- `PSTransitionNotificationsContext` constructor signature preserved (binary compat).
- `PSExitNotifyAssignees#sendNotifications(...)` — `Connection` parameter removed, signature changes (binary-incompatible). Phase 4b already broke the binary-compat pattern for `PSExitAddEditAuthFlag` / `PSExitAuthenticateUser`; tracked in the Phase 4c follow-up of `phase4-scope-survey.md`.
- `PSSendNotificationsTest` updated to match the new signature.

### 4. Erlang-specific review patterns

- **String-SQL drift:** No new string SQL. The only SQL still in the codebase for this module is the legacy `PSNotificationsContext.QRYSTRING` and `PSTransitionNotificationsContext.QRYSTRING` which are not used by the new code path.
- **Legacy constructor retention:** Both legacy raw-JDBC constructors remain in place for binary compat, marked `@Deprecated` per the existing `@Deprecated` annotation on the class.
- **Resource cleanup:** The new `loadFromHibernate` factories hold no JDBC resources — Hibernate's session cache is the only resident state.
- **N+1 risk:** `PSTransitionNotificationsContext.loadFromHibernate` could trigger N+1 if there are many notifications per transition, but the legacy code already had the same shape (one cursor walk per notification loop). The new factory performs exactly one `findTransitionNotifications` query to populate the cursor, then `PSNotificationsContext.loadFromHibernate` is called once per row (already how the legacy code worked). No N+1 regression.
- **Transaction boundary:** `findTransitionNotifications` and `findNotificationDef` are `@Transactional`, so they participate in the surrounding Spring transaction (matching the assumption made by every other Phase 4 service method).

### 5. Compile-clean check

- `extensions-workflow` clean install: **BUILD SUCCESS**, 34 tests (16 active, 18 disabled — old 8 + new 5 + new 5).
- `system` clean install: **BUILD SUCCESS**, 26 service tests pass (existing Phase 4a/4b + new Phase 4c).
- No new compiler / Spotless / Surefire warnings on changed files.

## Gate

**May commit/push: yes.** Branch is ready.

## Out-of-scope (captured in `phase4-scope-survey.md`)

- Phase 4d: `PSExitAddPossibleTransitions{,Ex}` + `PSExitPerformTransition` + `CONTENTADHOCUSERS` writes + `PSConnectionMgr` deletion.
- Phase 4+: Spring+H2 test infrastructure.
