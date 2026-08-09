# perc-auditlog (`audit-log`)

System-wide Percussion CMS audit logging and unified error-code support.

## Package

**New (preferred):** `com.intsof.percussioncms.auditlog`

* `SystemErrorCode` / package `*ErrorCodes` with explicit **`isAuditable`**
* `AuditLogService` dual-writes auditable events to Log4j (`server.log`) and an `AuditLogRepository` SPI
* Message form: `[AUTH-1001]-[<uuid>] …` with separate user/log message templates and redaction

**Legacy (to be removed after migration):** `com.percussion.auditlog` + IBM CADF (`auditlogger` module)

## Usage (Phase 1)

```java
import com.intsof.percussioncms.auditlog.*;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;

AuditLogService audit = DefaultAuditLogService.Holder.get();
audit.log(
    AuthenticationErrorCodes.LOGIN_SUCCESS,
    AuditContext.builder().actor("jdoe").sourceIp("10.0.0.1").build(),
    AuditOutcome.SUCCESS,
    "jdoe",
    "10.0.0.1");
```

Non-auditable codes (`isAuditable() == false`) never create audit rows.

Default dual-write: Log4j + in-process `ConcurrentMemoryAuditLogRepository` until Spring starts.

Production durable store: table `PSX_SYSTEM_AUDIT_LOG` with JPA entity
`com.percussion.services.audit.data.PSSystemAuditLogEntry` and
`PSSystemAuditLogRepository` (`sys_systemAuditLogRepository`), which registers itself on the
`DefaultAuditLogService.Holder` at Spring init.

Login smoke path: `PSSystemAuditLogger` / `PSLoginServlet` (success, failure, logout).

## Phase 3 — REST query + role property (#2618)

**Permission:** Rhythmyx role property `sys_securityAuditLogViewer` (truthy `true`/`yes`/`1`/`y`). Members of **Admin** always may view. Pure helper: `com.percussion.services.audit.PSSystemAuditLogPermission`.

**REST** (public adaptor pattern):

* Resource: `com.percussion.rest.auditlog.AuditLogResource` → `/auditlog/entries`
* Interface: `IAuditLogAdaptor`
* Impl: `com.percussion.apibridge.AuditLogAdaptor` (sitemanage) → `PSSystemAuditLogRepository` query helpers
* Seed: installer `cmsTableData.xml` registers the property and seeds Admin=`true`

Unauthorized callers receive HTTP **403**. Admin UI for browsing is Phase 4 (#2619).

## Building

```bash
cd modules/perc-auditlog
../../mvnw clean install
```

Windows:

```bat
cd modules\perc-auditlog
..\..\mvnw.cmd clean install
```

## Design

See `docs/ai-generated/tasks/system-audit-log/design.md`.
