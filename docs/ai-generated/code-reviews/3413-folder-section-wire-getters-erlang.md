# Erlang review — #3413 Folder / Section / path-request wire getters

**Branch:** `fix/issue-3413-folder-section-wire-getters`  
**Scope:** uncommitted vs `HEAD` + commits not in `origin/main`  
**Date:** 2026-08-15  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** REST DTO `Optional` getters serialize as empty/present beans or drop fields under `@JsonInclude(NON_NULL)` (ContentType #1693 / TemplateSummary #2189 / sibling #3412).

## Summary

Converts Explorer folder/section REST wire DTOs from `Optional<T>` getters to plain nullable getters with `@JsonInclude(NON_NULL)`, matching `ContentType` / LocationScheme (#3412) peer style. Updates `FoldersResource` and `FolderAdaptor` callers that used `.orElse()` / `.isPresent()` / `ApiUtils.orNull` / `ApiUtils.orEmpty` on converted getters. Appends production-mapper round-trip tests to `JacksonContextResolverOptionalTest` (does not rewrite existing methods).

## Issues

None (gate-blocking).

## Notes (non-blocking)

- `LinkRef` (parent of `SectionLinkRef`) still returns `Optional` for `name`/`href` — out of scope; nested landing-page JSON is asserted not to emit empty/present keys under the production mapper.
- `rest/AGENTS.md` wire-getter convention remains uncommitted (human rule review).
- Cross-platform path checklist: **N/A** (no filesystem I/O).

## Builds

- `cd rest && ../mvnw.cmd clean install` → BUILD SUCCESS, Tests run: 423, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` → BUILD SUCCESS, Tests run: 1223, Failures: 0, Skipped: 125
