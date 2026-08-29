# Erlang review: #3982 REST icalendar virtual.sourceKind persist

**Branch:** `fix/issue-3982-icalendar-virtual-persist`  
**Base:** `origin/main` (stacked on SPI #3986 / PR #3987)  
**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-29  
**Memory patterns hit:** change-class closure (rest resource + Spring stub javadoc + adaptor tests + product-docs); credential/secret leak in errors; portable Path validation; exhaustive enum switch

## Summary

REST GET/PUT `/sites/{nameOrId}/virtual` round-trips `virtual.sourceKind=icalendar` with a portable-safe local `rootPath`. Leftover `virtual.remoteUrl`, credential-like properties, and cloud URL `rootPath` values fail closed with **400**. Unknown kinds remain **400**. git/csv/sql/http-json/object-storage/rss-atom persist unchanged. Developer Sites chrome and REST Build/Preview/Publish for icalendar are intentionally not added (slice 3).

SPI #3986 already allow-lists `VirtualSiteSourceType.ICALENDAR` and `PSVirtualSiteHelper.requiresLocalOnlyRoot`. This slice stacks on that branch and wires REST OpenAPI + adaptor persist tests + product-docs.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Persist validation uses NIO `Path.of(...).normalize()` / `isSafeRootPath` (existing helper)
- [x] Tests use property strings (`C:/cal-docs`, `../outside`, `https://…`) not OS file joins
- [x] Cloud URL rejection does not treat Windows drive letters as URI schemes (`colon > 1`)
- [x] No Unix-only `/tmp` roots or line-ending-sensitive assertions

## Issues

None (hard-gate).

### Suggestion (non-blocking)

Factory `case ICALENDAR` constructs `PSIcalendarVirtualSiteSource` (SPI assemble already on this branch). Persist still does not call `create()`. Developer Sites chrome and REST Build/Preview/Publish for icalendar remain out of scope (slice 3).

## Tests / companions

- Helper (SPI): allow-list includes `icalendar`; safe root; reject remoteUrl / `..` / cloud URL / credentials (secret not echoed)
- Adaptor: PUT/GET round-trip; 400 paths (parent `rootPath`, remoteUrl, cloud URL, credentials)
- Resource: GET/PUT round-trip; OpenAPI PUT mentions `icalendar` local fixture / no CalDAV
- Jackson wrap round-trip of `icalendar`
- Spring test stub `SitesTestAdaptor` javadoc includes `icalendar` (existing bean; no new adaptor interface)
- product-docs 8.2 REST / Virtual Sites / admin Sites / site-config
- No WebUI / Playwright (not a screen change)

## Builds

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 2608, Failures: 0, Errors: 0, Skipped: 247
- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 759, Failures: 0, Errors: 0, Skipped: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1789, Failures: 0, Errors: 0, Skipped: 125
