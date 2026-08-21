# Erlang review — #3708 csv-filesystem rebuild reads current CSV

**Branch:** `fix/issue-3708-csv-filesystem-rebuild-current`  
**Date:** 2026-08-21  
**Reviewer:** Erlang (pre-commit, independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for changed logic; portable `Path`/`Files` (no Unix-only roots); product-docs companion for operator-facing rebuild note; no incomplete rest/sitemanage adaptor cache (none present)

## Summary

`PSCsvFilesystemVirtualSiteSource` / `VirtualCsvParser` already `Files.readString` + parse on every discover/load (no path/mtime cache). `VirtualSiteConfigLoader` always opens `_config.yaml` from disk. `SitesAdaptor.runBuild` constructs a new `PSVirtualSiteBuildService` per REST build. This change locks the no-stale-parse-cache contract in Javadoc, proves same-JVM CSV/`_config.yaml` edit→rebuild emits new HTML, and documents that operators rebuild after a CSV edit without a CMS restart.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Test I/O uses `@TempDir` + `Path.resolve` + `Files.writeString` / `readString`
- [x] No Unix-only `/tmp` or Windows-only `C:\` hardcodes
- [x] No raw OS `toString()` path equality assertions
- [x] Line-ending sensitive checks use `contains` tokens, not whole-file `\n` equality

## Issues

None (hard-gate).

## Nits

- Two nearly parallel `secondBuildAfterCsvAndConfigEditEmitsUpdatedHtmlWithoutRestart` tests (`PSVirtualSiteBuildServiceTest` factory wiring vs `PSCsvFilesystemVirtualSiteSourceTest` same-instance source). Acceptable as the SPI + assemble proofs for peer #3367.

## C2 API shape

Javadoc-only on existing public types; no `final`/`sealed` and no method/ctor signature change. Reverse-deps (`rest` / `sitemanage`) not required.

## Build

Standalone `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**.  
Focused: `PSCsvFilesystemVirtualSiteSourceTest` Tests run: 15, Failures: 0, Skipped: 1 (Unix-only path). `PSVirtualSiteBuildServiceTest` Tests run: 11, Failures: 0. `VirtualSiteConfigLoaderTest` Tests run: 10, Failures: 0.  
Module Surefire: Tests run: 2339, Failures: 0, Errors: 0, Skipped: 242.
