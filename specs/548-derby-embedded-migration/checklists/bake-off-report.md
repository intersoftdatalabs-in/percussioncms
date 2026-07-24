# Engine bake-off report (#548 / QC-025)

**Feature**: `specs/548-derby-embedded-migration`  
**Issue**: [#548](https://github.com/intersoftdatalabs-in/percussioncms/issues/548)  
**Template task**: T006  
**Fill during**: T007–T011  
**Status**: **CLOSED** — H2 locked 2026-07-24  

## Decision

| Field | Value |
|-------|--------|
| **Locked engine** | **H2** |
| **Maven coordinates** | `com.h2database:h2:2.3.232` |
| **Lock date** | 2026-07-24 |
| **Locked by** | implementation session (T008 + T009) |
| **Bake-off status** | **Closed** — do not re-open H2 vs HSQL unless a hard production-shaped regression forces it |
| **GitHub #548 comments** | [Bake-off results](https://github.com/intersoftdatalabs-in/percussioncms/issues/548#issuecomment-5066264381); closed note follows |
| **Harness classes** | `PSH2DialectSmokeTest`, `PSH2MultiuserLockHarnessTest` |
| **Foundation PR** | https://github.com/intersoftdatalabs-in/percussioncms/pull/1493 |

## Environment

| Field | Value |
|-------|--------|
| OS | Linux (agent CI-style host) |
| JDK | 21 |
| Hibernate | 7.2.x (from parent POM) |
| Branch | `548-derby-embedded-migration` |
| Canonical JDBC URL template | See `contracts/repository-config.md` (file H2 / `DB_CLOSE_ON_EXIT=FALSE`; harness also uses `FILE_LOCK=NO` for same-JVM multi-conn) |

## Tests executed

| Test | Pass? | Notes |
|------|-------|-------|
| SessionFactory / dialect smoke (T008) | **PASS** | `PSH2DialectSmokeTest` 3/3; file H2 create/DML/`FOR UPDATE` |
| ≥10 concurrent editors — checkout-shaped SQL (T009, QC-006 partial) | **PASS** | Distinct-item checkouts 10/10; same-item exclusive 1 winner; object locks |
| Same-item exclusive edit contention | **PASS** | Exactly one winner; version=1; no lost update |
| CLOB body on concurrent edit | **PASS** | BODY CLOB updated under exclusive checkout |
| Object-lock contention | **PASS** | Distinct + same-object claim patterns |
| Boolean/BIT map | **PASS** (unit) | TableFactory H2 map uses `BOOLEAN` — runtime product soak is later work |
| Full CMS Spring checkout IT | **Deferred** | Task **T069** — GA multiuser gate, not engine selection |
| HSQLDB full bake-off | **N/A** | H2 passed; HSQL only if a later hard regression re-opens the decision |

## Metrics

| Metric | Value |
|--------|-------|
| Concurrent editors | 10 |
| Lost updates | 0 (asserted) |
| Corruptions | 0 (asserted) |
| Smoke tests | 3/3 PASS |
| Multiuser harness tests | 5/5 PASS |

## Outcome

- [x] **H2 locked** — proceed with US1+ as H2; bake-off **closed**
- [x] **HSQLDB locked** — **N/A** (not required; H2 passed multiuser/locking gate)
- [x] **Failed both** — **N/A** (H2 passed)

## Post-bake-off validation (not re-open criteria)

These remain on the implementation plan; they do **not** reopen H2 vs HSQL unless they prove H2 unfit:

| Item | Task / QC |
|------|-----------|
| Full CMS Spring checkout/workflow soak | T069 / residual QC-006 |
| DTS concurrent write smoke | T071 / SC-005 |
| Product boolean/identity under migrator | US2 / QC-003–004 |

## Notes

- File H2 multi-connection harness uses `FILE_LOCK=NO` (same JVM) and `LOCK_TIMEOUT=10000`.
- Product-shaped patterns: `SELECT … FOR UPDATE` checkout user column; object lock claim/release.
- Seed DDL must `commit()` under `autoCommit=false` (caught during harness debug).
- Seed strategy **A** locked: empty H2 + TableFactory/product load (`research.md` R11, `contracts/repository-config.md`).


## US4 multiuser evidence (T069–T074)

| Gate | Result | Evidence |
|------|--------|----------|
| ≥10 editor distinct checkout | PASS (automated) | `PSH2MultiuserLockHarnessTest` |
| Same-item exclusive checkout | PASS | same class |
| DTS concurrent write smoke | PASS | `PSH2DtsConcurrentWriteSmokeTest` + `checklists/dts-concurrency.md` |
| Custom H2 dialect | N/A | stock `H2Dialect` sufficient |
| Pool/isolation retune | N/A | not required by harness |
