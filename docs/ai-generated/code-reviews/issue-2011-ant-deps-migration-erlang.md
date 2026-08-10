# Erlang review — issue #2011 (ant.deps nested migration)

**Branch:** `fix/issue-2011-ant-deps-migration`  
**Scope:** uncommitted / feature diff vs `origin/main`  
**Date:** 2026-08-05  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Nested installer script `migration_i18n_locales.xml` failed at project-level
`taskdef classpathref="ant.deps"` with **Reference ant.deps not found** because
Ant applies parent `inheritRefs` only *after* the child project is configured.
Fix redefines a local `<path id="ant.deps">` from inherited `${install.dir}` /
`${install.src}` properties (available via `inheritAll` during configure).

## Cross-platform path checklist

- [x] No new hardcoded OS filesystem separators in Java
- [x] Tests use `Path.of(...)` relative segments only
- [x] ANT path construction uses Ant `${prop}/...` (installer convention; not Java string concat)
- [x] No Unix-only absolute path assertions

## Issues

None (bug / missing behavioral tests / non-portable I/O).

### Notes (non-blocking)

- Wiring tests are static XML assertions (appropriate; full installer not required per issue triage).
- `onerror="ignore"` on taskdef remains; if the class fails to load, target fails later with unknown task — same as pre-fix behavior for class-load issues.
- Out-of-scope Spotless monorepo hits discarded; only in-scope module files ship.

## Tests / gates evidence

- `cd modules/perc-distribution-tree && ../../mvnw clean install` — BUILD SUCCESS
- `I18nLocaleMigrationWiringTest` — tests=3, failures=0, errors=0
- Module `spotless:check` — BUILD SUCCESS (in-scope only)

