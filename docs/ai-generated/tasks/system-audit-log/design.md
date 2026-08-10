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
| CADF / jcadf | **Removed** Phase 2c (#2675): `modules/jcadf-master` deleted; no `com.ibm.cadf` product dependency |
| Legacy `com.percussion.auditlog` | **Purged** Phase 2c (#2675); keep only `com.intsof.percussioncms.auditlog` |
| Design audit table `PSX_DESIGN_AUDIT_LOG` | **Keep** for design-object history (`PSAuditLogEntry` / `PSDesignObjectAuditor`). Not a CADF store. System security events use `PSX_SYSTEM_AUDIT_LOG` only. Read-only freeze or drop is a later schema decision; no silent dual-store without this note. |
| Grep gate | `scripts/verify-no-cadf-legacy-auditlog.py` — no `com.ibm.cadf` / `com.percussion.auditlog` in production Java/POMs; no `modules/jcadf-master` tree |

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
- [x] PR merge for #2614

## Phase 2b status (auth/security + residual catalogs)

- [x] `SecurityErrorCodes` for full defined `IPSSecurityErrors` ints (9001–9026, host/OS/role, dir 98xx, cataloger 99xx)
- [x] `PathItemErrorCodes` for CMS path/folder/item (`IPSCmsErrors` subset) with `isAuditable`
- [x] `DesignErrorCodes` for design lifecycle (2901–2903) + objectstore ACL subset with `isAuditable`
- [x] `LegacyErrorCodeRegistry` int → `SystemErrorCode` with safe non-auditable default
- [x] `IPSSecurityErrors` bridged ints; `PSSystemAuditLogger.logLegacyIfAuditable`
- [x] Central `PSErrorHandler.appendError` dual-writes only when registry marks auditable
- [x] Unit tests: non-auditable skip dual-write; auditable SEC/path/design dual-write
- [ ] Follow-on: content/workflow ErrorCodes residual (#2635) when not yet merged; bulk remaining objectstore validation ints as needed

## Phase 3 — Role property + REST query (#2618)

### Operator notes (Admin Manual style)

**Role property:** `sys_securityAuditLogViewer`

| Setting | Meaning |
|---------|---------|
| Role **Admin** | Always allowed to query the system security audit log (even without the property) |
| Role property `sys_securityAuditLogViewer=true` | Grants query access to members of that role |
| Truthy values | `true`, `yes`, `y`, `1` (case-insensitive) |
| Falsy / absent | No access for non-Admin roles |

**Fresh install seed:** installer data registers the property name under `PSX_ADMINLOOKUP` (so Server Admin can assign it) and seeds `true` on the **Admin** role (`PSX_ATTRIBUTE_*` / `PSX_ROLE_ATTRIBUTES`). Existing sites may add the property on Admin or other roles via Server Admin → Roles.

**Public REST (read-only):**

| Method | Path | AuthZ |
|--------|------|-------|
| `GET` | `/Rhythmyx/rest/auditlog/entries` | Admin or property |
| `GET` | `/Rhythmyx/rest/auditlog/entries/{auditId}` | Admin or property |

Query parameters on list: `from`, `to` (ISO-8601 instants), `module`, `eventType`, `outcome`, `actor`, `offset`, `limit` (default 50, max 200).

Responses: `200` page/entry, `403` without permission, `404` missing id, `400` bad dates.

**Not in Phase 3:** Admin WebUI viewer / Playwright (#2619).

## Phase 4 — Admin UI + i18n + Playwright (#2619)

- [x] Admin Tools → **Security Audit Log** viewer (filters, pagination, detail for `userMessage` / `logMessage`)
- [x] REST client uses existing `GET /services/auditlog/entries` + `…/{auditId}` (no new REST)
- [x] TMX keys under `perc.ui.admin.auditlog@*` / tools tab chrome in `CmsUi.tmx` (en-us + lou)
- [x] Vitest: query builder, viewer list/detail/403/filters, tools section
- [x] Playwright surface: `modules/perc-qa-automation/frontend/tests/admin-security-audit-log.spec.js` (`@security-audit-log`)

**AuthZ (UI):** Admin shell remains Admin-only (`RequireRole gate=admin`). Server REST still enforces Admin **or** `sys_securityAuditLogViewer` for any non-UI clients.

## Phase 2c status — retire CADF / jcadf (#2617 / #2675)

- [x] Confirm production no longer imports `com.ibm.cadf` or `com.percussion.auditlog` (call sites on `PSSystemAuditLogger` / intsof API)
- [x] Remove reactor module `modules/jcadf-master` and delete tree
- [x] Purge legacy CADF-facing types from `modules/perc-auditlog` (keep intsof API)
- [x] Drop `com.ibm.cadf:auditlogger` Maven dependency from `audit-log` POM
- [x] Portable grep gate `scripts/verify-no-cadf-legacy-auditlog.py` + unit tests
- [x] Document `PSX_DESIGN_AUDIT_LOG` disposition (keep / not CADF; system events on `PSX_SYSTEM_AUDIT_LOG`)
- [ ] Sibling slices (separate PRs): design auditor dual-write DESN → system store (#2673); retention job wiring (#2674)

## Phase 5 — Hardening / compliance pack (#2620)

| Slice | Issue | Notes |
|-------|-------|-------|
| Export CSV/JSON | [#2715](https://github.com/intersoftdatalabs-in/percussioncms/issues/2715) | Same AuthZ as query; see [export.md](./export.md) when present |
| Audit-of-audit access events | [#2716](https://github.com/intersoftdatalabs-in/percussioncms/issues/2716) | Viewer list/detail (and export) dual-write |
| **Federal/ops runbook + integrity hash** | [#2717](https://github.com/intersoftdatalabs-in/percussioncms/issues/2717) | **[ops-federal-runbook.md](./ops-federal-runbook.md)**; `AuditIntegrityHash` SHA-256 helper |

Operator/federal deployers: start with **[ops-federal-runbook.md](./ops-federal-runbook.md)** (retention job, NTP, role property grant, export permission, AU control matrix, integrity digest).

## Phase tracking (GitHub)

| Phase | Issue |
|-------|-------|
| 0+1 Core API | [#2614](https://github.com/intersoftdatalabs-in/percussioncms/issues/2614) |
| 2a Call-site migrate | [#2615](https://github.com/intersoftdatalabs-in/percussioncms/issues/2615) |
| 2b ErrorCodes unification | [#2616](https://github.com/intersoftdatalabs-in/percussioncms/issues/2616) |
| 2c Design auditor + retire CADF | [#2617](https://github.com/intersoftdatalabs-in/percussioncms/issues/2617) (slice 3 removal: [#2675](https://github.com/intersoftdatalabs-in/percussioncms/issues/2675)) |
| 3 Role property + REST | [#2618](https://github.com/intersoftdatalabs-in/percussioncms/issues/2618) |
| 4 Admin UI + Playwright | [#2619](https://github.com/intersoftdatalabs-in/percussioncms/issues/2619) |
| 5 Hardening | [#2620](https://github.com/intersoftdatalabs-in/percussioncms/issues/2620) |

## References

* Rhythmyx 7.3 Implementation Guide — Roles, communities, folder ACLs  
* Rhythmyx 7.3 Administration Manual — Role properties (`sys_defaultCommunity`, …)  
* NIST SP 800-53 Rev. 5 AU family  
* [ops-federal-runbook.md](./ops-federal-runbook.md) — Phase 5 operator / federal AU matrix  

