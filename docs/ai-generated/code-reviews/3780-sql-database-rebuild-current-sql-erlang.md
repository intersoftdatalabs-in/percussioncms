# Erlang review — #3780 sql-database rebuild reads current SQL

**Branch:** `fix/issue-3780-sql-database-rebuild-current`  
**Date:** 2026-08-23  
**Reviewer:** Erlang (pre-commit, independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for changed logic; portable `Path`/`Files` (no Unix-only roots); product-docs companion for operator-facing rebuild note; no incomplete rest/sitemanage adaptor cache (none present)

## Summary

`PSSqlDatabaseVirtualSiteSource` already `Files.readString`s `sql.queryFile` (or uses the current inline `sql.query`) and re-runs the SELECT on every discover/load (no path/mtime or row cache). `VirtualSiteConfigLoader` always opens `_config.yaml` from disk. `PSVirtualSiteBuildService.build` reloads config then re-discovers. This change locks that no-stale-parse-cache contract (Javadoc), proves same-JVM `_config.yaml` + `queryFile` Path/Files edit→rebuild emits new HTML, and documents the operator rebuild path (no JVM restart; no file watchers).

Peer: csv-filesystem rebuild #3708 / cluster absorb #3777. Parent epic: #2678. Slice of #3780.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Test I/O uses `@TempDir` + `Path.resolve` + `Files.writeString` / `readString`
- [x] No Unix-only `/tmp` or Windows-only `C:\` hardcodes
- [x] No raw OS `toString()` path equality assertions
- [x] Line-ending sensitive checks use `contains` tokens, not whole-file `\n` equality
- [x] H2 is in-memory (`jdbc:h2:mem:` + `DB_CLOSE_DELAY=-1`); no file-H2 URLs

## Issues

None (hard-gate).

## Nits

- Two nearly parallel `secondBuildAfterSqlConfigAndQueryFileEditEmitsUpdatedHtmlWithoutRestart` tests (`PSVirtualSiteBuildServiceTest` factory wiring vs `PSSqlDatabaseVirtualSiteSourceTest` same-instance source). Acceptable as the SPI + assemble proofs for peer #3708 / #3367.

## C2 API shape

Javadoc-only on existing public types; no `final`/`sealed` and no method/ctor signature change. Reverse-deps (`rest` / `sitemanage`) not required.

## Build

Focused: `PSSqlDatabaseVirtualSiteSourceTest` Tests run: 25, Failures: 0, Skipped: 1 (Unix-only absolute queryFile). `PSVirtualSiteBuildServiceTest` Tests run: 12, Failures: 0. `VirtualSiteConfigLoaderTest` Tests run: 13, Failures: 0.  
Product-docs: `scripts\ci-smoke-product-docs.bat` → **OK** (23 pages).  
Standalone `cd system && ../mvnw.cmd clean install` → **BUILD SUCCESS** (2026-08-23). Surefire: Tests run: 2370, Failures: 0, Errors: 0, Skipped: 243.
