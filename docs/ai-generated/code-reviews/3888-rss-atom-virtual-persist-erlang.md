# Erlang review: #3888 REST rss-atom virtual.sourceKind persist

**Branch:** `fix/issue-3888-rss-atom-virtual-persist`  
**Base:** `origin/main`  
**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-27  
**Memory patterns hit:** change-class closure (rest resource + adaptor tests + product-docs); credential/secret leak in errors; portable Path validation; exhaustive enum switch

## Summary

REST GET/PUT `/sites/{nameOrId}/virtual` now round-trips `virtual.sourceKind=rss-atom` with a portable-safe local `rootPath`. Leftover `virtual.remoteUrl`, credential-like properties, and cloud URL `rootPath` values fail closed with **400**. Unknown kinds remain **400**. Developer Sites chrome and REST Build/Preview/Publish for rss-atom are intentionally not added (#3889).

SPI allow-list was not on `main` yet (cluster #3892). This slice adds `VirtualSiteSourceType.RSS_ATOM` so helper validation can persist the kind. Factory `create(RSS_ATOM)` throws `IllegalArgumentException` until SPI assemble lands — persist does not call `create()`.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Persist validation uses NIO `Path.of(...).normalize()` / `isSafeRootPath` (existing helper)
- [x] Tests use property strings (`C:/rss-docs`, `../outside`, `https://…`) not OS file joins
- [x] Cloud URL rejection does not treat Windows drive letters as URI schemes (`colon > 1`)
- [x] No Unix-only `/tmp` roots or line-ending-sensitive assertions

## Issues

None (hard-gate).

### Suggestion (non-blocking)

Factory `case RSS_ATOM` throws rather than constructing `PSRssAtomVirtualSiteSource`. That is correct for this persist-only slice; SPI assemble is cluster #3892 / #3881. REST Build of an rss-atom site would surface as 500 until SPI/factory wiring lands — out of scope (#3889 / later Build slices). Callers of `createFromWireName("rss-atom")` should expect that until SPI merge.

## Tests / companions

- Helper: allow-list includes `rss-atom`; safe root; reject remoteUrl / `..` / cloud URL / credentials (secret not echoed)
- Adaptor: PUT/GET round-trip; 400 paths
- Resource: GET/PUT round-trip; OpenAPI PUT mentions `rss-atom` local/loopback
- Jackson wrap round-trip of `rss-atom`
- product-docs 8.2 REST / Virtual Sites / admin Sites / site-config
- No WebUI / Playwright (not a screen change)

## Builds

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 2492, Failures: 0, Errors: 0, Skipped: 245
- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 645, Failures: 0, Errors: 0, Skipped: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1633, Failures: 0, Errors: 0, Skipped: 125
