# Erlang Review — 004/us3-t042-java-sql-injection

**Date**: 2026-07-17  
**Reviewer**: Erlang (strict independent pre-PR)  
**Scope**: Uncommitted T042 java/sql-injection cluster vs `origin/development`

## Summary

Closes the six open `java/sql-injection` alerts (#656–#661) with structural defenses:

| Alert | Site | Fix |
|-------|------|-----|
| #656 | `PSMetadataQueryService` | Parameterize `prop.name`; allowlist `ORDER BY me.<col>`; validate tokens |
| #657 | `PSJdbcResultSetIteratorStep` | Reject multi-statement / comment SQL before `executeQuery` |
| #658 | `PSJdbcTableFactory.hasRows` | `requireSqlObjectName` on table/schema/db before `SELECT COUNT(*)` |
| #659/#660 | `PSJdbcTableMetaData` | Validate table/schema/db identifiers in ctor before JDBC metadata |
| #661 | `PSSQLStatement` | Same multi-statement guard on `executeQuery` / `executeUpdate` |

Shared barriers live in `SecureStringUtils` with a CodeQL model pack row (`sql-object-name.model.yml`).

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Cross-platform path checklist

N/A (SQL identifiers only; no filesystem paths).

## Issues

### Issue 1 — Severity: suggestion
- Description: `requireSingleSqlStatement` is a coarse filter (not a full SQL parser). Legitimate factory SQL must not embed `;` or `--`/`/*`. Existing generated SQL is SELECT-style without comments.
- Suggestion: Accept for T042; expand only if a real factory statement fails closed.

## Tests

- `SecureStringUtilsSqlInjectionTest`
- `PSSQLStatementSqlInjectionTest`
- `PSJdbcResultSetIteratorStepSqlInjectionTest`

## Handoff

Safe to commit and open PR against `development`.
