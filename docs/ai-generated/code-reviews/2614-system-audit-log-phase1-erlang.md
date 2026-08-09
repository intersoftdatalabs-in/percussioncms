# Erlang Code Review — #2614 System-Wide Audit Log Phase 1

| Field | Value |
|-------|--------|
| **Date** | 2026-08-09 |
| **Reviewer** | Erlang (strict independent) |
| **Scope** | Uncommitted / local Phase 1 work for system-wide audit log |
| **Ticket** | [#2614](https://github.com/intersoftdatalabs-in/percussioncms/issues/2614) |
| **Design** | `docs/ai-generated/tasks/system-audit-log/design.md` |
| **Base note** | Reviewed artifact contents under the listed paths (full file context). Git CLI was not available in this review session for a unified `git diff` dump; inventory was taken from the working tree files matching the Phase 1 design checklist. |

## Summary

Phase 1 introduces a clean `com.intsof.percussioncms.auditlog` API (error codes with `isAuditable`, dual-write service, redaction, template formatting), a `PSX_SYSTEM_AUDIT_LOG` table definition, JPA entity + Spring repository, login/logout smoke hooks in `PSLoginServlet`, and a retention job skeleton. Core library tests cover dual-write identity, non-auditable skip, sink isolation, and redaction; system tests cover entity mapping and login logger smoke via the memory repository.

**Dominant risk:** `PSSystemAuditLogRepository.afterPropertiesSet()` registers the **raw Spring bean** (`this`) into `DefaultAuditLogService.Holder` *before* AOP creates the transactional proxy. Production dual-write to the DB will almost certainly hit `TransactionRequiredException` (or equivalent), get swallowed as `AUDIT_SINK_FAILURE`, and leave only Log4j — silently defeating Phase 1 durable dual-write. That is a hard **bug**.

Secondary gaps: incomplete JSON escaping for attributes, login-success audit ordered after `sendRedirect`, thin coverage for retention/registration, and a vacuous redaction assertion in one service test.

**Cross-platform path checklist:** N/A — no new filesystem path construction, install path joins, or path assertions in this change set. **Outcome: clean.**

**Memory patterns hit:** empty catch / swallowed failures (sink failures are logged — good); missing behavioral proof for integration wiring; Spring transactional proxy / shared-context companion awareness.

## Recommendation

**request-changes**

## Gate

| Gate | Result |
|------|--------|
| Bugs open | **yes** (transactional dual-write registration) |
| Missing behavioral tests (non-trivial) | partial — core API OK; repository TX/wiring not proven |
| Non-portable paths | **pass** |
| May commit/push | **no** |

## Issues

### Issue 1 -- Severity: bug
- File: `system/services/src/com/percussion/services/audit/impl/PSSystemAuditLogRepository.java:42-45` (and `save` at 47-52)
- Description: `afterPropertiesSet()` does `DefaultAuditLogService.Holder.set(DefaultAuditLogService.create(this))`. Spring invokes `InitializingBean.afterPropertiesSet` **before** `BeanPostProcessors` create the AOP transactional proxy. The `AuditLogRepository` held by `RepositoryAuditLogSink` is therefore the raw target. `@Transactional` on `save` / `deleteOlderThan` does not apply to those external calls. Login/logout (and any other `Holder.get()` path) will call `entityManager.persist(...)` with no active transaction → runtime failure, caught by `DefaultAuditLogService` sink loop → only Log4j dual-write path remains. Design and README claim JPA durable store when Spring wires; production behavior will not match.
- Suggestion: Do **not** pass `this` from `afterPropertiesSet`. Prefer one of:
  1. Inject `PlatformTransactionManager` / `TransactionTemplate` and open a transaction **inside** `save` / `deleteOlderThan` (works regardless of caller), or
  2. Implement `SmartInitializingSingleton` / `ApplicationContextAware` and register `applicationContext.getBean("sys_systemAuditLogRepository", AuditLogRepository.class)` (the proxy) after singletons are ready, or
  3. Self-inject the repository via `@Lazy @Autowired` proxy field and register that field in a late init hook.
  Add a unit/integration test that proves `save` runs under a transaction (or that `TransactionTemplate.execute` is invoked) when called through `DefaultAuditLogService.Holder`, not only through a Spring-injected client.
- Status: open

### Issue 2 -- Severity: bug
- File: `system/src/main/java/com/percussion/servlets/PSLoginServlet.java:523` (relative to authenticate success path)
- Description: `PSSystemAuditLogger.loginSuccess` runs **after** `response.sendRedirect(safeRedirect)`. If redirect throws, or the success path aborts between auth and redirect, a successful authentication may never produce an audit row. Security audit trails should record successful login when authentication succeeds, independent of subsequent redirect mechanics. Logout correctly audits before session teardown; success is inconsistent.
- Suggestion: Call `PSSystemAuditLogger.loginSuccess(request, uid)` immediately after successful `PSSecurityFilter.authenticate(...)` (and still wrap in try/catch like logout so audit sink issues cannot break login). Keep failure audit in the `LoginException` catch as today.
- Status: open

### Issue 3 -- Severity: suggestion
- File: `system/services/src/com/percussion/services/audit/data/PSSystemAuditLogEntry.java:114-132`
- Description: `attributesToJson` / `jsonEscape` only escape `\` and `"`. Newlines, tabs, and other control characters produce **invalid JSON**. Phase 1 login path rarely sets attributes, but any call site that puts free-text attributes into the map will persist unreadable/broken `ATTRIBUTES_JSON` for Admin UI / REST consumers (later phases).
- Suggestion: Escape control characters (`\n`, `\r`, `\t`, other `\u0000-\u001F`) or use an existing Jackson/JSON utility already on the system classpath. Add a unit test with a value containing quotes and a newline.
- Status: open

### Issue 4 -- Severity: suggestion
- File: `system/services/src/com/percussion/services/audit/PSSystemAuditLogger.java:87-92`
- Description: Client IP is only `request.getRemoteAddr()`. Behind Jetty reverse-proxy / load balancer deployments, durable audit rows will record the proxy address, not the end client — weak for AUTH_FAILURE / AUTH_LOGIN forensics (AU-2/AU-3 intent).
- Suggestion: Document Phase 1 limitation; later use an existing product trusted-proxy / remote-IP helper if one exists, or a documented `server.properties` allowlist for `X-Forwarded-For`. Do not blindly trust arbitrary headers without an allowlist.
- Status: open

### Issue 5 -- Severity: suggestion
- File: `modules/perc-auditlog/src/test/java/com/intsof/percussioncms/auditlog/DefaultAuditLogServiceTest.java:77-96`
- Description: `sinkFailureDoesNotPreventOtherSinks` asserts  
  `logMessage.contains(REDACTED) || logMessage.contains("jdoe")`. The reason param is `"bad password"` (no `password=` form), so redaction usually does not fire and the assertion passes solely because actor `jdoe` is always present — vacuous for redaction/sink-failure interaction.
- Suggestion: Assert `sinkFailureCount == 1`, `bad` wrote nothing, `ok` has one record, and (if redaction is intended) use a reason like `password=hunter2` with `assertFalse(logMessage.contains("hunter2"))` — or drop the redaction clause from this test and rely on `redactsSecretsInMessages`.
- Status: open

### Issue 6 -- Severity: suggestion
- File: `system/business/src/com/percussion/rx/audit/PSSystemAuditLogRetentionJob.java` (class-level); tests absent
- Description: Retention skeleton is intentionally unwired (no Spring bean / schedule) — acceptable for Phase 1 “skeleton” per design. Still, `runOnce` / disable (`retentionDays <= 0`) / null repository branches have no unit tests with a mock repository.
- Suggestion: Add a small pure unit test with a mock `PSSystemAuditLogRepository` verifying disable path returns 0, null repo returns 0, and happy path forwards cutoff and returns deleted count. Schedule/config can remain a follow-on issue.
- Status: open

### Issue 7 -- Severity: suggestion
- File: `system/src/test/java/com/percussion/services/audit/PSSystemAuditLoggerTest.java`
- Description: Covers login success and failure into memory repository; no logout test; no assertion that failure outcome is `FAILURE` / event type `AUTH_FAILURE`; no test that `Holder` registration replaces the repository sink (would have caught Issue 1 earlier if paired with a transactional mock).
- Suggestion: Add logout smoke; assert outcomes/event types; add a focused test for repository registration once Issue 1 is fixed.
- Status: open

### Issue 8 -- Severity: nit
- File: `system/src/main/java/com/percussion/servlets/PSLoginServlet.java:157-161` vs success path ~523
- Description: Logout wraps audit in try/catch; login success/failure do not. `DefaultAuditLogService` already swallows sink `RuntimeException`s, so risk is low, but inconsistency remains if future changes throw outside sinks.
- Suggestion: Mirror the logout try/catch (or a shared private `safeAudit(Runnable)`) on login success/failure for defense in depth.
- Status: open

## What looks sound

- Intersoft 2026 copyright headers on new sources.
- `isAuditable` gate and null `eventType` guard in `DefaultAuditLogService`.
- Sink isolation + failure logging without failing the business call (API design).
- SLF4J-style `{}` formatter and canonical `[MOD-####]-[uuid]` line form.
- Redactor coverage (password KV, URL credentials, JWT, sensitive attribute keys) with dedicated tests.
- Table def columns/indexes align with entity fields; `packagesToScan=com.percussion` will pick up the entity.
- `system` module already depends on `audit-log` artifact.
- Login failure logs exception class name, not password material.
- Memory repository + Holder reset used correctly in `PSSystemAuditLoggerTest`.

## Change-class closure (Phase 1)

| Companion | Present? |
|-----------|----------|
| Core API + unit tests (`perc-auditlog`) | yes |
| Table def (`cmsTableDef.xml` → install lockstep copies) | yes (single source) |
| JPA entity + repository bean | yes (TX wiring incomplete — Issue 1) |
| Login smoke call sites | yes |
| Retention skeleton | yes (unscheduled — intentional) |
| Role property / REST / Admin UI | out of scope (later issues) |
| CADF removal | out of scope (later) |

## Recommendation detail

Do **not** commit or open/update a PR until **Issue 1** is fixed and re-tested. Prefer fixing **Issue 2** in the same pack (correct success-audit timing). Issues 3–7 may ship as follow-ups if explicitly deferred in the PR body, but Issue 1 is not deferrable if the PR claims dual-write to `PSX_SYSTEM_AUDIT_LOG`.

## Re-review (2026-08-09, post-fix)

### Mitigations

| Issue | Status | Mitigation |
|-------|--------|------------|
| 1 TX proxy / raw `this` | **fixed** | `TransactionTemplate` for `save` / `deleteOlderThan` / `countAll`; unit test verifies `persist` under TX manager |
| 2 loginSuccess after redirect | **fixed** | Audit immediately after `authenticate`; try/catch like logout |
| 3 JSON escape | **fixed** | Control-char escape in `jsonEscape` + unit test |
| 5 vacuous redaction assert | **fixed** | `password=hunter2` + assert no secret in message |
| 6 retention tests | **fixed** | `PSSystemAuditLogRetentionJobTest` |
| 7 logout / outcomes | **fixed** | logout smoke + FAILURE outcome assert |
| 4 client IP | deferred | documented Phase 1 limitation (proxy-blind) |

### Gate (re-review)

**Recommendation:** approve  
**May commit/push:** yes (after focused clean install evidence)

---

## Handoff

- **Reviewed:** Phase 1 system audit log API, dual-write service, entity/repository, login hooks, table def, retention skeleton, related unit tests, design note.
- **Top finding:** Spring raw-bean registration kills `@Transactional` for Holder dual-write path.
- **Recommendation:** `request-changes`; **May commit/push: no**.
- **Next:** Hephaestus fixes Issue 1 (+ preferably 2); Erlang re-review; then module standalone `mvnw clean install` for `modules/perc-auditlog` and `system` per Pre-PR Maven gate.

---

> Co-Authored by Grok (xAI) using agent Erlang (strict independent code review).
