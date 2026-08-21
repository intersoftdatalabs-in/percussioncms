# Erlang review: #3698 REST csv-filesystem virtual/build

**Date:** 2026-08-21  
**Branch:** `fix/issue-3698-csv-filesystem-virtual-build`  
**Scope:** uncommitted vs `HEAD` / `origin/main` (sitemanage SitesAdaptor, system config fallback, rest OpenAPI, product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor + resource tests + product-docs REST/admin); no WebUI/Playwright required (explicitly out of scope #3697).

## Summary

Removes the git-only gate on `SitesAdaptor.buildVirtualSite`. `runBuild` uses `PSVirtualSiteBuildService.forSourceType` (`PSVirtualSiteSourceFactory`). CSV trees may omit `_config.yaml` (`VirtualSiteConfigLoader.loadOrDefault` infers version folders). Unknown `sourceKind` still 400. git-filesystem build unchanged. Product-docs 8.2 REST/admin/developer/reference state CSV Virtual Sites can be built.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Config fallback uses `Path` / `Files.newDirectoryStream` / `Path.normalize` / `startsWith`
- [x] Temp trees via JUnit `@TempDir`
- [x] CSV fixture paths via `Path.resolve` (not OS separator concatenation)
- [x] Containment: inferred version dirs must `startsWith` normalized site root
- [x] Line-ending: CSV parser already normalizes; tests do not assert raw `\n` file bytes

## Issues

None blocking.

### Notes (non-blocking)

- Publish (`POST …/virtual/publish`) now inherits CSV build because it calls `buildVirtualSite`. Documented CSV **publish** remains slice #3699 (no extra UI/docs claim here).
- REST resource tests mock the adaptor; real CSV HTML emit is covered in `SitesAdaptorTest` and system `PSCsvFilesystemVirtualSiteSourceTest`.

## Tests

- `SitesAdaptorTest` — CSV real-filesystem build (`pagesWritten > 0`), optional `_config.yaml`, missing column 400, unsafe path 400, unknown kind 400; existing git-filesystem tests
- `SitesResourceTest` — temp CSV fixture delegation, unknown kind 400, OpenAPI `csv-filesystem` mention
- `VirtualSiteConfigLoaderTest` — `loadOrDefault` infers versions / fails empty / uses YAML when present
- `PSCsvFilesystemVirtualSiteSourceTest` — CLI/service build without `_config.yaml`

## Change-class companions

REST virtual/build for a new source kind: adaptor gate + factory wiring + config fallback + resource OpenAPI + sitemanage + rest + system tests + product-docs 8.2. No WebUI (later #3697). No publish docs as complete (#3699).

## Builds

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS
- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS
