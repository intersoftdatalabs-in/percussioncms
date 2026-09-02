# Erlang review: #4124 REST sitemap-xml virtual/build

**Branch:** `feat/issue-4124-sitemap-xml-virtual-build`

Independent pre-commit review of REST POST `/sites/{nameOrId}/virtual/build` for
`virtual.sourceKind=sitemap-xml` (parent #2678; consumes persist #4114 / PR #4121).

## Change class

REST Build companion for the sitemap-xml Virtual Site adapter. Factory / SPI already on
main (`VirtualSiteSourceType.SITEMAP_XML`, `PSVirtualSiteSourceFactory`). This slice
documents and tests POST `/sites/{nameOrId}/virtual/build` against a portable-safe local
`sitemap.xml` fixture (`pagesWritten > 0`). Leftover `virtual.remoteUrl`, credential-like
extra properties, and cloud URL `rootPath` remain 400. Missing fixture / unsafe path 400.
Git/csv/sql/http-json/object-storage/rss-atom/icalendar Build paths unchanged. REST
Preview/Publish and Developer Sites chrome are **not** in this PR.

## Checklist

- [x] Adaptor tests: local urlset fixture writes HTML (`pagesWritten > 0`); missing
      sitemap.xml / missing `_config.yaml` / unsafe `rootPath` 400; leftover remoteUrl /
      credentials / cloud rootPath 400.
- [x] Resource tests: fixture delegates; remoteUrl 400 propagates; OpenAPI mentions
      `sitemap-xml` local fixture / no live crawl.
- [x] OpenAPI POST `/virtual/build` lists `sitemap-xml` local sitemap.xml only.
- [x] product-docs 8.2 REST / Virtual Sites / admin Sites / site-config: operators can
      Build a sitemap-xml Virtual Site from a local sitemap.xml.
- [x] Portable NIO `Path` / `Files` in tests (no hardcoded `/` filesystem joins).
- [x] No Preview/Publish/chrome in this PR.
- [x] No system SPI re-implementation; factory already on main (PR #4120 not required).

## Notes

- `SitesAdaptor.buildVirtualSite` already routes through
  `PSVirtualSiteBuildService.forSourceType`; production change is comments + tests +
  OpenAPI + product-docs.
- C2: no public method signature change. Downstream `projects/sitemanage` is the rest
  reverse-dep and is built standalone after rest install.

## Verdict

Pass (pending rest + sitemanage standalone `clean install`).
