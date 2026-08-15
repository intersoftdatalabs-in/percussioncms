# Erlang review: #3412 LocationScheme / Context / DeliveryType wire getters

**Date:** 2026-08-15  
**Branch:** `fix/issue-3412-location-scheme-wire-getters`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** approve

## Summary

Slice 4 of parent #3388 converts publishing-location REST wire DTOs
(`LocationScheme`, `LocationSchemeParameter`, `Context`, `DeliveryType`) from
`Optional<T>` getters to plain nullable getters with existing
`@JsonInclude(NON_NULL)`. sitemanage `ContextAdaptor` / `DeliveryTypeAdaptor`
callers that unwrapped those getters via `ApiUtils.orNull` / `flatMap` now use
the plain values. Guid `getStringValue()` remains Optional + `@JsonIgnore`
(out of this slice).

Production-mapper tests appended to `JacksonContextResolverOptionalTest` use
`new JacksonContextResolver().getContext(TheDto.class)` and assert JSON has no
`empty`/`present` Optional-bean keys, plus round-trip field equality.

Memory patterns hit: incomplete change-class closure (callers + production
mapper tests); behavioral tests required for wire-shape change.

## Issues

None (no bugs, no missing behavioral tests, no path/file I/O).

## Cross-platform path checklist

N/A — no filesystem path construction or path assertions.

## Change-class companions

| Companion | Status |
| --- | --- |
| Peer getter style (`ContentType` / PR #3406) | Done |
| `JacksonContextResolverOptionalTest` family methods | Appended (8 tests total) |
| rest / sitemanage callers of converted getters | Updated |
| `rest/AGENTS.md` wire-getter rule | Not committed (human rule review) |
| product-docs | N/A (no documented Optional-bean JSON example) |
| Playwright / WebUI | N/A |

## Build evidence

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 422, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1223, Failures: 0, Skipped: 125
- C2: public `Optional<T>` → `T` getters. Grep for anonymous `extends` / `new Type() {` — none. Reverse-dep sitemanage standalone install green.
