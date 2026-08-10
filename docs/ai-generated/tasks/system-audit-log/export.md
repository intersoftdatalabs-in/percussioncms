# System audit log export (CSV / JSON)

**Issue:** [#2715](https://github.com/intersoftdatalabs-in/percussioncms/issues/2715) (Parent [#2620](https://github.com/intersoftdatalabs-in/percussioncms/issues/2620) Phase 5 slice 1)  
**Status:** Implemented (REST)

## Endpoint

| Method | Path | AuthZ |
|--------|------|-------|
| `GET` | `/Rhythmyx/rest/auditlog/export` | Admin role **or** role property `sys_securityAuditLogViewer` (truthy) |

Same permission model as Phase 3 query API (`GET /auditlog/entries`). Unauthorized → **HTTP 403**.

### Query parameters

| Param | Description |
|-------|-------------|
| `format` | `csv` or `json` (default `json`) |
| `from` / `to` | ISO-8601 instants (same as query API) |
| `module` | Module code filter (e.g. `AUTH`) |
| `eventType` | Event type filter |
| `outcome` | Outcome filter (`SUCCESS`, `FAILURE`, …) |
| `actor` | Actor (user name), case-insensitive |
| `maxRows` | Max rows to export (default **5000**, hard cap **10000**) |

### Response

| Format | Content-Type | Content-Disposition |
|--------|--------------|---------------------|
| `json` | `application/json` | `attachment; filename="system-audit-log.json"` |
| `csv` | `text/csv` | `attachment; filename="system-audit-log.csv"` |

- **JSON:** root array of entry objects (ISO-8601 `eventTime`, no root wrapper).
- **CSV:** RFC 4180-style header + data rows; columns match wire DTO field names (`auditId`, `eventTime`, `moduleCode`, …).

### Examples

```http
GET /Rhythmyx/rest/auditlog/export?format=csv&module=AUTH&from=2026-01-01T00:00:00Z
GET /Rhythmyx/rest/auditlog/export?format=json&actor=admin&maxRows=1000
```

## Layers

| Layer | Location |
|-------|----------|
| Resource | `rest` `com.percussion.rest.auditlog.AuditLogResource` |
| Formatter | `rest` `SystemAuditLogExport` |
| Adaptor | `rest` `IAuditLogAdaptor#export` → `sitemanage` `AuditLogAdaptor` |
| Store | `PSSystemAuditLogRepository#findEntries` (paged under the export cap) |

## Out of scope (siblings)

- Audit-of-audit (viewer access events) — separate slice
- Federal/ops runbook + integrity hash — separate slice
- Admin SPA export button — optional follow-up (REST-only is enough for this slice)
