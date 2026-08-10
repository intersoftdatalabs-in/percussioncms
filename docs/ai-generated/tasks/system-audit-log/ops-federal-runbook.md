# System Audit Log — Federal / Ops Runbook (Phase 5)

**Status:** Active  
**Date:** 2026-08-10  
**Parent epic:** [#2620](https://github.com/intersoftdatalabs-in/percussioncms/issues/2620)  
**Slice:** [#2717](https://github.com/intersoftdatalabs-in/percussioncms/issues/2717)  
**Audience:** Deployers, operators, federal / compliance reviewers  
**Related design:** [design.md](./design.md) · module README: `modules/perc-auditlog/README.md`

This runbook describes **how to operate and grant access** to the system-wide security audit log (`PSX_SYSTEM_AUDIT_LOG` + dual-write Log4j), and maps product capabilities to the **NIST SP 800-53 Rev. 5 AU** control family for deployers. It is **not** a full ATO package or FedRAMP SSP; it is product-aligned operator guidance.

---

## 1. What is stored

| Sink | Location | Purpose |
|------|----------|---------|
| Durable DB | Table **`PSX_SYSTEM_AUDIT_LOG`** | Queryable security/compliance events (JPA: `PSSystemAuditLogEntry`) |
| File / Log4j | **`server.log`** (and any site-specific Log4j routing) | Same event line dual-written for ops/SIEM file shipping |

Each event has a UUID **`AUDIT_ID`** shared on both sinks, UTC **`EVENT_TIME`**, module/message codes, outcome, actor, target, source IP/host, redacted user/log messages, optional correlation id / attributes, and server node.

**Not this store:** design-object history remains on **`PSX_DESIGN_AUDIT_LOG`** (`PSDesignObjectAuditor`). System security events use **`PSX_SYSTEM_AUDIT_LOG` only**.

---

## 2. Retention job (NIST AU-11)

### 2.1 Property

| Key | File | Default | Meaning |
|-----|------|---------|---------|
| `systemAuditLogRetentionDays` | `rxconfig/Server/server.properties` (shipped template: `system/config/server.properties`) | **365** | Keep DB rows this many days |

- **`<= 0`** (e.g. `0` or negative): **disables** automatic deletion; rows kept indefinitely until operator action.
- Invalid / blank values fall back to **365**.
- Values above **3650** (~10 years) are **capped** by the job to avoid misconfiguration extremes.

### 2.2 Spring bean

| Item | Value |
|------|--------|
| Bean name | `sys_systemAuditLogRetentionJob` |
| Class | `com.percussion.rx.audit.PSSystemAuditLogRetentionJob` |
| Schedule | Daemon worker; default sleep **1440 minutes (24 hours)** between runs |
| Action | Deletes durable rows with `EVENT_TIME` older than *now − retentionDays* (UTC clock) |

Sleep interval is **not** a `server.properties` key in production (tests may inject a shorter interval). After changing `systemAuditLogRetentionDays`, **restart the CMS** so the bean re-resolves server properties at startup (runtime setter exists for tests; operators should treat properties as restart-bound unless your site has a supported hot-reload path).

### 2.3 Operator checklist

1. Confirm agency / contract retention policy (often 1 year minimum for security logs; some programs require longer).
2. Set `systemAuditLogRetentionDays` accordingly, or `0` if an external archive/SIEM is the system of record and the DB must not auto-purge.
3. Ensure disk capacity for Log4j rotation **and** DB growth if retention is lengthened.
4. Prefer **exporting** (CSV/JSON — Phase 5 export API, same AuthZ as query) before lowering retention or purging manually.
5. Do **not** truncate `PSX_SYSTEM_AUDIT_LOG` ad hoc without a change ticket and evidence package — that is itself an auditable ops action.

### 2.4 Log4j file retention

DB retention does **not** delete historical lines from `server.log`. File retention is governed by Jetty/Log4j configuration (see `modules/perc-jetty` logging notes / site Log4j). Align file retention with the same policy or ship logs to SIEM.

---

## 3. Time synchronization (NTP) — event timestamps

Audit **`EVENT_TIME`** is recorded as a UTC instant from the server clock (`Clock.systemUTC()` on the default audit service path).

| Requirement | Guidance |
|-------------|----------|
| Accurate timestamps | Host OS **must** run reliable NTP/chrony/Windows Time against an approved time source |
| Multi-node | All CMS nodes should share the same trusted time base so correlation across `SERVER_NODE` is meaningful |
| Clock skew | Large skew breaks forensic timeline ordering and may confuse retention cutoffs |
| Operator proof | Document NTP peer/source in the site runbook; include time-sync status in ATO evidence packs |

The product does **not** implement its own NTP client. Time integrity is an **infrastructure control**.

---

## 4. Role property grant — `sys_securityAuditLogViewer`

### 4.1 Rules

| Principal | Access to query / export / UI REST |
|-----------|-------------------------------------|
| Member of role **Admin** | **Always allowed** (even without the property row) |
| Member of any role with property `sys_securityAuditLogViewer` truthy | Allowed |
| Everyone else | **Denied** (HTTP **403** on REST) |

**Truthy values:** `true`, `yes`, `y`, `1` (case-insensitive).  
**Falsy / absent:** no access for non-Admin roles. Explicit negatives (`false`, `no`, `n`, `0`) do not grant access.

**Helper (server):** `com.percussion.services.audit.PSSystemAuditLogPermission`  
**Constant:** `PSSystemAuditLogPermission.ROLE_PROPERTY` = `sys_securityAuditLogViewer`

### 4.2 Fresh install seed

Installer data registers the property name for Server Admin assignment and seeds **Admin=`true`**. Existing upgraded sites may need to add the property on Admin or other roles via **Server Admin → Roles** if the seed was not applied historically.

### 4.3 Granting a non-Admin auditor role (recommended pattern)

1. Create or select a Rhythmyx role used only for security audit review (e.g. `SecurityAuditor`).
2. Set role property **`sys_securityAuditLogViewer` = `true`**.
3. Add only the minimum named users to that role.
4. Prefer **not** granting full **Admin** solely for log review.
5. Re-verify with a non-Admin session: list `GET …/rest/auditlog/entries` must return **200**; without the property, **403**.
6. Record the grant in the change ticket (who, when, why). Viewer access events may themselves be dual-written (Phase 5 audit-of-audit slice).

### 4.4 Admin WebUI note

The Admin shell tools section remains **Admin-gated** in the UI chrome. Server REST still enforces Admin **or** the role property for non-UI clients (scripts, SIEM pullers, export).

---

## 5. Export permission

Export (CSV/JSON) uses the **same AuthZ** as query:

| Capability | AuthZ |
|------------|--------|
| `GET …/rest/auditlog/entries` (list) | Admin or `sys_securityAuditLogViewer` |
| `GET …/rest/auditlog/entries/{auditId}` (detail) | Admin or `sys_securityAuditLogViewer` |
| `GET …/rest/auditlog/export?format=csv\|json` (Phase 5) | **Same** — Admin or property |

Unauthorized callers receive **403**. Do not create a separate “export-only” privilege in product code without an explicit design change — the control matrix assumes **one** viewer capability for read + export.

Operator tips:

* Prefer filtered export over full-table dumps.
* Treat export files as **sensitive** (may contain usernames, IPs, paths after redaction of secrets).
* Store exports in controlled repositories; apply agency media-handling rules.
* Optional **row integrity hash** helper: see §7 and `com.intsof.percussioncms.auditlog.integrity.AuditIntegrityHash`.

---

## 6. AU control matrix (deployers)

Mapping is **product capability → control intent**. Deployers remain responsible for residual risk, policies, and evidence.

| Control (NIST SP 800-53 Rev. 5) | Intent (short) | Percussion CMS product support | Deployer residual / ops action |
|---------------------------------|----------------|--------------------------------|--------------------------------|
| **AU-2** Event logging | Define auditable events | Unified `SystemErrorCode` catalogs with **`isAuditable`**; dual-write only for auditable codes; login/security/content/workflow/path/design bridges | Maintain site-specific event inventory; enable `enableAuditLogging=true`; review catalog coverage for local extensions |
| **AU-3** Content of audit records | What fields are recorded | UUID id, UTC time, module/code, event type, outcome, actor, target, source IP/host, session hash, messages (redacted), correlation, attributes, server node | Do not strip columns in custom DB views used for evidence; keep schema aligned with product upgrades |
| **AU-3(1)** Additional content | Optional enrichment | `attributes` / `CORRELATION_ID` / `SERVER_NODE` available on write path | Standardize correlation headers at reverse proxy if multi-tier |
| **AU-4** Audit log storage capacity | Capacity planning | DB table + Log4j sinks; retention job bounds DB growth | Size disks/DB; monitor table growth; SIEM offload for long retention |
| **AU-5** Response to audit logging process failures | Fail-safe on log failure | Dual-write failures are logged; business requests do not hard-fail solely on sink failure (`DefaultAuditLogService`) | Alert on sink-failure loggers; monitor free disk; page on prolonged dual-write failure |
| **AU-6** Audit record review, analysis, reporting | Human/process review | REST query + Admin UI viewer (Phase 3/4); export CSV/JSON (Phase 5) | Assign auditor roles; define review cadence; ticket findings |
| **AU-7** Audit record reduction / report generation | Reports / filters | REST filters (`from`, `to`, `module`, `eventType`, `outcome`, `actor`, …); export formats | Use SIEM for heavy analytics; keep export caps in mind (`maxRows` on export API) |
| **AU-8** Time stamps | Trusted time | UTC instants on records | **NTP/chrony/Windows Time** on all hosts (§3) |
| **AU-9** Protection of audit information | Integrity / access control | Role property + Admin; 403 on deny; redaction of secrets in messages; DB OS/schema ACLs | Restrict DB accounts; protect backups; file permissions on `server.log`; no shared Admin passwords |
| **AU-9(4)** Access by subset of privileged users | Separation | Non-Admin role + `sys_securityAuditLogViewer` without full Admin | Prefer dedicated auditor role; quarterly access recertification |
| **AU-11** Audit record retention | Retention policy | `systemAuditLogRetentionDays` + `sys_systemAuditLogRetentionJob` (§2) | Set policy value; archive via export/SIEM before purge; document exceptions |
| **AU-12** Audit generation | Generate audit records | `AuditLogService` / `PSSystemAuditLogger` production paths | Keep `enableAuditLogging=true` in production; never disable for convenience without risk acceptance |

### 6.1 Controls not fully productized (explicit residuals)

| Topic | Status | Notes |
|-------|--------|-------|
| Cryptographic **signing** of audit rows / WORM media | Residual | Optional **SHA-256 integrity hash helper** only (§7) — not a HSM-backed signature or immutable store |
| Central SIEM correlation / continuous monitoring (AU-6 automated) | Residual | Ship Log4j / export to agency SIEM |
| Multi-RDBMS forensic matrix | Residual | Out of scope for this runbook slice |
| Production secrets / host-only config | Residual | Operator-owned |

---

## 7. Optional integrity hash (row digest)

**Class:** `com.intsof.percussioncms.auditlog.integrity.AuditIntegrityHash`  
**Module:** `modules/perc-auditlog`

Provides a **deterministic SHA-256 (lowercase hex)** over a canonical field ordering for an audit row (from `AuditRecord` or raw field strings). Intended for:

* Verifying that an **exported** row was not altered after download  
* Spot-checking that query results match a previously computed digest  

**Not** provided by this helper:

* Database column storage of the hash (schema change — future residual if required)  
* HMAC / digital signatures / HSM  
* Protection against a privileged actor who can rewrite both the row **and** recompute the hash  

### 7.1 Canonicalization (stable)

Fields joined with Unicode unit separator `U+001F`, UTF-8, in this order:

1. `auditId`  
2. `eventTime` — `Instant.toString()` (ISO-8601)  
3. `moduleCode`  
4. `messageCode` — decimal integer string  
5. `eventType`  
6. `outcome`  
7. `actor`  
8. `target`  
9. `sourceIp`  
10. `sourceHost`  
11. `sessionIdHash`  
12. `userMessage`  
13. `logMessage`  
14. `correlationId`  
15. `attributes` — for `AuditRecord`, keys sorted lexicographically, each `key=value` joined with `U+001E`  
16. `serverNode`  

Nulls become empty strings. Digest algorithm name constant: `SHA-256`.

### 7.2 Operator usage (sketch)

```java
import com.intsof.percussioncms.auditlog.AuditRecord;
import com.intsof.percussioncms.auditlog.integrity.AuditIntegrityHash;

String hex = AuditIntegrityHash.sha256Hex(record);
boolean ok = AuditIntegrityHash.matches(record, hex);
```

Export pipelines may call the raw-field overload when mapping REST DTOs without constructing a full `AuditRecord`.

Unit tests: `AuditIntegrityHashTest` (determinism, field sensitivity, null-as-empty, attribute key order stability).

---

## 8. Quick reference — REST (read path)

Base (typical install): `/Rhythmyx/rest/auditlog/…` (public REST; WebUI may call `/services/…` proxy equivalents).

| Method | Path | AuthZ | Notes |
|--------|------|-------|-------|
| GET | `/auditlog/entries` | Admin or property | Filters + pagination |
| GET | `/auditlog/entries/{auditId}` | Admin or property | Detail |
| GET | `/auditlog/export` | Admin or property | `format=csv\|json` (Phase 5 export) |

---

## 9. Related documents & issues

| Doc / issue | Role |
|-------------|------|
| [design.md](./design.md) | Architecture decisions, phase tracking |
| [export.md](./export.md) | Export API details (when present on branch / after #2715 merge) |
| `modules/perc-auditlog/README.md` | Developer usage |
| [#2618](https://github.com/intersoftdatalabs-in/percussioncms/issues/2618) | Role property + REST query |
| [#2619](https://github.com/intersoftdatalabs-in/percussioncms/issues/2619) | Admin UI + Playwright |
| [#2620](https://github.com/intersoftdatalabs-in/percussioncms/issues/2620) | Phase 5 hardening parent |
| [#2715](https://github.com/intersoftdatalabs-in/percussioncms/issues/2715) | Export CSV/JSON |
| [#2716](https://github.com/intersoftdatalabs-in/percussioncms/issues/2716) | Audit-of-audit access events |
| [#2717](https://github.com/intersoftdatalabs-in/percussioncms/issues/2717) | This runbook + integrity hash |

---

## 10. Change log

| Date | Change |
|------|--------|
| 2026-08-10 | Initial federal/ops runbook + integrity hash helper (#2717 / parent #2620) |
