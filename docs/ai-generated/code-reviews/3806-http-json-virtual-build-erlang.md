# Erlang review — #3806 REST http-json virtual/build

**Date:** 2026-08-25  
**Branch:** `feat/issue-3806-http-json-virtual-build`  
**Scope:** uncommitted rest + sitemanage + product-docs 8.2 vs `origin/main`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor + resource tests + product-docs REST/admin); no WebUI/Playwright required (explicitly out of scope Preview #3807 / Build chrome #3808 / Publish).

## Summary

`POST /sites/{nameOrId}/virtual/build` already selected `PSVirtualSiteBuildService.forSourceType` for allow-listed kinds (SPI #3794 / REST persist #3795). This slice proves HTTP JSON REST Build: adaptor tests (`pagesWritten > 0` on a local JSON fixture, optional loopback `http.url`, missing `id` / missing `_config.yaml` / leftover `virtual.remoteUrl` 400), resource fixture + OpenAPI `http-json` guard, unknown kinds still 400, git/CSV/SQL unchanged. Product-docs 8.2 REST/admin/developer drop “Build later phase” for REST Build; Preview/Publish REST and Developer Sites Build chrome stay later slices.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] JSON fixtures use `Path` / `Files.createDirectories` / `Files.writeString` / `@TempDir`
- [x] Output HTML asserted via `out.resolve("8.2").resolve("index.html")` (not OS separator strings)
- [x] Line-ending sensitive HTML assertions use `contains`, not raw `\n` file equality
- [x] Loopback catalog URL uses URI `/` (`http://127.0.0.1:{port}/pages.json`) — not a filesystem join
- [x] Catalog file name `pages.json` resolved with `Path.resolve`

## Issues

None blocking.

### Notes (non-blocking)

- Resource tests mock the adaptor (peer SQL #3758); real HTML emit is in `SitesAdaptorTest` and system `PSHttpJsonVirtualSiteSourceTest.buildServiceFactoryWiresHttpJsonAndEmitsHtml`.
- Production adaptor had no http-json exclusion: factory + helper already allow-listed the kind. This slice is OpenAPI, tests, docs, and `loadBuildConfig` javadoc.
- Publish still calls `buildVirtualSite`; this PR does not claim REST Publish for http-json (out of scope).

## Tests

- `SitesAdaptorTest` — local fixture REST Build HTML (`pagesWritten > 0`); loopback `http.url`; missing `id` 400; missing `_config.yaml` 400; leftover `virtual.remoteUrl` 400; unknown kind 400; existing git/CSV/SQL tests
- `SitesResourceTest` — temp HTTP JSON `_config.yaml` + `pages.json` fixture delegation; OpenAPI mentions `http-json` in the build path block
- System SPI tests already cover factory + `forSourceType(HTTP_JSON)` emit (merged #3794)

## Change-class companions

REST virtual/build for a new source kind (peer #3758 sql-database): factory wiring (already on main), adaptor proof, resource OpenAPI, sitemanage + rest tests, product-docs 8.2. No WebUI (later #3808). No new adaptor interface.

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 593, Failures: 0 (`SitesResourceTest` 36)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1467, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 85)
- C2 N/A (no `final`/`sealed`/signature change). Reverse-dep `projects/sitemanage` still clean-installed as `ISiteAdaptor` implementer.
