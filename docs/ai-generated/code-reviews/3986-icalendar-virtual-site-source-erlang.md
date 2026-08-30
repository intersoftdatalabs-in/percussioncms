# Erlang review: #3986 icalendar IPSVirtualSiteSource

**Date:** 2026-08-28  
**Branch:** `fix/issue-3986-icalendar-virtual-site-source`  
**Scope:** uncommitted vs `HEAD` / `origin/main` (system virtualsite SPI + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection (remoteUrl, credentials, cloud root, icalendar.url, unsafe calendar paths); change-class companions (SPI + factory + tests + product-docs); no REST/UI companions required (explicitly out of scope); no CalDAV secrets.

## Summary

Adds `VirtualSiteSourceType.ICALENDAR` (`icalendar`), `PSIcalendarVirtualSiteSource` (discover+load VEVENTs from a local RFC 5545 `calendar.ics` under a portable-safe `virtual.rootPath`), optional `VirtualSiteConfig.IcalendarSpec` parsed from `_config.yaml` `icalendar:`, and factory wiring through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. Default file `calendar.ics`; `icalendar.file` overrides; `icalendar.url` is rejected (no CalDAV / live remotes). VEVENT `UID` + `SUMMARY` + `DTSTART` + `DESCRIPTION` assemble like csv-filesystem / rss-atom. `virtual.remoteUrl` and credential properties are rejected. Product-docs 8.2 document the local fixture SPI; REST persist and Developer Sites chrome stay later slices.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction (calendar file and page path use `/` as logical separators then `Path.resolve`)
- [x] Calendar I/O uses `Path` / `Files.readString` / `Files.isRegularFile` / `Files.size`
- [x] Temp trees via JUnit `@TempDir`
- [x] Windows vs Unix absolute `icalendar.file` assertions behind `@EnabledOnOs`
- [x] Containment: calendar file `startsWith` normalized site root; rejects `..`, NUL, drive letters, leading `/`
- [x] Id slugs for default `{version}/{slug}.html` avoid Windows-illegal `:` in UIDs
- [x] Line-ending unfold uses `\\r?\\n` so CRLF ICS files parse on Windows

## Issues

None blocking.

### Notes (non-blocking)

- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `icalendar`. That is the SPI allow-list; REST round-trip tests stay a follow-on (same as rss-atom #3881).
- Git remotes rejected for any non-`git-filesystem` kind. Credential-like extra properties and cloud `rootPath` URLs are fail-closed for `icalendar` (peer of rss-atom / object-storage).
- Exhaustive `switch` on `VirtualSiteSourceType` lives only in `PSVirtualSiteSourceFactory` (updated). Grep found no reverse-dep exhaustive switches in rest / sitemanage / perc-toolkit.
- RFC 5545 unfolding and TEXT unescape (`\\n`, `\\,`, `\\;`, `\\\\`) are implemented in-tree (no ical4j dependency).

## Tests

- `PSIcalendarVirtualSiteSourceTest` — local `.ics` fixture, default `calendar.ics`, folded DESCRIPTION, fail-closed blank UID/SUMMARY, empty calendar, unknown load, duplicate UIDs, `icalendar.url` rejected, path traversal, Windows/Unix absolute calendar files, no parse cache, factory + peers unchanged, unknown `sql-api` still rejected, build emits HTML (`pageCount > 0`), YAML `icalendar.file`, rebuild after edit
- `PSVirtualSiteHelperTest` — allow-list includes `icalendar`; safe root; `remoteUrl`, credentials, and cloud `rootPath` rejected; unknown `sql-api` lists `icalendar`
- `VirtualSiteConfigLoaderTest` — `icalendar:` scalar fails closed; second load sees current `icalendar.file`

## Change-class companions

New Virtual Site source adapter: enum + impl + factory + build-service selection + CLI + helper allow-list + product-docs + unit tests. REST persist / Build / Preview / Publish and Developer Sites chrome are follow-on slices. No Playwright (no UI).

Standalone: `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS. Tests run: 2608, Failures: 0, Errors: 0, Skipped: 247.
