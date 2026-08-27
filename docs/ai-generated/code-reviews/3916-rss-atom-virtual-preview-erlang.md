# Erlang review — #3916 REST rss-atom virtual/preview

**Date:** 2026-08-27  
**Branch:** `feat/issue-3916-rss-atom-virtual-preview`  
**Scope:** uncommitted rest + sitemanage + product-docs 8.2 vs `origin/main`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for last-output preview + missing build + traversal; change-class companions (adaptor + resource tests + Spring stub comment + product-docs REST/admin/developer); no WebUI/Playwright required (Developer Sites chrome is out of scope).

## Summary

`GET /sites/{nameOrId}/virtual/preview` (status) and `GET …/virtual/preview/{relPath}` (file) already gate on `PSVirtualSiteHelper.validate()`, which allow-lists `rss-atom`. This slice documents and proves that last-build contract: adaptor tests (after REST Build HTML from a local `feed.xml` fixture, missing build `available=false` HTTP 200, traversal 400, missing file 404, CLI default-root fallback), resource Mockito + OpenAPI `rss-atom` guards, Spring `SitesTestAdaptor` comment. git/CSV/SQL/`http-json`/object-storage preview unchanged; unknown `sourceKind` still 400. Product-docs 8.2 REST/admin/developer/reference document rss-atom last-build Preview. REST persist, REST Publish, and Developer Sites chrome stay later slices. No live remote feeds, credentials, or SPA Preview chrome.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] RSS fixtures use `Path` / `Files.createDirectories` / `Files.writeString` / `@TempDir`
- [x] Last-output HTML asserted via `resolve("8.2").resolve("index.html")` (not OS separator strings)
- [x] Line-ending sensitive HTML assertions use `contains`, not raw `\n` file equality
- [x] Operator examples remain portable field values (`8.2/index.html` as URL/relative preview paths)

## Issues

None blocking.

### Notes (non-blocking)

- Production adaptor had no rss-atom exclusion on preview: `requireVirtualAdminSite` → `validate()` already allow-lists the kind. This slice is OpenAPI, tests, docs, and javadoc.
- REST Build for rss-atom already selects `PSVirtualSiteBuildService.forSourceType`; sibling #3915 / PR #3921 adds dedicated Build tests/docs. The after-build preview test here consumes that factory path with a local RSS fixture (feed item id `index` so assembled home is `8.2/index.html`).
- Persist REST (#3888), Publish REST (#3917), and Developer Sites Preview chrome remain later slices.

## Tests

- `SitesAdaptorTest` — `previewRssAtom_afterBuildAvailableWithHtml` (`available=true` + HTML after Build); missing build `available=false` not 500; traversal 400 / missing file 404; default output fallback without last-output pointer
- `SitesResourceTest` — status/file delegation for RssHelp; missing-build `available=false`; OpenAPI preview status/file blocks mention `rss-atom`
- `SitesTestAdaptor` — Spring stub comment covers last-output preview for `rss-atom` (no new adaptor interface)
- Unknown kind preview still 400 (`preview_rejectsUnknownSourceKind`)

## Change-class companions

REST virtual/preview for a new source kind (peer #3858 object-storage / #3807 http-json): factory/allow-list (already on main), adaptor last-build proof, resource OpenAPI, sitemanage + rest tests, product-docs 8.2. No WebUI (later chrome slice). No new adaptor interface.

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 658, Failures: 0 (`SitesResourceTest` 50)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1568, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 114 including rss-atom last-build preview)
- C2 N/A (no `final`/`sealed`/public signature change). Reverse-dep `projects/sitemanage` still standalone clean-installed as `ISiteAdaptor` implementer.
