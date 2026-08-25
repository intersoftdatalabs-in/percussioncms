# Erlang review: #3807 REST http-json virtual/preview

**Date:** 2026-08-25  
**Branch:** `feat/issue-3807-http-json-virtual-preview`  
**Scope:** uncommitted vs stacked `feat/issue-3806-http-json-virtual-build` (preview tests + OpenAPI/javadoc + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor tests + REST resource tests + OpenAPI/docs + product-docs); no WebUI/Playwright (explicitly out of scope — Developer Sites Preview chrome).

## Summary

Proves `GET /sites/{nameOrId}/virtual/preview` and `GET …/virtual/preview/{relPath}` for `http-json` last-build output (local JSON fixture). Preview was already last-output based (`requireVirtualAdminSite` + `resolveLastOutputRoot`) and the stacked HTTP JSON Build path (#3806 / PR #3813) allow-lists `http-json`. This slice closes the remaining proof gap: adaptor tests run REST Build then assert `available=true` + streamed HTML; missing build is `available=false` HTTP 200 (file 404, not 500); `../` is 400; unknown `sourceKind` stays 400 (existing test). git/csv/sql preview tests unchanged. Product-docs 8.2 REST/admin/developer/reference document HTTP JSON last-build Preview. Developer Sites Preview chrome remains a later phase.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] JSON fixture and last-output use `@TempDir` + `Path` / `Files.createDirectories` / `Files.writeString`
- [x] Build `outputRoot` set via `Path.toAbsolutePath().normalize().toString()`
- [x] Home path asserted as wire form `8.2/index.html` (URL-style `/`, not OS `toString()`)
- [x] Traversal uses relative `../secret.txt` (existing portable reject path)
- [x] Line-ending sensitive HTML assertions use `contains` on assembled text, not raw `\n` file equality

## Issues

None blocking.

### Notes (non-blocking)

- Stacked on unmerged REST http-json virtual/build #3806 / PR #3813 (SPI #3794 + sourceKind #3795). Production preview code did not need a new gate: `PSVirtualSiteHelper.validate` already allow-lists `http-json`.
- Developer Sites Preview chrome is out of scope (follow-on; not this slice).
- Publish REST for `http-json` remains a later slice.

## Tests

- `SitesAdaptorTest.previewHttpJson_afterBuildAvailableWithHtml` — JSON fixture → `buildVirtualSite` → status `available=true` / `homePath=8.2/index.html` and streamed HTML contains HTTP Home
- `SitesAdaptorTest.previewHttpJson_missingBuildIsUnavailableNot500` — `available=false` + `MISSING_PREVIEW_MESSAGE`; file 404
- `SitesAdaptorTest.previewHttpJson_rejectsTraversalAndMissingFile` — `../` 400; missing file 404
- `SitesAdaptorTest.previewHttpJson_defaultOutputFallbackWithoutPointer` — CLI-style default root without last-output pointer
- `SitesAdaptorTest.preview_rejectsUnknownSourceKind` — `sql-adapter` 400 (unchanged)
- Existing git-filesystem, csv-filesystem, and sql-database preview tests unchanged
- `SitesResourceTest.previewStatusDelegatesHttpJson` / `previewStatusHttpJsonMissingBuildIsUnavailable` / `previewFileDelegatesHttpJsonHtml`
- OpenAPI guards that preview `@Operation` descriptions mention `http-json`

## Change-class companions

REST last-build preview for a new Virtual source kind: adaptor behavioral tests (http-json last-build fixture), rest resource tests + OpenAPI/javadoc, product-docs 8.2 REST/admin/developer/reference. No public method signature / `final` API change (C2 none). No WebUI.

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 596, Failures: 0 (`SitesResourceTest` 39/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1543, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 89/0)
- `scripts/ci-smoke-product-docs.bat` — OK (8.2/index.html)
