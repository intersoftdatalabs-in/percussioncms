# Erlang review — issue #3265 services.* remaining Xlint

**Date:** 2026-08-12  
**Branch:** `fix/issue-3265-services-xlint`  
**Scope:** uncommitted vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** prefer real generics over `@SuppressWarnings`; type Hibernate `Query`/`MutationQuery`; same-module behavioral tests for HQL helpers.

## Summary

PR-sized residual of epic #2022 after #3210 (contentmgr/legacy). Types remaining `com.percussion.services.*` Hibernate selects as `Query<T>` and deletes/updates as `MutationQuery` in schedule, workflow, relationship, filestorage, linkmanagement, useritems, siteimport, system, contentchange, pubserver, sitemgr, utils.orm, audit, and purge. No public method/ctor signatures changed. `deleteTaskLogsByDate` now calls `executeUpdate()` (previously bound `:endTime` and never executed — method was a no-op).

## Issues

None blocking.

## Tests

`PSServicesRemainingTypedTest` (11) covers extracted HQL constants and the site-optional content-change delete helper. Companion Mockito test `PSSystemServicePhase4d1bWritesTest` updated to stub/verify `createMutationQuery`.

## Re-review

Mock companion for `PSSystemService` write methods now matches `MutationQuery`. No remaining gate issues.

## Cross-platform path checklist

N/A — no path/file I/O.

## Change-class companions

Same class as #3210/#3188: typed Hibernate queries + unit tests of HQL helpers. Product-docs N/A (tech-debt). No UI.

## Residual

Assembly (`PSNavHelper` / assemblers / jexl map-cast suppressions) and leftover native SQL in relationship/purge remain for a follow-up issue.
