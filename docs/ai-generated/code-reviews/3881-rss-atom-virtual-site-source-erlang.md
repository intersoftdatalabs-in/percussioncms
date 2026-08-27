# Erlang review: #3881 rss-atom IPSVirtualSiteSource

**Date:** 2026-08-27  
**Branch:** `feat/issue-3881-rss-atom-virtual-site-source`  
**Scope:** uncommitted vs `HEAD` / `origin/main` (system virtualsite SPI + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection (remoteUrl, credentials, cloud root, non-loopback url, userinfo, XXE/DOCTYPE, unsafe feed paths); change-class companions (SPI + factory + tests + product-docs); no REST/UI companions required (explicitly out of scope); no live-feed secrets.

## Summary

Adds `VirtualSiteSourceType.RSS_ATOM` (`rss-atom`), `PSRssAtomVirtualSiteSource` (discover+load from local RSS 2.0 / Atom XML under a portable-safe `virtual.rootPath`), optional `VirtualSiteConfig.RssSpec` parsed from `_config.yaml` `rss:`, and factory wiring through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. Default files `feed.xml` then `atom.xml`; `rss.file` overrides; `rss.url` is loopback http(s) only. Item `guid`/`id`/`link` + `title` + `content`/`summary`/`description` assemble like csv-filesystem / http-json. `virtual.remoteUrl` and credential properties are rejected. Product-docs 8.2 document the local/loopback SPI; REST persist and Developer Sites chrome stay later slices.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction (feed file and page path use `/` as logical separators then `Path.resolve`)
- [x] Feed I/O uses `Path` / `Files.readString` / `Files.isRegularFile` / `Files.size`
- [x] Temp trees via JUnit `@TempDir`
- [x] Windows vs Unix absolute `rss.file` assertions behind `@EnabledOnOs`
- [x] Containment: feed file `startsWith` normalized site root; rejects `..`, NUL, drive letters, leading `/`
- [x] HTTP URLs are not filesystem joins (`http`/`https` loopback only)
- [x] Id slugs for default `{version}/{slug}.html` avoid Windows-illegal `:` in URL guids

## Issues

None blocking.

### Notes (non-blocking)

- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `rss-atom`. That is the SPI allow-list; REST round-trip tests stay a follow-on (same as http-json #3794 / object-storage #3838).
- Git remotes rejected for any non-`git-filesystem` kind. Credential-like extra properties and cloud `rootPath` URLs are fail-closed for `rss-atom` (peer of object-storage).
- Exhaustive `switch` on `VirtualSiteSourceType` lives only in `PSVirtualSiteSourceFactory` (updated). Grep found no reverse-dep exhaustive switches in rest / sitemanage / perc-toolkit.
- XML parse uses `PSSecureXMLUtils` + `PSXmlSecurityOptions.secure()` (DOCTYPE / XXE fail-closed).

## Tests

- `PSRssAtomVirtualSiteSourceTest` — local RSS and Atom fixtures, default `feed.xml`/`atom.xml`, loopback HttpServer, Atom summary fallback, fail-closed blank id/title, invalid XML, DOCTYPE, unknown load, duplicates, path traversal, Windows/Unix absolute feed files, non-http / userinfo / cloud / metadata URLs, both url+file, no parse cache, factory + peers unchanged, unknown `sql-api` still rejected, build emits HTML (`pageCount > 0`), YAML `rss.file`, rebuild after edit
- `PSVirtualSiteHelperTest` — allow-list includes `rss-atom`; safe root; `remoteUrl`, credentials, and cloud `rootPath` rejected; unknown `sql-api` lists `rss-atom`
- `VirtualSiteConfigLoaderTest` — `rss:` scalar fails closed

## Change-class companions

New Virtual Site source adapter: enum + impl + factory + build-service selection + CLI + helper allow-list + product-docs + unit tests. REST persist / Build / Preview / Publish and Developer Sites chrome are follow-on slices. No Playwright (no UI).

Standalone: `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS. Tests run: 2515, Failures: 0, Errors: 0, Skipped: 246.
