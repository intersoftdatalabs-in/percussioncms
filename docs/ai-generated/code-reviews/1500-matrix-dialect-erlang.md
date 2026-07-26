# Erlang review — 1500 matrix dialect follow-up

**Date:** 2026-07-26  
**Scope:** Uncommitted working tree on `1500-matrix-smoke-followup` (dialect + matrix SSL/collation)  
**Base:** `HEAD` / commits already on branch vs `origin/1500-matrix-smoke-followup`

## Summary

PostgreSQL/MySQL/SQL Server matrix install-smoke greens required several dialect and harness fixes: Quartz flag columns as JDBC BIT, PG function defs, metadata identifier folding, Hibernate LONGVARCHAR for config CLOB/TEXT, matrix SSL disabled for compose DBs, MySQL connection collation + view CAST COLLATE.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Cross-platform path checklist

- No new filesystem path concatenation with hardcoded separators in product code.
- Matrix entrypoint continues to force POSIX paths for `java -jar` argv (tested).
- Unit tests use mocks / relative config discovery, not Unix-only absolute assertions for product I/O.

## Memory patterns hit

- Behavioral tests required for non-trivial logic — fold helpers, BIT maps, function defs, PSConfig mapping, MYSQL_CONN_PARAMS, entrypoint SSL flags covered.
- Installer silent path / property wiring — SSL and MySQL URL params fixed in harness + resolver.

## Issues

None hard-gate.

### Suggestions (non-blocking)

1. **SQL Server compose healthcheck** still reports `unhealthy` (sqlcmd needs `-C`); matrix smoke still passes. Follow-up compose fix optional.
2. **`toLowerCase()` / `toUpperCase()`** without `Locale.ROOT` in `foldStoredIdentifier` — consistent with existing TableFactory folding; fine for SQL identifiers.

## Evidence

- Unit tests: TableFactory BIT maps, system fold/function/PSConfig tests, utils MYSQL_CONN_PARAMS, pytest matrix harness (13).
- Matrix Layer-1: cms-h2 (prior), cms-postgresql, cms-mysql, cms-sqlserver — HTTP 200.

## Re-review

N/A — first review of this delta.
