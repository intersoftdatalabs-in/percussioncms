# Erlang review — #3686 REST csv-filesystem virtual.sourceKind

**Branch:** `feat/issue-3686-csv-filesystem-sourcekind`  
**Date:** 2026-08-20  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (rest resource + adaptor tests + helper allow-list + product-docs); Jackson WRAP/UNWRAP_ROOT_VALUE envelope; portable NIO `Path` / no `..` after normalize; fail-closed unknown kinds.

## Summary

Parent #2678 slice 2. Site REST GET/PUT accepts `virtual.sourceKind=csv-filesystem` (peer of git-filesystem #2955). `VirtualSiteSourceType.CSV_FILESYSTEM` is allow-listed via `PSVirtualSiteHelper`; `rootPath` still uses NIO normalize and rejects remaining `..`. GET after PUT round-trips the kind. Unknown kinds and CSV + `remoteUrl` remain 400. In-product `POST …/virtual/build` stays git-filesystem only (SPI #3685). No WebUI in this slice (#3687).

## Issues

None.

## Cross-platform path checklist

- [x] No new `".../" +` filesystem joins in production Java
- [x] Validation uses existing `Path.of(…).normalize()` + `isSafeRootPath`
- [x] Tests use `../outside` (portable) and `C:/csv-docs` as operator-style examples matching git-filesystem peers
- [x] REST/JSON envelopes use `/` (URL form)

## Tests

- `PSVirtualSiteHelperTest` — allow-list `git-filesystem, csv-filesystem`; CSV safe root; reject `..`; reject remoteUrl; unknown `sql-api` still lists both kinds (30 tests, 1 OS-skipped)
- `SitesAdaptorTest` — PUT persist + GET round-trip; reject parent path; reject remote; Build 400 for csv-filesystem
- `SitesResourceTest` / `VirtualSitePropertiesSerialDeserialTest` — resource GET/PUT and Jackson envelope for `csv-filesystem`

## Change-class closure

| Companion | Status |
|-----------|--------|
| Enum allow-list (`VirtualSiteSourceType`) | yes |
| Helper validate | yes |
| SitesAdaptor persist (existing PUT + helper) | yes (no extra adaptor branch) |
| rest OpenAPI / DTO docs | yes |
| rest + sitemanage tests | yes |
| product-docs 8.2 admin Sites / REST / virtual-sites / site-config | yes |
| WebUI / Playwright | N/A (slice #3687) |
| CSV parser SPI | N/A (slice #3685) |
