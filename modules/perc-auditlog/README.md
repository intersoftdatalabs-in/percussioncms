# perc-auditlog (`audit-log`)

System-wide Percussion CMS audit logging and unified error-code support.

## Package

**New (preferred):** `com.intsof.percussioncms.auditlog`

* `SystemErrorCode` / package `*ErrorCodes` with explicit **`isAuditable`**
* `AuditLogService` dual-writes auditable events to Log4j (`server.log`) and an `AuditLogRepository` SPI
* Message form: `[AUTH-1001]-[<uuid>] …` with separate user/log message templates and redaction
* **Phase 2b:** `SecurityErrorCodes` + `LegacyErrorCodeRegistry` bridge legacy `IPSSecurityErrors` ints
  (auth/security first slice). Non-auditable / unregistered ints never dual-write.

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

## Legacy IPS*Errors bridge (Phase 2b auth/security)

```java
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;

// Prefer enum when available:
audit.log(SecurityErrorCodes.AUTHENTICATION_FAILED, ctx, "Directory", "ldap1", "jdoe");

// Central handlers with only a legacy int (e.g. PSException.getErrorCode()):
LegacyErrorCodeRegistry.logIfAuditable(audit, 9002, ctx, "Directory", "ldap1", "jdoe");
// Provider config noise (isAuditable=false) and unknown ints → no dual-write
```

Non-auditable codes (`isAuditable() == false`) never create audit rows.

Default dual-write: Log4j + in-process `ConcurrentMemoryAuditLogRepository` until Spring starts.

Production durable store: table `PSX_SYSTEM_AUDIT_LOG` with JPA entity
`com.percussion.services.audit.data.PSSystemAuditLogEntry` and
`PSSystemAuditLogRepository` (`sys_systemAuditLogRepository`), which registers itself on the
`DefaultAuditLogService.Holder` at Spring init.

Login smoke path: `PSSystemAuditLogger` / `PSLoginServlet` (success, failure, logout).

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
