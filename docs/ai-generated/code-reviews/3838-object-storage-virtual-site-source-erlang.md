# Erlang review: #3838 object-storage IPSVirtualSiteSource

**Date:** 2026-08-26  
**Branch:** `feat/issue-3838-object-storage-spi`  
**Scope:** uncommitted vs `HEAD` / `origin/main` (system virtualsite SPI + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (SPI + factory + tests + product-docs); no REST/UI companions required (explicitly out of scope #3839); no cloud secrets / AWS SDK.

## Summary

Adds `VirtualSiteSourceType.OBJECT_STORAGE` (`object-storage`), `PSObjectStorageVirtualSiteSource` (discover+load of a local directory as an object-key bucket), optional `VirtualSiteConfig.ObjectsSpec` parsed from `_config.yaml` `objects.keys`, and factory wiring through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. Object keys are portable relative paths under `virtual.rootPath` (NIO `Path` / `Files`; no remaining `..`, no absolute roots). Markdown / HTML / JSON catalog assemble `id`/`title`/`body` like http-json / csv-filesystem. No AWS SDK, access keys, or network. Product-docs 8.2 document the SPI; REST persist remains sibling #3839.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction (object keys use `/` as logical separators then `Path.resolve`)
- [x] Object I/O uses `Path` / `Files.readString` / `Files.walk` / `Files.isRegularFile`
- [x] Temp trees via JUnit `@TempDir`
- [x] Windows vs Unix absolute-root assertions behind `@EnabledOnOs`
- [x] Containment: resolved keys `startsWith` normalized site root; rejects `..`, NUL, drive letters, leading `/`
- [x] Line-ending frontmatter parse normalizes `\r\n` (shared `VirtualFrontmatterParser`)

## Issues

None blocking.

### Notes (non-blocking)

- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `object-storage`. That is the SPI allow-list; REST round-trip tests are #3839.
- Git remotes rejected for any non-`git-filesystem` kind (same helper branch as CSV/SQL/HTTP JSON).
- Exhaustive `switch` on `VirtualSiteSourceType` lives only in `PSVirtualSiteSourceFactory` (updated). Grep found no reverse-dep exhaustive switches (`SitesAdaptor` uses `if` on CSV).

## Tests

- `PSObjectStorageVirtualSiteSourceTest` — discover/load Markdown+HTML+JSON catalog, stem id, `objects.keys` subset, fail-closed missing keys / blank id/title / unknown load / `..` / absolute / duplicates, no parse cache, factory + peers unchanged, unknown `sql-api` still rejected, build emits HTML, YAML keys, Windows/Unix temp roots
- `PSVirtualSiteHelperTest` — allow-list includes `object-storage`; remoteUrl and `..` root rejected; unknown `sql-api` still lists the kind
- `VirtualSiteConfigLoaderTest` — `objects:` scalar fails closed

## Change-class companions

New Virtual Site source adapter: enum + impl + factory + build-service selection + CLI + helper allow-list + product-docs + unit tests. REST persist is sibling #3839. Developer Sites chrome is later. No Playwright (no UI).

Standalone: `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS. Tests run: 2446, Failures: 0, Errors: 0, Skipped: 245.
