# Erlang review: #3419 Community family plain wire getters

**Date:** 2026-08-15  
**Branch:** `fix/issue-3419-community-wire-getters`  
**Scope:** uncommitted Community / CommunityRole / CommunityVisibility Optional→nullable conversion vs `origin/main`  
**Memory patterns hit:** incomplete change-class closure; missing behavioral tests; agent rule files without human review; focused `-Dtest` vs module suite

## Summary

Slice 7 of parent #3388 converts the Community REST wire DTO family from `Optional<T>` getters to plain nullable getters under `@JsonInclude(NON_NULL)`, matching `ContentType` / sibling Asset family (#3418). Callers in `CommunityAdaptor` and `ApiUtils` that used `.orElse()` / `Optional.isEmpty()` are updated. Production-mapper round-trip tests were appended to `JacksonContextResolverOptionalTest` (not rewritten). `rest/AGENTS.md` was not committed.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None.

### Change-class closure

- Production DTOs: `Community`, `CommunityRole`, `CommunityVisibility` — plain getters, `@JsonInclude(NON_NULL)`, Optional helpers deleted (not left as `@JsonIgnore`).
- Tests: `JacksonContextResolverOptionalTest` uses `new JacksonContextResolver().getContext(TheDto.class)`; asserts no `"empty"`/`"present"` keys; round-trips name/roleName/guid.
- Callers: `CommunityResourceDetailTest`, `CommunityAdaptor`, `ApiUtils` compile against new signatures.
- Existing `CommunitiesTestAdaptor` Spring stub unchanged (no new adaptor interface).
- No WebUI / Playwright (C5 N/A).
- Product-docs N/A — no documented Optional-bean JSON examples.
- `rest/AGENTS.md` left uncommitted (human rule-review gate).

### Cross-platform path checklist

N/A — no filesystem path / I/O changes.

### C2 API shape

Public getter return types changed (`Optional<T>` → `T`). Grep found no `extends Community` / anonymous subclasses. Reverse-dep `projects/sitemanage` standalone `clean install` is green.

## Build evidence

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 421, Failures: 0, Errors: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1223, Failures: 0, Errors: 0, Skipped: 125
