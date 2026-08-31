# Erlang review — issue #4037 H2 system-def PUT/add/delete

**Scope:** uncommitted `fix/issue-4037-h2-systemdef-put-add-delete` vs `HEAD`.
**Recommendation:** approve
**Gate:** May commit/push: yes
**Memory patterns hit:** missing behavioral tests for new/changed non-trivial logic; non-portable path/file I/O (not applicable — JDBC identifiers only)

## Summary

H2 system-def REST writes: empty PUT no longer rewrites `ContentEditorSystemDef.xml` (stops post-save NPE); POST creates `CONTENTSTATUS` columns; DELETE drops when present and still saves XML if the column is missing; load retries once on H2 `no such column` so duplicate `sys_title` stays 409.

## Issues

None. Column identifiers are letter/digit/underscore only (`requireIdent` / existing field-name validator). Connections are closed in `finally`. Adaptor unit tests cover empty PUT skip-save, PUT-after-first-save, missing-column delete, duplicate-after-poisoned-load 409. In-memory H2 tests cover add/idempotent add and missing drop. Playwright REST surface passed on qa-up H2.

## Cross-platform path checklist

N/A for filesystem paths. DDL uses validated SQL identifiers and `PSSqlHelper.qualifyTableName` with a schema-qualified fallback (not OS separators).

## Evidence

- `cd projects/sitemanage && ../../mvnw.cmd clean install` → BUILD SUCCESS. Tests run: 1997, Failures: 0, Skipped: 125.
- `cd rest && ../mvnw.cmd clean install` → BUILD SUCCESS. Tests run: 893, Failures: 0.
- Playwright: `npm run test:surface -- --path tests/developer-system-def-writes.spec.js` — 1 passed. console-clean=yes (request API). server.log-clean=yes (WARN retry only; no ERROR/FATAL).
