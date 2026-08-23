# Erlang review: #3758 REST sql-database virtual/build

**Date:** 2026-08-23  
**Branch:** `feat/issue-3758-sql-database-virtual-build` (stacked on SPI #3733 / PR #3745 and REST sourceKind #3734 / PR #3746)  
**Scope:** uncommitted rest + sitemanage + system javadoc + product-docs 8.2 vs stacked HEAD  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor + resource tests + product-docs REST/admin); no WebUI/Playwright required (explicitly out of scope slice 2 / #3735).

## Summary

`POST /sites/{nameOrId}/virtual/build` already selected `PSVirtualSiteBuildService.forSourceType` for allow-listed kinds (stacked SPI/sourceKind). This slice proves SQL REST Build: in-memory H2 adaptor tests (`pagesWritten > 0`, queryFile NIO path, missing column / missing `_config.yaml` / Oracle+MySQL URL 400), resource fixture + OpenAPI `jdbc:h2:mem:` guard, unknown kinds still 400, git/CSV unchanged. Product-docs 8.2 REST/admin/developer/publishing state operators can Build a SQL Virtual Site on H2.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] H2 fixtures use `Path` / `Files.createDirectories` / `Files.writeString` / `@TempDir`
- [x] `sql.queryFile` stored as logical `/` path (`replace('\\', '/')`) after `Path.resolve`
- [x] Output HTML asserted via `out.resolve("8.2").resolve("index.html")` (not OS separator strings)
- [x] Line-ending sensitive HTML assertions use `contains`, not raw `\n` file equality
- [x] JDBC URLs are not filesystem joins (`jdbc:h2:mem:`)

## Issues

None blocking.

### Notes (non-blocking)

- Resource tests mock the adaptor (peer CSV); real H2 HTML emit is in `SitesAdaptorTest` and system `PSSqlDatabaseVirtualSiteSourceTest.buildServiceFactoryWiresSqlAndEmitsHtml`.
- Developer Sites SQL Build chrome remains a later slice; publishing.md and admin Sites say REST/CLI until then.
- Publish inherits SQL build because `publishVirtualSite` calls `buildVirtualSite` (documented; full SQL publish chrome is a later slice).

## Tests

- `SitesAdaptorTest` — H2 REST Build HTML (`pagesWritten > 0`); queryFile; missing column 400; Oracle/MySQL URL 400; missing `_config.yaml` 400; unknown kind 400; existing git/CSV tests
- `SitesResourceTest` — temp SQL `_config.yaml` fixture delegation; OpenAPI mentions sql-database and `jdbc:h2:mem:`
- System SPI tests already cover factory + `forSourceType(SQL_DATABASE)` emit (stacked #3733)

## Change-class companions

REST virtual/build for a new source kind (peer #3698 csv-filesystem): factory wiring (SPI), adaptor gate, resource OpenAPI, sitemanage + rest tests, product-docs 8.2. No WebUI (later Developer Sites SQL chrome). No new adaptor interface.

## Builds

- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 2358, Failures: 0, Skipped: 243
- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 549, Failures: 0 (`SitesResourceTest` 29)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1397, Failures: 0 (`SitesAdaptorTest` 66)
