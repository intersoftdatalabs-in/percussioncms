# Erlang review: #3733 sql-database IPSVirtualSiteSource (H2)

**Date:** 2026-08-22  
**Branch:** `feat/issue-3733-sql-database-virtual-site-source`  
**Scope:** uncommitted vs `HEAD` / `origin/main` (system virtualsite SPI + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (SPI + factory + tests + product-docs); no REST/UI companions required (explicitly out of scope #3734/#3735); H2-only JDBC allow-list (no live Oracle/MySQL/SQL Server).

## Summary

Adds `VirtualSiteSourceType.SQL_DATABASE` (`sql-database`), `PSSqlDatabaseVirtualSiteSource` (discover+load), `VirtualSiteConfig.SqlSpec` parsed from `_config.yaml` `sql:`, and factory wiring through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. Required query columns `id`, `title`, `body` (optional mapped names, `path`/`order`/`version`); fail-closed on missing columns, blank id/title, duplicates, unsafe paths, non-SELECT SQL, and non-`jdbc:h2:mem:` URLs. Product-docs 8.2 admin/developer/reference describe the H2 SPI; REST/UI remain sibling slices.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction (queryFile uses `/` as logical separators then `Path.resolve`)
- [x] Query file I/O uses `Path` / `Files.readString` / `Files.isRegularFile`
- [x] Temp trees via JUnit `@TempDir`
- [x] Windows vs Unix absolute queryFile assertions behind `@EnabledOnOs`
- [x] Containment: queryFile `startsWith` normalized site root; rejects `..`, NUL, drive letters, leading `/`
- [x] JDBC URLs are not filesystem joins (`jdbc:h2:mem:` only)

## Issues

None blocking.

### Notes (non-blocking)

- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `sql-database`. That is the SPI allow-list; REST round-trip tests are #3734.
- Git remotes rejected for any non-`git-filesystem` kind (CSV message still contains `csv-filesystem`).
- Query is operator-configured SELECT executed via `PreparedStatement` (no bind params). Fail-closed: single SELECT, no extra statements, no CSVREAD/CALL/SCRIPT/DML, no `INIT`/`RUNSCRIPT` in the JDBC URL. Password is not logged (JDBC URL redacted at first `;`).

## Tests

- `PSSqlDatabaseVirtualSiteSourceTest` — discover/load in-memory H2, mapped columns, fail-closed paths/URLs/SQL, no row cache, factory + build HTML, portable queryFile, OS-specific absolute paths
- `PSVirtualSiteHelperTest` — allow-list includes `sql-database`; SQL remoteUrl rejected; unknown `sql-api` still 400

## Change-class companions

New Virtual Site source adapter: enum + impl + factory + build-service selection + CLI + helper allow-list + product-docs + unit tests. REST/UI slices filed separately (#3734/#3735).

Standalone: `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS.
