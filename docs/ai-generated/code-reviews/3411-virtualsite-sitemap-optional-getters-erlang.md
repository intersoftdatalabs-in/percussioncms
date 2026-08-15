# Erlang review — `fix/issue-3411-virtualsite-sitemap-optional-getters`

**Reviewer:** Erlang Shen (independent; did not author this change)
**Date:** 2026-08-15
**Scope:** uncommitted vs `HEAD` on `fix/issue-3411-virtualsite-sitemap-optional-getters` (slice of #3388).
**Memory patterns hit:** incomplete change-class closure; production-mapper tests vs bare `JsonMapper`; agent rule files require human review.

## Summary

Convert Virtual Site + SiteMap REST wire DTO getters from `Optional<T>` to plain nullable types so Jackson emits scalars, not Optional-bean `empty`/`present` keys.

In scope: `VirtualSiteBuildRequest`, `VirtualSiteBuildResult`, `VirtualSitePreviewStatus`, `VirtualSitePublishResult`, `SiteMapOptions`. Callers in `rest` tests and `SitesAdaptor` / `SitesAdaptorTest` updated. Family methods appended to `JacksonContextResolverOptionalTest` using `new JacksonContextResolver().getContext(TheDto.class)`. `rest/AGENTS.md` not committed.

## Recommendation

approve

## Gate

- **May commit/push: yes** (feature branch)
- Bugs: none
- Behavioral tests: production-mapper round-trips + no `empty`/`present` keys for all five types; rest/sitemanage caller tests updated
- Agent rule files: none (rest/AGENTS.md not touched)
- Cross-platform: **pass** — no new filesystem path construction; existing `Path.of` / `blankToNull` path remains after getter conversion. Test JSON examples use `/` in string values (URL/JSON form, not OS joins)
- Change-class companions: DTO getters + rest callers + sitemanage adaptor + production-mapper tests. No new adaptor interface (no MainTest stub). DisplayFormat/Template/Page, LocationScheme/Context/DeliveryType, Folder/Section left for sibling slices
- C2: public getter signatures changed (`Optional<T>` → `T`). Grep found no `extends` / anonymous subclasses of the five types

## Issues

None.

## Tests

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 423, Failures: 0 (`JacksonContextResolverOptionalTest` 9 tests)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1223, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 36 tests)
