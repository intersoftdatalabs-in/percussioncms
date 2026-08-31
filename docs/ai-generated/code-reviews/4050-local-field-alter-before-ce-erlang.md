# Erlang review — #4050 REST local-field ALTER before CE re-init

**Date:** 2026-08-31  
**Branch:** `fix/issue-4050-local-field-alter-before-ce`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for new logic; do not stub past the persist/ALTER; identifier-only SQL (no OS path joins); change-class companions (adaptor + H2 schema persist + product-docs)

## Summary

`ContentTypeAdaptor.addLocalField` now creates the backend column (`ALTER TABLE … ADD COLUMN`) before `IPSContentDesignWs.saveContentTypes` re-inits the content editor application. Duplicate 409 and unlocked 409 still skip ALTER and save. H2 mem tests actually execute ALTER (not stubbed).

## Issues

None (hard-gate).

### Notes (non-blocking)

- JDBC helper is a sibling of unmerged `JdbcSystemDefColumnSchema` (#4037 / PR #4049). Unify after that PR lands if both stay.
- Production DDL is explicit ALTER with identifier allow-list (letter/digit/underscore + reserved-word reject), not `PSJdbcTableFactory.processTable`. Connection still comes from table-factory `PSJdbcDbmsDef` / `PSJdbcTableFactory.getConnection`. Matches the #4037 peer; avoids `create=true` drop/recreate.

## Cross-platform path checklist

- No filesystem path concatenation. JDBC identifiers only.
- H2 URLs are in-memory (`jdbc:h2:mem:…`), not OS temp paths.
- `PSSqlHelper.qualifyTableName` with schema fallback; no `/` or `\\` joins.

## Tests

- `ContentTypeAdaptorLocalFieldTest`: InOrder ALTER then `saveContentTypes`; 409 duplicate/unlocked never ALTER; ALTER failure never save.
- `JdbcContentTypeLocalFieldColumnSchemaTest`: H2 create + idempotent; case-insensitive metadata; unsafe ident reject.
- Standalone `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1994, Failures: 0, Skipped: 125.
