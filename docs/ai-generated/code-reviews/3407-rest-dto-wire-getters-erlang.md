# Erlang review — `fix/issue-3407-rest-dto-wire-getters`

**Reviewer:** Erlang Shen (independent; did not author this change)  
**Date:** 2026-08-15  
**Scope:** uncommitted vs `HEAD` on `fix/issue-3407-rest-dto-wire-getters`.  
**Memory patterns hit:** Optional wire getters drop fields / emit `empty`/`present` beans; public getter signature change requires reverse-dep compile.

## Summary

REST wire DTOs `DisplayFormatProperty`, `Template`, `TemplateBinding`, and the Page family (`Page`, `Widget`, `SeoInfo`, `Region`, `CalendarInfo`, `CodeInfo`, `WorkflowInfo`) returned `Optional<T>` from JSON getters. That produces Optional-bean keys (`empty`/`present`) or dropped catalog fields under `@JsonInclude(NON_NULL)` (same failure as ContentType #1693 / TemplateSummary #2189 / User-Role-ObjectSummary #3388).

This change converts those types to plain nullable getters/setters with `@JsonInclude(NON_NULL)` and updates callers that used `.orElse()` / `.ifPresent()` / `Optional.flatMap` (`PagesResource`, sitemanage `PageAdaptor` / `AssetAdaptor`). `ApiUtils.orEmpty(Collection)` was added so converted list getters compile without an Optional wrapper. `PageAdaptor.findWidget` still unwraps `PSWidgetItem.getName()` (`Optional`) when comparing to the now-plain REST `Widget.getName()`.

Production-mapper tests use `new JacksonContextResolver().getContext(TheDto.class)` and assert JSON has no Optional-bean keys.

`TemplateDetail` already used plain getters. Virtual Site / LocationScheme / Folder families were left for #3411–#3413. `rest/AGENTS.md` was not committed (human rule review).

## Recommendation

approve

## Gate

- **May commit/push: yes** (feature branch)
- Bugs: none remaining for this family
- Behavioral tests: JacksonContextResolverOptionalTest (9 tests, including new family round-trips)
- Playwright companion: N/A (no WebUI product screen change)
- Agent rule files: none
- Cross-platform: **N/A** (no path I/O)
- C2: public `Optional<T>` → `T` getters. Grep for `extends DisplayFormatProperty|Template|Page|Widget|SeoInfo|Region|CalendarInfo|CodeInfo|WorkflowInfo` and `new Type() {` — no anonymous subclasses of the REST types. sitemanage `Template` subclasses are `com.percussion.sitemanage.service.Template`, not the REST DTO. Standalone sitemanage clean install green.

## Tests

- `cd rest && ../mvnw.cmd clean install` — Tests run: 423, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — Tests run: 1223, Failures: 0, Skipped: 125
