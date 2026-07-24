# Engine bake-off report (#548 / QC-025)

**Feature**: `specs/548-derby-embedded-migration`  
**Issue**: [#548](https://github.com/intersoftdatalabs-in/percussioncms/issues/548)  
**Template task**: T006  
**Fill during**: T007–T011  

## Decision

| Field | Value |
|-------|--------|
| **Locked engine** | **H2** (smoke + ≥10-editor product-shaped lock harness PASS; full CMS Spring checkout IT remains T069) |
| **Maven coordinates** | `com.h2database:h2:2.3.232` |
| **Lock date** | 2026-07-24 |
| **Locked by** | implementation session (T008 + T009) |
| **GitHub #548 comment** | _pending post_ |
| **Harness class** | `com.percussion.services.datasource.PSH2MultiuserLockHarnessTest` |

## Environment

| Field | Value |
|-------|--------|
| OS | |
| JDK | 21 |
| Hibernate | 7.2.x (from parent POM) |
| Branch | `548-derby-embedded-migration` |
| Canonical JDBC URL template | _paste from repository-config.md_ |

## Tests executed

| Test | Pass? | Notes |
|------|-------|-------|
| SessionFactory / dialect smoke (T008) | **PASS** | `PSH2DialectSmokeTest` 3/3; file H2 create/DML/FOR UPDATE |
| ≥10 concurrent editors — checkout-shaped SQL (T009, QC-006) | **PASS** | Distinct-item checkouts 10/10; same-item exclusive 1 winner; object locks |
| Same-item exclusive edit contention | **PASS** | Exactly one winner; version=1; no lost update |
| CLOB body on concurrent edit | **PASS** | BODY CLOB updated under exclusive checkout |
| Object-lock contention | **PASS** | Distinct + same-object claim patterns |
| Boolean/BIT smoke | pending | H2 map uses BOOLEAN (not Derby CHAR T/F) — covered by TableFactory unit map |
| Full CMS Spring checkout IT | pending | T069 |
| HSQLDB rerun (only if H2 failed) | N/A | |

## Metrics

| Metric | Value |
|--------|-------|
| Concurrent editors | 10 |
| Lost updates | 0 (asserted) |
| Corruptions | 0 (asserted) |
| Lock test classes | `PSH2DialectSmokeTest`, `PSH2MultiuserLockHarnessTest` |

## Outcome

- [x] **H2 locked** — proceed with US1+ as H2  
- [ ] **HSQLDB locked** — update `tasks.md` engine names/paths in same change set (T011)  
- [ ] **Failed both** — stop and escalate on #548  

## Notes

- File H2 multi-connection harness uses `FILE_LOCK=NO` (same JVM) and `LOCK_TIMEOUT=10000`.
- Product-shaped patterns: `SELECT … FOR UPDATE` checkout user column; object lock claim/release.
- Seed DDL must `commit()` under `autoCommit=false` (caught during harness debug).
- Full in-process CMS checkout/workflow API soak remains **T069**.
