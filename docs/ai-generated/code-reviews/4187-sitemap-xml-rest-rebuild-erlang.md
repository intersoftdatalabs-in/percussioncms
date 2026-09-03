# Erlang review — #4187 REST sitemap-xml Build after current-file edit

- Date: 2026-09-02
- Branch: `fix/issue-4187-sitemap-xml-rest-rebuild`
- Recommendation: **approve**
- Gate: **May commit/push: yes**
- Memory patterns hit: change-class companions (REST resource + sitemanage adaptor tests + product-docs); portable NIO `Path`/`Files`/`@TempDir`; leftover remoteUrl/credentials/cloud 400 fail-closed; do not steal SPI sibling tests.

## Summary

REST Admin `POST /services/sites/{id}/virtual/build` for `sourceKind=sitemap-xml` already re-reads the local fixture per Build (SPI is stateless). This slice adds adaptor + resource tests that PUT sitemap-xml, write `sitemap.xml` via `Path`/`Files`, POST build, edit loc/lastmod/pages, POST build again, and assert `pagesWritten` and HTML change. OpenAPI + product-docs 8.2 make the second-Build-without-Jetty-restart contract explicit. Leftover `virtual.remoteUrl`, credentials, and cloud `rootPath` remain 400 (existing adaptor tests + new resource propagate tests). Did not steal #4166/#4169, #4174/#4179, or SPI unit tests owned by #4186/#4193.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Tests use `Path` / `Files` / `@TempDir`
- [x] No Unix-only absolute path assertions
- [x] Line-ending sensitive assertions are substring contains, not raw `\n` file equality

## Issues

None blocking.

### Suggestion (non-blocking)

`SitesResourceTest.buildVirtualSiteSitemapXmlSecondBuildAfterCurrentFileEditDelegates` uses a Mockito `thenAnswer` that re-reads the fixture and writes HTML. That proves two resource POSTs plus current-file edits; production assemble is covered by `SitesAdaptorTest.buildVirtualSite_sitemapXmlSecondBuildAfterCurrentFileEditWritesUpdatedHtml`. Acceptable split for rest (no sitemanage dependency).
