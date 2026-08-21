# Erlang review: #3709 REST csv-filesystem virtual/preview

**Date:** 2026-08-21  
**Branch:** `feat/issue-3709-csv-virtual-preview`  
**Scope:** uncommitted vs `HEAD` / `origin/main` (rest + sitemanage preview + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor tests + REST OpenAPI/docs + product-docs); no WebUI/Playwright (explicitly out of scope #3707).

## Summary

Proves `GET /sites/{nameOrId}/virtual/preview` and `GET …/virtual/preview/{relPath}` for `csv-filesystem` last-build output. Preview was already last-output based (`requireVirtualAdminSite` + `resolveLastOutputRoot`); this slice closes the remaining git-only gap in tests, OpenAPI, and product-docs. Adaptor tests assemble a CSV fixture via `PSVirtualSiteBuildService.forSourceType(CSV_FILESYSTEM)`, inject the last-output pointer, and assert available+homePath, streamed HTML, missing-build 404/empty (no 500), `../` 400, unknown `sourceKind` 400. Existing git-filesystem preview tests remain.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] CSV fixture and last-output use `@TempDir` + `Path` / `Files.createDirectories` / `Files.writeString` / `Files.isRegularFile`
- [x] Home path asserted as wire form `8.2/index.html` (URL-style `/`, not OS `toString()`)
- [x] Traversal uses relative `../secret.txt` (existing portable reject path)
- [x] Line-ending sensitive HTML assertions use `contains` on assembled text, not raw `\n` file equality

## Issues

None blocking.

### Notes (non-blocking)

- REST `SitesAdaptor.buildVirtualSite` still rejects non-`git-filesystem`. Out of scope (#3698). Preview injects last-output so this slice does not re-implement Build.
- Developer Sites Preview chrome / Playwright is #3707.

## Tests

- `SitesAdaptorTest.previewCsvFilesystem_lastBuildHomeAndRejectsTraversal` — SPI CSV assemble + last-output pointer → available/homePath, HTML 200, `../` 400, missing file 404
- `SitesAdaptorTest.previewCsvFilesystem_missingBuildIsUnavailableNot500` — status available=false, file 404
- `SitesAdaptorTest.preview_rejectsUnknownSourceKind` — `sql-adapter` 400
- Existing git-filesystem preview + repository 400 tests unchanged
- `SitesResourceTest` OpenAPI string guard that preview is not git-only

## Change-class companions

REST last-build preview for a new Virtual source kind: adaptor behavioral tests (CSV fixture), rest OpenAPI/javadoc, product-docs 8.2 REST/admin/developer/reference. No public method signature / `final` API change (C2 none). No WebUI.

## Builds

- `cd rest && ../mvnw clean install` — BUILD SUCCESS, Tests run: 539, Failures: 0
- `cd projects/sitemanage && ../../mvnw clean install` — BUILD SUCCESS, Tests run: 1297, Failures: 0 (`SitesAdaptorTest` 43/0)
