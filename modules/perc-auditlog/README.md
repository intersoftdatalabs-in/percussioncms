# perc-auditlog (`audit-log`)

System-wide Percussion CMS audit logging and unified error-code support.

## Package

**New (preferred):** `com.intsof.percussioncms.auditlog`

* `SystemErrorCode` / package `*ErrorCodes` with explicit **`isAuditable`**
* `AuditLogService` dual-writes auditable events to Log4j (`server.log`) and an `AuditLogRepository` SPI
* Message form: `[AUTH-1001]-[<uuid>] …` with separate user/log message templates and redaction
* **Phase 2b:** `SecurityErrorCodes` (full SEC range), `ContentErrorCodes`, `WorkflowErrorCodes`,
  `PathItemErrorCodes` (CMS path/item/folder), `DesignErrorCodes` (design lifecycle + objectstore ACL)
  + `LegacyErrorCodeRegistry` bridge legacy `IPS*Errors` ints. Non-auditable / unregistered ints never
  dual-write. Central `PSErrorHandler.appendError` dual-writes only when the registry marks the legacy
  int auditable.

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

## Legacy IPS*Errors bridge (Phase 2b)

```java
import com.intsof.percussioncms.auditlog.LegacyErrorCodeRegistry;
import com.intsof.percussioncms.auditlog.codes.ContentErrorCodes;
import com.intsof.percussioncms.auditlog.codes.DesignErrorCodes;
import com.intsof.percussioncms.auditlog.codes.PathItemErrorCodes;
import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.intsof.percussioncms.auditlog.codes.WorkflowErrorCodes;

// Prefer enum when available:
audit.log(SecurityErrorCodes.AUTHENTICATION_FAILED, ctx, "Directory", "ldap1", "jdoe");
audit.log(ContentErrorCodes.CREATE, ctx, AuditOutcome.SUCCESS, "guid", "42", "/Sites/demo");
audit.log(WorkflowErrorCodes.ACCESS_DENIED, ctx, "5", "jdoe");
audit.log(PathItemErrorCodes.FOLDER_PERMISSION_DENIED, ctx);
audit.log(DesignErrorCodes.SRV_ACL_NO_ADMIN, ctx);

// Central handlers with only a legacy int (e.g. PSException.getErrorCode()):
LegacyErrorCodeRegistry.logIfAuditable(audit, 9002, ctx, "Directory", "ldap1", "jdoe"); // SEC
LegacyErrorCodeRegistry.logIfAuditable(audit, 17001, ctx); // CONT conversion — non-auditable skip
LegacyErrorCodeRegistry.logIfAuditable(audit, 6, ctx, "5", "jdoe"); // WF access denied
// Provider/config/conversion/path/design noise (isAuditable=false) and unknown ints → no dual-write
```

| Catalog | Ranges | Notes |
|---------|--------|-------|
| `SecurityErrorCodes` | 9001–9026, residual SEC (host filter, OS, dir-auth 9801+) | Auth/security exception bridge |
| `ContentErrorCodes` | 2001–2006 lifecycle; 17001–17010 conversion | Aligns Phase 2a lifecycle numbering |
| `WorkflowErrorCodes` | 4001 transition; 1–10 service | Aligns Phase 2a transition numbering |
| `PathItemErrorCodes` | CMS path/item/folder (e.g. 13007) | Folder/path permission bridge |
| `DesignErrorCodes` | Design lifecycle + objectstore ACL (e.g. 2353) | Design server ACL bridge |

Non-auditable codes (`isAuditable() == false`) never create audit rows.

`PSErrorHandler.appendError` calls `PSSystemAuditLogger.logLegacyIfAuditable` for every
`PSException` so only cataloged auditable codes dual-write on the central error path.

Default dual-write: Log4j + in-process `ConcurrentMemoryAuditLogRepository` until Spring starts.

Production durable store: table `PSX_SYSTEM_AUDIT_LOG` with JPA entity
`com.percussion.services.audit.data.PSSystemAuditLogEntry` and
`PSSystemAuditLogRepository` (`sys_systemAuditLogRepository`), which registers itself on the
`DefaultAuditLogService.Holder` at Spring init.

**Retention (AU-11):** Spring bean `sys_systemAuditLogRetentionJob`
(`PSSystemAuditLogRetentionJob`) deletes rows older than
`systemAuditLogRetentionDays` from `rxconfig/Server/server.properties` (default **365**).
Set the property to **0** or a negative value to disable automatic deletion. The job runs
daily as a daemon worker (same lifecycle style as the design-object audit reaper).

Login smoke path: `PSSystemAuditLogger` / `PSLoginServlet` (success, failure, logout).

Phase 2a migrates production call sites off legacy CADF `PSAuditLogService` to
`PSSystemAuditLogger` helpers + package `*ErrorCodes` (`ContentErrorCodes`,
`UserManagementErrorCodes`, `WorkflowErrorCodes`, extended `AuthenticationErrorCodes`).
Legacy CADF types remain in this module until Phase 2c (#2617).

## Phase 3 — REST query + role property (#2618)

**Permission:** Rhythmyx role property `sys_securityAuditLogViewer` (truthy `true`/`yes`/`1`/`y`). Members of **Admin** always may view. Pure helper: `com.percussion.services.audit.PSSystemAuditLogPermission`.

**REST** (public adaptor pattern):

* Resource: `com.percussion.rest.auditlog.AuditLogResource` → `/auditlog/entries`
* Interface: `IAuditLogAdaptor`
* Impl: `com.percussion.apibridge.AuditLogAdaptor` (sitemanage) → `PSSystemAuditLogRepository` query helpers
* Seed: installer `cmsTableData.xml` registers the property and seeds Admin=`true`

Unauthorized callers receive HTTP **403**. Admin UI for browsing is Phase 4 (#2619).

## Phase 4 — Admin UI + Playwright (#2619)

* WebUI Admin → System Tools → **Security Audit Log** (`SecurityAuditLogViewer`)
* Client: `WebUI/src/main/ts/api/auditlog/*` → existing REST list/detail (no new endpoints)
* TMX: `perc.ui.admin.auditlog@*` in `modules/perc-i18n` `CmsUi.tmx`
* Playwright surface: `modules/perc-qa-automation/frontend/tests/admin-security-audit-log.spec.js`

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
