# System-Wide Audit Log — Design Note (Phase 0)

**Status:** Active  
**Date:** 2026-08-09  
**Package:** `com.intsof.percussioncms.auditlog`  
**Module:** `modules/perc-auditlog` (artifact `audit-log`)

## Decisions (locked)

| Topic | Decision |
|-------|----------|
| Dual write | `server.log` (Log4j) + `PSX_SYSTEM_AUDIT_LOG` (DB); no CADF third sink |
| Error catalog | Unified `SystemErrorCode` with **`isAuditable`**; package `*ErrorCodes` enums |
| Placeholders | Sequential `{}` (SLF4J-style) |
| Permission | Rhythmyx Role model; Role property `sys_securityAuditLogViewer`; Admin always allowed |
| Log id | UUID string, same id on both sinks |
| CADF / jcadf | Remove after call-site migration |

## Architecture

See session plan and root implementation under `com.intsof.percussioncms.auditlog`.

```text
SystemErrorCode (*ErrorCodes) → AuditLogService
  → redact + format [MOD-####]-[logId] message
  → Log4j sink (server.log)
  → AuditLogRepository SPI (DB impl in system — later phase)
```

## Phase 1 status (2026-08-09)

- [x] Core API `com.intsof.percussioncms.auditlog` + unit tests
- [x] Dual-write: Log4j + repository SPI (memory default; JPA when Spring wires)
- [x] Table `PSX_SYSTEM_AUDIT_LOG` in `cmsTableDef.xml`
- [x] Entity `PSSystemAuditLogEntry` + `PSSystemAuditLogRepository` (`@PSBaseBean`)
- [x] Login/logout smoke path via `PSSystemAuditLogger` in `PSLoginServlet`
- [x] Retention job skeleton `PSSystemAuditLogRetentionJob`
- [ ] PR merge for #2614

## Phase tracking (GitHub)

| Phase | Issue |
|-------|-------|
| 0+1 Core API | [#2614](https://github.com/intersoftdatalabs-in/percussioncms/issues/2614) |
| 2a Call-site migrate | [#2615](https://github.com/intersoftdatalabs-in/percussioncms/issues/2615) |
| 2b ErrorCodes unification | [#2616](https://github.com/intersoftdatalabs-in/percussioncms/issues/2616) |
| 2c Design auditor + retire CADF | [#2617](https://github.com/intersoftdatalabs-in/percussioncms/issues/2617) |
| 3 Role property + REST | [#2618](https://github.com/intersoftdatalabs-in/percussioncms/issues/2618) |
| 4 Admin UI + Playwright | [#2619](https://github.com/intersoftdatalabs-in/percussioncms/issues/2619) |
| 5 Hardening | [#2620](https://github.com/intersoftdatalabs-in/percussioncms/issues/2620) |

## References

* Rhythmyx 7.3 Implementation Guide — Roles, communities, folder ACLs  
* Rhythmyx 7.3 Administration Manual — Role properties (`sys_defaultCommunity`, …)  
* NIST SP 800-53 Rev. 5 AU family  
