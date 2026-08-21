# Erlang review: #3709 REST csv-filesystem virtual/preview

**Date:** 2026-08-21  
**Branch:** `feat/issue-3709-csv-virtual-preview-rest`  
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

- After rebase onto `main` (#3710), REST `SitesAdaptor.buildVirtualSite` records last-output for `csv-filesystem` as well as `git-filesystem`. CLI assemble still does not write the pointer; preview then falls back to the default output root.
- Developer Sites Preview chrome lives in the Virtual Build section (shown for csv-filesystem after #3710). Playwright coverage is #3707.

## Tests

- `SitesAdaptorTest.previewCsvFilesystem_*` — write assembled HTML tree (no CSV assembler); split status/home, stream, traversal 400, missing 404, default-root fallback without pointer
- `SitesAdaptorTest.previewCsvFilesystem_missingBuildIsUnavailableNot500` — `available=false` and `SitesAdaptor.MISSING_PREVIEW_MESSAGE`
- `SitesAdaptorTest.preview_rejectsUnknownSourceKind` — `sql-adapter` 400
- Existing git-filesystem preview + repository 400 tests unchanged
- `SitesResourceTest` OpenAPI guards that both preview `@Operation` descriptions mention `csv-filesystem`

## Change-class companions

REST last-build preview for a new Virtual source kind: adaptor behavioral tests (CSV fixture), rest OpenAPI/javadoc, product-docs 8.2 REST/admin/developer/reference. No public method signature / `final` API change (C2 none). No WebUI.

## Builds

- `cd rest && ../mvnw clean install` — BUILD SUCCESS, Tests run: 539, Failures: 0
- `cd projects/sitemanage && ../../mvnw clean install` — BUILD SUCCESS, Tests run: 1297, Failures: 0 (`SitesAdaptorTest` 43/0)
