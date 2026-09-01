# Erlang review — #4125 REST sitemap-xml virtual/preview

**Date:** 2026-09-01  
**Branch:** `feat/issue-4125-sitemap-xml-virtual-preview`  
**Scope:** uncommitted rest + sitemanage + product-docs 8.2 vs stacked #4124 Build  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for last-output preview + missing build + leftover remoteUrl/credentials 400; change-class companions (adaptor + resource tests + Spring stub comment + OpenAPI + product-docs REST/admin/developer/reference); no WebUI/Playwright required (Developer Sites chrome is out of scope).

## Summary

`GET /sites/{nameOrId}/virtual/preview` (status) and `GET …/virtual/preview/{relPath}` (file) already gate on `PSVirtualSiteHelper.validate()`, which allow-lists `sitemap-xml`. This slice documents and proves that last-build contract: adaptor tests (after REST Build HTML from a local `sitemap.xml` fixture whose `<loc>` slugs to `index`, missing build `available=false` HTTP 200, traversal 400, missing file 404, CLI default-root fallback, leftover `virtual.remoteUrl`/credentials 400), resource Mockito + OpenAPI `sitemap-xml` last-build local HTML guards, Spring `SitesTestAdaptor` comment. git/CSV/SQL/`http-json`/object-storage/rss-atom/icalendar preview unchanged; unknown `sourceKind` still 400. Product-docs 8.2 REST/admin/developer/reference document sitemap-xml last-build Preview. REST Publish and Developer Sites chrome stay later slices. No live crawl, credentials, or SPA Preview chrome.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Sitemap fixtures use `Path` / `Files.createDirectories` / `Files.writeString` / `@TempDir`
- [x] Last-output HTML asserted via `resolve("8.2").resolve("index.html")` (not OS separator strings)
- [x] Line-ending sensitive HTML assertions use `contains`, not raw `\n` file equality
- [x] Operator examples remain portable field values (`8.2/index.html` as URL/relative preview paths)

## Issues

None blocking.

### Notes (non-blocking)

- Production adaptor had no sitemap-xml exclusion on preview: `requireVirtualAdminSite` → `validate()` already allow-lists the kind. This slice is OpenAPI, tests, docs, and javadoc.
- REST Build sibling #4124 / PR #4137 is stacked: after-build preview consumes that factory path with a local sitemap fixture (`pages/index.md` so assembled home is `8.2/index.html`).
- Persist REST (#4114 / PR #4121), Publish REST (#4126), and Developer Sites Preview chrome remain later slices.

## Tests

- `SitesAdaptorTest` — `previewSitemapXml_afterBuildAvailableWithHtml` (`available=true` + HTML after Build); missing build `available=false` not 500; traversal 400 / missing file 404; default output fallback without last-output pointer; leftover remoteUrl/credentials 400
- `SitesResourceTest` — status/file delegation for SitemapHelp; missing-build `available=false`; leftover remoteUrl 400; OpenAPI preview status/file blocks mention `sitemap-xml`
- `SitesTestAdaptor` — Spring stub comment covers last-output preview for `sitemap-xml` (no new adaptor interface)
- Unknown kind preview still 400 (`preview_rejectsUnknownSourceKind`)

## Change-class companions

REST virtual/preview for a new source kind (peer #3916 rss-atom / #3988 icalendar): factory/allow-list (already on main), adaptor last-build proof, resource OpenAPI, sitemanage + rest tests, product-docs 8.2. No WebUI (later chrome slice). No new adaptor interface.

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1020, Failures: 0 (`SitesResourceTest` 72)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 2225, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 170 including sitemap-xml last-build preview)
- C2 N/A (no `final`/`sealed`/public signature change). Reverse-dep `projects/sitemanage` still standalone clean-installed as `ISiteAdaptor` implementer.
