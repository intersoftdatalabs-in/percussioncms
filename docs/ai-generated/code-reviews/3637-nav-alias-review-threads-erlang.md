# Erlang review — #3637 Kilo nav-alias review threads

**Branch:** `cluster/night-issue-20260820-nav-architecture-explorer`  
**Base:** `origin/main`  
**Date:** 2026-08-20  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; restore statics in `@AfterEach`; exact mock type

## Summary

Follow-up for three unresolved Kilo threads on PR #3637:

1. Drop dead `aliasId == null` after `ConcurrentHashMap.computeIfAbsent` whose mapping returns `NO_NAV_ALIAS` instead of null.
2. Sort registered type ids (nulls last, natural order) so same-role perc/rff alias selection is deterministic.
3. Add a positive-path cache test: stub `PSTypeConfiguration` at percNavTree 1017, assert rffNavTree 315 memoizes 1017, and a second lookup uses the cached id.

Package-visible test hook `putTypeConfigurationForTest` mutates `ms_configuration`; `@AfterEach` removes 1017 and clears the alias cache.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests cover lowest-id selection on unordered `HashSet`s and positive alias-id memoization (mock is exact `PSTypeConfiguration`). Module `cd system && ../mvnw.cmd clean install`: **BUILD SUCCESS**, Tests run: 2278, Failures: 0, Errors: 0, Skipped: 241. `PSNavNameAliasesTest` 9, `PSServicesContentmgrTypedTest` 10.

Cross-platform path checklist: **clean** — no filesystem path construction.

Javadoc plugin errors in perc-system are pre-existing (unrelated `@link` / HTML) and did not fail the install.

## Issues

None (blocking).
