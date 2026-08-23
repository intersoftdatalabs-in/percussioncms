# Erlang review: #3779 REST sql-database virtual/publish

**Date:** 2026-08-23  
**Branch:** `feat/issue-3779-sql-database-virtual-publish`  
**Stacked on:** cluster #3777 (`cluster/night-issue-20260823-sql-virtual-site`)  
**Scope:** uncommitted vs stacked cluster HEAD (SQL publish adaptor/resource tests + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor tests + REST resource tests + OpenAPI/docs + product-docs); no WebUI/Playwright (explicitly out of scope — Developer Sites Publish chrome is #3778).

## Summary

Proves `POST /sites/{nameOrId}/virtual/publish` for `virtual.sourceKind=sql-database`. Production publish already builds-then-copies via `SitesAdaptor.publishVirtualSite` → `PSVirtualSiteFilesystemPublisher` (kind-agnostic once SQL Build exists on cluster #3777). This slice closes the remaining proof/docs gap so SQL is not build-only: in-memory H2 adaptor tests copy assembled HTML under a dedicated `IPSSite.root`; injected `buildRunner` isolates the NIO copy (`_meta` skipped); `sql.queryFile` publish; unsafe Site root 400; Oracle/MySQL JDBC URLs 400; repository still cannot publish. REST resource tests cover SQL delegation + OpenAPI `jdbc:h2:mem:`. Product-docs 8.2 admin Sites/Publishing, developer REST/virtual-sites, and reference site-config document SQL Publish to the Site filesystem target.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] H2 fixture and publish roots use `@TempDir` + `Path` / `Files.createDirectories` / `Files.writeString` / `Path.resolve`
- [x] `queryFile` uses `Path.of("queries").resolve("pages.sql")` (portable); YAML logical path uses `/` (URL-style, not OS join)
- [x] Unsafe root uses `Path.of("a", "..", "..", "etc")` (peer CSV)
- [x] Tests do not assert Unix-only absolute path strings; HTML assertions use `contains`
- [x] Line-ending sensitive HTML assertions use `contains` on assembled text, not raw `\n` file equality
- [x] JDBC password in `_config.yaml` is a dummy test secret, asserted **not** present in published HTML

## Issues

None blocking.

### Notes (non-blocking)

- Stacked on unmerged cluster #3777 / PR #3777 (SQL SPI + REST sourceKind + REST Build). Production publish code did not need a new gate: `publishVirtualSite` already called `buildVirtualSite` then NIO-copied output.
- Developer Sites Publish chrome is out of scope (follow-on #3778). Rebuild-without-restart is #3780. Preview chrome is #3768.
- Oracle / MySQL live matrix remains out of scope (fail-closed 400).

## Tests

- `SitesAdaptorTest.publishVirtualSite_sqlDatabaseBuildsThenCopiesToSiteRoot` — H2 fixture → publish → `8.2/index.html` under Site root contains SQL Home; password not leaked; `_meta` not copied
- `SitesAdaptorTest.publishVirtualSite_sqlDatabaseInjectedBuildRunnerCopiesToSiteRoot` — injected runner writes HTML + `_meta`; only HTML copied
- `SitesAdaptorTest.publishVirtualSite_sqlDatabaseQueryFileBuildsThenCopiesToSiteRoot` — `sql.queryFile` NIO path
- `SitesAdaptorTest.publishVirtualSite_sqlDatabaseRejectsUnsafeSiteRoot` — remaining `..` after normalize → 400
- `SitesAdaptorTest.publishVirtualSite_sqlDatabaseOracleAndMysqlUrl400` — live RDBMS URLs 400
- Existing git/csv publish + repository-reject tests unchanged
- `SitesResourceTest.publishVirtualSiteDelegatesSqlDatabase` + OpenAPI `sql-database` / `jdbc:h2:mem:`

## Change-class companions

REST Virtual Site publish for a new source kind: adaptor behavioral tests (H2 fixture + injected runner + reject paths), rest resource tests + OpenAPI/javadoc, product-docs 8.2 REST/admin/developer/reference. No public method signature / `final` API change (C2 none). No WebUI.

## Builds

- `cd rest && ../mvnw.cmd clean install` — **BUILD SUCCESS**. Tests run: 553, Failures: 0 (`SitesResourceTest` 33/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — **BUILD SUCCESS**. Tests run: 1336, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 77/0)
- `scripts/ci-smoke-product-docs.bat` — OK (8.2/index.html)

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
