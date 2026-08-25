# Erlang review — #3795 REST http-json virtual.sourceKind

**Branch:** `feat/issue-3795-http-json-sourcekind`  
**Date:** 2026-08-25  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (rest resource + Spring stub + sitemanage adaptor tests + helper allow-list + product-docs); Jackson WRAP/UNWRAP_ROOT_VALUE envelope; portable NIO `Path` / no `..` after normalize; fail-closed unknown kinds; REST persist can land before SPI assemble (#3794).

## Summary

Parent #2678 slice 2 (REST) after sql-database REST. Site REST GET/PUT accepts `virtual.sourceKind=http-json` (peer of csv-filesystem #3686 / sql-database #3734). `VirtualSiteSourceType.HTTP_JSON` is allow-listed via `PSVirtualSiteHelper`; `rootPath` still uses NIO normalize and rejects remaining `..`. GET after PUT round-trips the kind. Unknown kinds and `http-json` + `remoteUrl` remain 400 (no secrets on the envelope). Assemble factory throws until SPI #3794 / PR #3798 supplies `PSHttpJsonVirtualSiteSource`. No WebUI in this slice (#3796).

## Issues

None.

## Cross-platform path checklist

- [x] No new `".../" +` filesystem joins in production Java
- [x] Validation uses existing `Path.of(…).normalize()` + `isSafeRootPath`
- [x] Tests use `../outside` (portable) and `C:/http-docs` as operator-style examples matching git/csv/sql peers
- [x] REST/JSON envelopes use `/` (URL form)

## Tests

- `PSVirtualSiteHelperTest` — allow-list `git-filesystem, csv-filesystem, sql-database, http-json`; HTTP JSON safe root; reject `..`; reject remoteUrl; unknown `sql-api` still lists all four kinds
- `SitesAdaptorTest` — PUT persist + GET round-trip; reject parent path; reject remote (including userinfo)
- `SitesResourceTest` / `VirtualSitePropertiesSerialDeserialTest` / `SitesTestAdaptor` — resource GET/PUT, Jackson envelope, Spring stub comment for `http-json`

## Change-class closure

| Companion | Status |
|-----------|--------|
| Enum allow-list (`VirtualSiteSourceType`) | yes |
| Helper validate | yes (enum.values(); remoteUrl fail-closed for non-git) |
| SitesAdaptor persist (existing PUT + helper) | yes (no extra adaptor branch) |
| rest OpenAPI / DTO docs | yes |
| rest + sitemanage tests | yes |
| Spring test stub adaptor | yes (existing `SitesTestAdaptor` echoes envelope) |
| product-docs 8.2 admin Sites / REST / virtual-sites / site-config | yes |
| WebUI / Playwright | N/A (slice #3796) |
| HTTP JSON parser SPI | N/A (slice #3794) |

## Notes

- Factory `case HTTP_JSON` throws `IllegalArgumentException` so the exhaustive enum switch compiles without shipping SPI internals. Sibling #3798 replaces this with `new PSHttpJsonVirtualSiteSource()`.
- `ContentTypeAdaptorWorkflowsTest` fails on origin/main without this diff (stash-verified). Not in scope.
