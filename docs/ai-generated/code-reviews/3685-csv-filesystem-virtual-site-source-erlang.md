# Erlang review: #3685 csv-filesystem IPSVirtualSiteSource

**Date:** 2026-08-20  
**Branch:** `feat/issue-3685-csv-filesystem-virtual-site-source`  
**Scope:** uncommitted vs `HEAD` / `origin/main` (system virtualsite SPI + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (SPI + factory + tests + product-docs); no REST/UI companions required (explicitly out of scope #3686/#3687).

## Summary

Adds `VirtualSiteSourceType.CSV_FILESYSTEM` (`csv-filesystem`), `PSCsvFilesystemVirtualSiteSource` (discover+load), RFC 4180 `VirtualCsvParser`, and `PSVirtualSiteSourceFactory` wired through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. Required CSV columns `id`, `title`, `body`; optional `path`/`order`; fail-closed on missing columns, blank id/title, duplicates, unsafe paths. Product-docs 8.2 admin/developer/reference describe offline CSV build; REST Build remains git-filesystem.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction (logical CSV `path` uses `/` as href-style separators then `Path.of`/`resolve`)
- [x] Discover/load uses `Path` / `Files.readString` / `Files.walk`
- [x] Temp trees via JUnit `@TempDir`
- [x] Windows vs Unix absolute path assertions behind `@EnabledOnOs`
- [x] Containment: CSV files and version roots `startsWith` normalized site root; `path` column rejects `..`, NUL, drive letters, leading `/`
- [x] Line endings: parser normalizes `\r\n` / `\r`; tests cover CRLF

## Issues

None blocking.

### Notes (non-blocking)

- REST `SitesAdaptor` still rejects non-`git-filesystem` on Build. Documented and in-scope for #3686, not this slice.
- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `csv-filesystem`. That is the SPI allow-list, not REST Build.

## Tests

- `VirtualCsvParserTest` — required columns, quotes/newlines, CRLF, BOM, ragged/unclosed
- `PSCsvFilesystemVirtualSiteSourceTest` — discover/load temp tree, fail-closed paths, no cache, factory + build HTML, OS-specific roots
- `PSVirtualSiteHelperTest` — allow-list includes both kinds; csv remoteUrl rejected

## Change-class companions

New Virtual Site source adapter: enum + impl + factory + build-service selection + CLI + helper allow-list + product-docs + unit tests. REST/UI slices filed separately.
