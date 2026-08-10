# perc-auditlog (`audit-log`)

System-wide Percussion CMS audit logging and unified error-code support.

## Package

**Only supported package:** `com.intsof.percussioncms.auditlog`

* `SystemErrorCode` / package `*ErrorCodes` with explicit **`isAuditable`**
* `AuditLogService` dual-writes auditable events to Log4j (`server.log`) and an `AuditLogRepository` SPI
* Message form: `[AUTH-1001]-[<uuid>] …` with separate user/log message templates and redaction
* **Phase 2b:** `SecurityErrorCodes` (full SEC range), `ContentErrorCodes`, `WorkflowErrorCodes`,
  `PathItemErrorCodes` (CMS path/item/folder), `DesignErrorCodes` (design lifecycle + objectstore ACL)
  + `LegacyErrorCodeRegistry` bridge legacy `IPS*Errors` ints. Non-auditable / unregistered ints never
  dual-write. Central `PSErrorHandler.appendError` dual-writes only when the registry marks the legacy
  int auditable.

### Retired (Phase 2c / #2675)

* IBM CADF module `modules/jcadf-master` (`com.ibm.cadf:auditlogger`) — **removed from reactor**
* Legacy package `com.percussion.auditlog` (CADF-facing `PSAuditLogService` / event types) — **purged**
* Grep gate: `python3 scripts/verify-no-cadf-legacy-auditlog.py` (Windows: same via `python`)

Do **not** reintroduce CADF or the legacy package. Call sites must use
`com.intsof.percussioncms.auditlog` / `PSSystemAuditLogger`.

### Legacy design-object table (not CADF)

Design-object auditor rows remain on table **`PSX_DESIGN_AUDIT_LOG`** via JPA entity
`com.percussion.services.audit.data.PSAuditLogEntry` (package
`com.percussion.services.audit` — **not** the retired `com.percussion.auditlog`).

**Disposition (#2675 / Phase 2c):** keep the table and entity for existing design-audit
history and current `PSDesignObjectAuditor` writes. New system-wide security audit
events go to **`PSX_SYSTEM_AUDIT_LOG`** only. Dual-write of design lifecycle DESN codes
into the system store is Phase 2c slice 1 (#2673); this module no longer provides a
CADF/file third sink. Dropping or read-only-freezing `PSX_DESIGN_AUDIT_LOG` is a
follow-on schema decision outside this removal PR.

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

**Durable write TX:** repository `save` / retention deletes use Spring
`TransactionTemplate` with **`PROPAGATION_REQUIRES_NEW`** + explicit `flush`, so dual-write from
login (and other Holder call sites) commits independently of any ambient request transaction that
might roll back or never commit before redirect (#2727).

**Empty-table seed (H2 QA / Admin viewer):** property `systemAuditLogSeedIfEmpty` (default
`true`) inserts a few clearly labeled `[SEED]` AUTH demo rows when the table is empty at Spring
start, so Security Audit Log QA is not blocked with “0 of 0” before the first real login event.
Set `systemAuditLogSeedIfEmpty=false` when synthetic rows are not desired.

**Retention (AU-11):** Spring bean `sys_systemAuditLogRetentionJob`
(`PSSystemAuditLogRetentionJob`) deletes rows older than
`systemAuditLogRetentionDays` from `rxconfig/Server/server.properties` (default **365**).
Set the property to **0** or a negative value to disable automatic deletion. The job runs
daily as a daemon worker (same lifecycle style as the design-object audit reaper).

Login smoke path: `PSSystemAuditLogger` / `PSLoginServlet` (success, failure, logout).

Phase 2a migrated production call sites to `PSSystemAuditLogger` helpers + package
`*ErrorCodes`. Phase 2c (#2617 / #2675) removed CADF and the legacy package entirely.

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

## Phase 5 — Ops / federal runbook + integrity hash (#2620 / #2717)

* **Operator runbook:** `docs/ai-generated/tasks/system-audit-log/ops-federal-runbook.md`  
  Retention (`systemAuditLogRetentionDays` / `sys_systemAuditLogRetentionJob`), NTP guidance,  
  `sys_securityAuditLogViewer` grant, export AuthZ, NIST AU control matrix for deployers.
* **Integrity digest helper:** `com.intsof.percussioncms.auditlog.integrity.AuditIntegrityHash`  
  Deterministic SHA-256 (lowercase hex) over canonical row fields for export/query verification.  
  Not a digital signature or DB column — see runbook §7.

## Design

See `docs/ai-generated/tasks/system-audit-log/design.md` and  
`docs/ai-generated/tasks/system-audit-log/ops-federal-runbook.md`.
