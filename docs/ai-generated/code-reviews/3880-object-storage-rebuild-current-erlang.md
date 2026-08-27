# Erlang review — #3880 object-storage rebuild reads current object keys

**Branch:** `fix/issue-3880-object-storage-rebuild-current`  
**Date:** 2026-08-27  
**Reviewer:** Erlang (pre-commit, independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for changed logic; portable `Path`/`Files` (no Unix-only roots); product-docs companion for operator-facing rebuild note; no incomplete rest/sitemanage adaptor cache (none present)

## Summary

`PSObjectStorageVirtualSiteSource` already `Files.readString`s Markdown / HTML / JSON object keys on every discover/load (no path/mtime parse cache). `VirtualSiteConfigLoader` always opens `_config.yaml` from disk (including `objects.keys`). `PSVirtualSiteBuildService.build` reloads config then re-discovers. This slice locks that no-stale-parse-cache contract (Javadoc), proves same-JVM object-key / `_config.yaml` Path/Files edit→rebuild emits new HTML, and documents the operator rebuild path (no JVM restart; no file watchers).

Peer: http-json rebuild #3837 / sql-database #3780 / csv-filesystem #3708 / git-filesystem #3367. Parent epic: #2678.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Test I/O uses `@TempDir` + `Path.resolve` + `Files.writeString` / `readString`
- [x] No Unix-only `/tmp` or Windows-only `C:\` hardcodes in new tests
- [x] No raw OS `toString()` path equality assertions
- [x] Line-ending sensitive checks use `contains` tokens, not whole-file `\n` equality
- [x] Object keys in YAML use logical `/` (URI-style keys), not OS separators

## Issues

None (hard-gate).

## Nits

- Two nearly parallel `secondBuildAfterObject…` tests (`PSVirtualSiteBuildServiceTest` factory wiring vs `PSObjectStorageVirtualSiteSourceTest` same-instance source). Acceptable as the SPI + assemble proofs matching HTTP JSON #3837 / CSV #3708 / SQL #3780.
- Factory test also proves `_config.yaml` `objects.keys` is honored on the second build (`pageCount` 2→1). Extra vs peers; useful, not sprawl.

## C2 API shape

Javadoc-only on existing public types; no `final`/`sealed` and no method/ctor signature change. Reverse-deps (`rest` / `sitemanage`) not required.

## Build

Standalone `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**.  
Focused: `PSObjectStorageVirtualSiteSourceTest` Tests run: 18, Failures: 0, Skipped: 1 (Unix-only absolute key). `PSVirtualSiteBuildServiceTest` Tests run: 14, Failures: 0. `VirtualSiteConfigLoaderTest` Tests run: 17, Failures: 0.  
Module Surefire: Tests run: 2489, Failures: 0, Errors: 0, Skipped: 245.  
Product-docs: `scripts\ci-smoke-product-docs.bat` → **OK** (24 pages).
