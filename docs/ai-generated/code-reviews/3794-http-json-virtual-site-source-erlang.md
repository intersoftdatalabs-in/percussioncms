# Erlang review: #3794 http-json IPSVirtualSiteSource

**Date:** 2026-08-25  
**Branch:** `feat/issue-3794-http-json-virtual-site-source`  
**Scope:** uncommitted vs `HEAD` / `origin/main` (system virtualsite SPI + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection (SSRF, userinfo, redirects, unsafe catalog paths); change-class companions (SPI + factory + tests + product-docs); no REST/UI companions required (explicitly out of scope #3795/#3796); no secrets in URL/logs.

## Summary

Adds `VirtualSiteSourceType.HTTP_JSON` (`http-json`), `PSHttpJsonVirtualSiteSource` (discover+load from HTTP GET or local JSON under the site root), `VirtualSiteConfig.HttpSpec` parsed from `_config.yaml` `http:`, and factory wiring through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. JSON `pages[]` require `id`; `title` + `body` assemble like csv-filesystem / sql-database; optional `path` defaults to `{id}.html`. SSRF fail-closed: http(s) only, no userinfo, `URLValidation`, redirects off-loopback refused, no Authorization/API keys. Product-docs 8.2 describe the SPI/CLI; REST/UI remain sibling slices.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction (catalog file and page path use `/` as logical separators then `Path.resolve`)
- [x] Catalog I/O uses `Path` / `Files.readString` / `Files.isRegularFile` / `Files.size`
- [x] Temp trees via JUnit `@TempDir`
- [x] Windows vs Unix absolute `http.file` assertions behind `@EnabledOnOs`
- [x] Containment: catalog file `startsWith` normalized site root; rejects `..`, NUL, drive letters, leading `/`
- [x] HTTP URLs are not filesystem joins (`http`/`https` only)

## Issues

None blocking.

### Notes (non-blocking)

- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `http-json`. That is the SPI allow-list; REST round-trip tests are #3795.
- Git remotes rejected for any non-`git-filesystem` kind.
- Remote fetch uses `HttpClient.Redirect.NEVER`, rebuilds the request URI from a URLValidation-passed URL, and follows at most one hop when Location is literal loopback.

## Tests

- `PSHttpJsonVirtualSiteSourceTest` — loopback HttpServer discover/load, local fixture, default `pages.json`, fail-closed paths/schemes/userinfo/metadata/off-loopback redirect, loopback redirect, no catalog cache, factory + build HTML, portable catalog file, OS-specific absolute paths
- `PSVirtualSiteHelperTest` — allow-list includes `http-json`; remoteUrl rejected; unknown `sql-api` still rejected
- `VirtualSiteConfigLoaderTest` — `http:` scalar fails closed

## Change-class companions

New Virtual Site source adapter: enum + impl + factory + build-service selection + CLI + helper allow-list + product-docs + unit tests. REST/UI slices filed separately (#3795/#3796).

Standalone: `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS. Tests run: 2402, Failures: 0.
