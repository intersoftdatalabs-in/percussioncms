# Erlang review — #3857 REST object-storage virtual/build

**Date:** 2026-08-26  
**Branch:** `feat/issue-3857-object-storage-virtual-build`  
**Scope:** uncommitted rest + sitemanage + product-docs 8.2 vs `origin/main`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor + resource tests + Spring stub comment + product-docs REST/admin); no WebUI/Playwright required (explicitly out of scope Preview #3858 / Build chrome later).

## Summary

`POST /sites/{nameOrId}/virtual/build` already selected `PSVirtualSiteBuildService.forSourceType` for allow-listed kinds (SPI #3838 / REST persist #3839). This slice proves object-storage REST Build: adaptor tests (`pagesWritten > 0` on a local object-key fixture, missing `_config.yaml` / leftover `virtual.remoteUrl` 400), resource fixture + OpenAPI `object-storage` guard, unknown kinds still 400, git/CSV/SQL/http-json unchanged. Product-docs 8.2 REST/admin/developer drop “Build later phase” for REST Build; Preview/Publish REST and Developer Sites chrome stay later slices. No cloud URLs, IAM, or secrets on the REST envelope.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Object-key fixtures use `Path` / `Files.createDirectories` / `Files.writeString` / `@TempDir`
- [x] Output HTML asserted via `out.resolve("8.2").resolve("index.html")` (not OS separator strings)
- [x] Line-ending sensitive HTML assertions use `contains`, not raw `\n` file equality
- [x] Operator examples remain portable field values (`C:/object-docs`)

## Issues

None blocking.

### Notes (non-blocking)

- Resource tests mock the adaptor (peer http-json #3806); real HTML emit is in `SitesAdaptorTest` and system `PSObjectStorageVirtualSiteSourceTest` (already on `main`).
- Production adaptor had no object-storage exclusion: factory + helper already allow-listed the kind. This slice is OpenAPI, tests, docs, and `loadBuildConfig` javadoc.
- Publish still calls `buildVirtualSite`; this PR does not claim REST Publish or Preview for object-storage (out of scope).

## Tests

- `SitesAdaptorTest` — local fixture REST Build HTML (`pagesWritten > 0`); missing `_config.yaml` 400; leftover `virtual.remoteUrl` 400; unknown kind 400; existing git/CSV/SQL/http-json tests
- `SitesResourceTest` — temp object-storage `_config.yaml` + Markdown fixture delegation; OpenAPI mentions `object-storage` in the build path block
- `SitesTestAdaptor` — Spring stub comment covers REST Build for `object-storage` (no new adaptor interface)
- System SPI tests already cover factory + `forSourceType(OBJECT_STORAGE)` emit (merged #3838)

## Change-class companions

REST virtual/build for a new source kind (peer #3806 http-json): factory wiring (already on main), adaptor proof, resource OpenAPI, sitemanage + rest tests, product-docs 8.2. No WebUI (later chrome slice). No new adaptor interface.

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 612, Failures: 0 (`SitesResourceTest` 43)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1617, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 101)
- C2 N/A (no `final`/`sealed`/signature change). Reverse-dep `projects/sitemanage` still clean-installed as `ISiteAdaptor` implementer.
