# Erlang review: #2245 Oracle schema vs UID in TableFactory/publishing

**Branch:** `fix/issue-2245-oracle-schema-vs-uid`  
**Date:** 2026-08-07  
**Reviewer persona:** Erlang (strict independent pre-commit)  
**Gate:** **approve** — May commit/push: **yes**

## Summary

Minimal fix at the inventory choke point `PSDatabaseDeliveryHandler.getDbmsInfoFromPubServer`: Oracle TableFactory origin now prefers the **connect user** when non-blank, so configs with `schema=ORAPROD` / `userid=SYSTEM` no longer qualify DDL as `ORAPROD.PERC_EXPORT_PAGE` (ORA-01918 when that Oracle user is absent). MSSQL `owner` path unchanged. Pure helper + 10 behavioral unit tests without live Oracle.

## Scope

| Path | Change |
|------|--------|
| `system/business/.../PSDatabaseDeliveryHandler.java` | `resolveDbmsOrigin` + wire into `getDbmsInfoFromPubServer` |
| `system/src/test/.../PSDatabaseDeliveryHandlerOracleOriginTest.java` | New JUnit 5 tests (Oracle ≠ UID, default schema, MSSQL owner, qualifyTableName) |

Cross-platform path review: N/A (no path/file I/O).  
Prior memory: inventory #2244 / PR #2253 recommended this surface.

## Recommendation

Approve. Residual live Oracle matrix remains #2246 (intentional cross-schema when schema user exists is a documented trade-off of this fix).

## Gate

| Check | Result |
|-------|--------|
| Bugs | None found |
| Behavioral unit tests for new logic | Present (`resolveDbmsOrigin` + property-bag + qualify) |
| Non-portable paths | None |
| Module `mvnw clean install` | `system` BUILD SUCCESS |
| Change-class companions | Tests only; no Spring bean / REST surface |

## Issues

None (severity bug/suggestion/nit empty).

## Notes for residual #2246

When operators intentionally connect as DBA (`SYSTEM`) and create objects in an existing Oracle schema user (`ORAPROD`), this fix creates objects under the connect user schema instead. Live smoke should confirm whether that matches customer intent or needs a follow-up policy (e.g. optional “force schema origin” flag).
