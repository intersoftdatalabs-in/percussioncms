# Erlang review — #3837 http-json rebuild reads current JSON

**Branch:** `fix/issue-3837-http-json-rebuild-current`  
**Date:** 2026-08-26  
**Reviewer:** Erlang (pre-commit, independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for changed logic; portable `Path`/`Files` (no Unix-only roots); product-docs companion for operator-facing rebuild note; no incomplete rest/sitemanage adaptor cache (none present)

## Summary

`PSHttpJsonVirtualSiteSource` already re-reads local catalog bytes (`Files.readString`) and HTTP catalog bodies on every discover/load; `VirtualSiteConfigLoader` always opens `_config.yaml` from disk; `PSVirtualSiteBuildService.build` reloads config per invocation. This slice locks the no-stale-parse-cache contract in Javadoc (peer of sql-database #3780 / csv-filesystem #3708 / git-filesystem #3367), proves same-JVM JSON fixture / `_config.yaml` / loopback HTTP catalog edit → second build emits new HTML, and documents that operators rebuild after a JSON source edit without a CMS JVM restart and without file watchers.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Test I/O uses `@TempDir` + `Path.resolve` + `Files.writeString` / `readString`
- [x] No Unix-only `/tmp` or Windows-only `C:\` hardcodes in new tests
- [x] No raw OS `toString()` path equality assertions
- [x] Line-ending sensitive checks use `contains` tokens, not whole-file `\n` equality
- [x] Loopback `HttpServer` is in-process (127.0.0.1) — not a Unix-only path

## Issues

None (hard-gate).

## Nits

- Two nearly parallel `secondBuildAfterHttpJson…` tests (`PSVirtualSiteBuildServiceTest` factory wiring vs `PSHttpJsonVirtualSiteSourceTest` same-instance source). Acceptable as the SPI + assemble proofs matching CSV #3708 / SQL #3780.
- Loopback rebuild test is extra vs the CSV/SQL peer (those have no HTTP catalog). Useful coverage; not sprawl.

## C2 API shape

Javadoc-only on existing public types; no `final`/`sealed` and no method/ctor signature change. Reverse-deps (`rest` / `sitemanage`) not required.

## Build

Standalone `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS**.  
Focused: `PSHttpJsonVirtualSiteSourceTest` Tests run: 24, Failures: 0, Skipped: 1 (Unix-only absolute catalog path). `PSVirtualSiteBuildServiceTest` Tests run: 13, Failures: 0. `VirtualSiteConfigLoaderTest` Tests run: 15, Failures: 0.  
Module Surefire: Tests run: 2429, Failures: 0, Errors: 0, Skipped: 244.
