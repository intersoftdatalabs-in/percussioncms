# Erlang review: #3734 REST sql-database virtual.sourceKind

**Date:** 2026-08-22  
**Branch:** `feat/issue-3734-sql-database-sourcekind` (stacked on SPI #3733 / PR #3745)  
**Scope:** uncommitted rest + sitemanage + product-docs vs `HEAD`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: behavioral tests for PUT/GET/reject paths; rest↔sitemanage adaptor companions (resource tests + adaptor tests + existing Spring stub echo); product-docs 8.2 REST notes; no secrets on the REST envelope; portable NIO (`../outside` reject, `@TempDir` H2 fixture); no WebUI/Playwright (UI slice #3735).

## Summary

Allow-lists and round-trips `virtual.sourceKind=sql-database` on public Site REST GET/PUT. Helper/enum/factory already come from stacked SPI #3745. SitesAdaptor PUT/GET persist kind + root/siteKey; `sql-database` + `remoteUrl` and remaining `..` after NIO normalize stay 400; unknown kinds stay 400; csv/git tests unchanged. JDBC URL/user/query remain in `_config.yaml` (never on `VirtualSiteProperties`; empty test password; serial JSON asserts no `password` key). Because SPI is on the branch, REST Build is wired via existing `forSourceType` and proven with an in-memory H2 adaptor test. Product-docs 8.2 REST/admin/developer/reference updated. Developer Sites SQL chrome remains #3735.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] H2 fixture uses `Path` / `Files.createDirectories` / `Files.writeString` / `@TempDir`
- [x] PUT safety tests reuse peer `../outside` (logical remaining `..`, not OS-specific)
- [x] Example roots `C:/sql-docs` match csv-filesystem peer `C:/csv-docs` (property strings, not OS `toString()` assertions)
- [x] JDBC URLs are not filesystem joins (`jdbc:h2:mem:`)
- [x] Line-ending sensitive HTML assertions use `contains`, not raw `\n` file equality

## Issues

None blocking.

### Notes (non-blocking)

- JDBC connection fields are not new REST envelope properties; they live in `_config.yaml` (same SPI contract). Tests exercise safe H2 URL + `user: sa` with empty password.
- OpenAPI/ISiteAdaptor now document sql-database Build/preview/publish because factory already selects `PSSqlDatabaseVirtualSiteSource`. Developer Sites SQL source/Build/Publish chrome ships in stacked #3735 / #3759 (PR #3764 / #3766); REST last-build Preview is #3761.
- sitemanage test-scoped H2 is explicit (perc-system H2 is also test-scoped; not transitive).

## Tests

- `SitesAdaptorTest` — PUT/GET round-trip; reject `../outside`; reject `remoteUrl`; unknown `sql-adapter` still 400 (existing); Build writes HTML from in-memory H2
- `SitesResourceTest` — GET/PUT round-trip; unknown kind 400; OpenAPI strings mention sql-database
- `VirtualSitePropertiesSerialDeserialTest` — Jackson wrap round-trip; JSON has no `password`
- `SitesTestAdaptor` — existing Spring stub echoes PUT (shared MainTest context)

## Change-class companions

REST virtual.sourceKind slice (peer #3686 csv-filesystem): helper allow-list (SPI), rest DTO/OpenAPI, resource tests, sitemanage adaptor tests, product-docs 8.2. No new adaptor interface (existing `ISiteAdaptor`). No WebUI/Playwright in this slice.

Standalone: `cd rest && ../mvnw.cmd clean install` then `cd projects/sitemanage && ../../mvnw.cmd clean install` (system first for stacked SPI SNAPSHOT).
