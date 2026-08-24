# Erlang review: #3761 REST sql-database virtual/preview

**Date:** 2026-08-23  
**Branch:** `feat/issue-3761-sql-database-virtual-preview`  
**Scope:** uncommitted vs stacked `feat/issue-3758-sql-database-virtual-build` (preview tests + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor tests + REST resource tests + OpenAPI/docs + product-docs); no WebUI/Playwright (explicitly out of scope — Developer Sites Preview chrome).

## Summary

Proves `GET /sites/{nameOrId}/virtual/preview` and `GET …/virtual/preview/{relPath}` for `sql-database` last-build output (in-memory H2). Preview was already last-output based (`requireVirtualAdminSite` + `resolveLastOutputRoot`) and the stacked SQL Build path (#3758 / PR #3765) allow-lists `sql-database`. This slice closes the remaining proof gap: adaptor tests run H2 Build then assert `available=true` + streamed HTML; missing build is `available=false` HTTP 200 (file 404, not 500); `../` is 400; unknown `sourceKind` stays 400 (existing test). git/csv preview tests unchanged. Product-docs 8.2 REST/admin/developer/reference document SQL last-build Preview.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] H2 fixture and last-output use `@TempDir` + `Path` / `Files.createDirectories` / `Files.writeString`
- [x] Build `outputRoot` set via `Path.toAbsolutePath().normalize().toString()`
- [x] Home path asserted as wire form `8.2/index.html` (URL-style `/`, not OS `toString()`)
- [x] Traversal uses relative `../secret.txt` (existing portable reject path)
- [x] Line-ending sensitive HTML assertions use `contains` on assembled text, not raw `\n` file equality

## Issues

None blocking.

### Notes (non-blocking)

- Stacked on unmerged REST sql-database virtual/build #3758 / PR #3765 (SPI #3733 + sourceKind #3734). Production preview code did not need a new gate: `PSVirtualSiteHelper.validate` already allow-lists `sql-database`.
- Developer Sites Preview chrome is out of scope (follow-on; not this slice).

## Tests

- `SitesAdaptorTest.previewSqlDatabase_afterBuildAvailableWithHtml` — H2 fixture → `buildVirtualSite` → status `available=true` / `homePath=8.2/index.html` and streamed HTML contains SQL Home
- `SitesAdaptorTest.previewSqlDatabase_missingBuildIsUnavailableNot500` — `available=false` + `MISSING_PREVIEW_MESSAGE`; file 404
- `SitesAdaptorTest.previewSqlDatabase_rejectsTraversalAndMissingFile` — `../` 400; missing file 404
- `SitesAdaptorTest.previewSqlDatabase_defaultOutputFallbackWithoutPointer` — CLI-style default root without last-output pointer
- `SitesAdaptorTest.preview_rejectsUnknownSourceKind` — `sql-adapter` 400 (unchanged)
- Existing git-filesystem and csv-filesystem preview tests unchanged
- `SitesResourceTest.previewStatusDelegatesSqlDatabase` / `previewStatusSqlDatabaseMissingBuildIsUnavailable` / `previewFileDelegatesSqlDatabaseHtml`
- Existing OpenAPI guards that preview `@Operation` descriptions mention `sql-database`

## Change-class companions

REST last-build preview for a new Virtual source kind: adaptor behavioral tests (H2 last-build fixture), rest resource tests + OpenAPI/javadoc, product-docs 8.2 REST/admin/developer/reference. No public method signature / `final` API change (C2 none). No WebUI.

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 552, Failures: 0 (`SitesResourceTest` 32/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1401, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 70/0)
- `scripts/ci-smoke-product-docs.bat` — OK (8.2/index.html)
