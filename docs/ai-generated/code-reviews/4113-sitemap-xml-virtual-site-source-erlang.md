# Erlang review: #4113 sitemap-xml IPSVirtualSiteSource

**Branch:** `feat/issue-4113-sitemap-xml-virtual-site-source`

Independent pre-commit review of the sitemap-xml Virtual Site SPI slice (parent #2678).

## Change class

Adds `VirtualSiteSourceType.SITEMAP_XML` (`sitemap-xml`), `PSSitemapXmlVirtualSiteSource` (discover+load from a local `sitemap.xml` under a portable-safe `virtual.rootPath`), optional `VirtualSiteConfig.SitemapSpec` parsed from `_config.yaml` `sitemap:`, and factory wiring through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. Default file `sitemap.xml`; `sitemap.file` overrides; `sitemap.url` is rejected (no live crawl). `urlset` / `sitemapindex` `<loc>` entries resolve to portable files under the site root; non-loopback `http(s)` locs, Git `virtual.remoteUrl`, and credential properties are rejected. Loopback `http(s)` locs are allowed for tests only. Product-docs 8.2 document the local sitemap SPI; REST persist (#4114) and Developer Sites chrome (#4115) stay later slices.

## Gates

- [x] Bugs: fail-closed on blank loc, empty urlset, duplicate ids, traversal, absolute sitemap.file, cloud/userinfo locs, `sitemap.url`
- [x] Portable Path/Files; Windows vs Unix absolute `sitemap.file` behind `@EnabledOnOs`
- [x] Behavioral tests for discover/load, factory peers unchanged, helper remoteUrl/credentials/cloud root, CLI assemble via `forSourceType` (`pageCount > 0`)
- [x] XXE fail-closed (`PSSecureXMLUtils`); SSRF fail-closed loopback (`URLValidation` + host allow-list); CodeQL sink suppression on rebuilt request URI
- [x] Exhaustive `switch` on `VirtualSiteSourceType` lives only in `PSVirtualSiteSourceFactory` (updated). Grep found no reverse-dep exhaustive switches in rest / sitemanage / perc-toolkit.
- [x] Product-docs 8.2 (admin/developer/reference) note local sitemap adapter; REST persist not claimed
- [x] Standalone `cd system && ../mvnw.cmd clean install` BUILD SUCCESS

## Notes

- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `sitemap-xml`. That is the SPI allow-list; REST round-trip tests stay a follow-on (same as icalendar #3986).
- Git remotes rejected for any non-`git-filesystem` kind. Credential-like extra properties and cloud `rootPath` URLs are fail-closed for `sitemap-xml` (peer of icalendar / rss-atom).
