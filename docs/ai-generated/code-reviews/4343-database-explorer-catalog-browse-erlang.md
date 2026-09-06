# Erlang review — #4343 Database Explorer catalog browse

**Branch:** `feat/issue-4343-database-explorer-catalog-browse`  
**Base:** `origin/main`  
**Date:** 2026-09-05  
**Reviewer:** Erlang (pre-commit, implementer-independent)

## Summary

Admin-only read-only JDBC catalog browse for Developer Database Explorer
(Workbench §12.2): allow-listed datasources → TABLE/VIEW. rest owns
resource/DTOs/`IDatabaseExplorerAdaptor`; sitemanage `DatabaseExplorerAdaptor`
opens JDBC metadata only after catalog-id allow-list checks. Distinct from
File Explorer (`/fileexplorer`). Non-allow-listed names are HTTP 400. Wire
DTOs omit JDBC URLs and credentials.

Memory patterns hit: change-class completeness (Spring `TestDatabaseExplorerAdaptor`,
CXF `restDatabaseExplorerResource` ref, `CatalogRestJaxrsRegistrationTest`,
sitemanage adaptor tests, WebUI panel/Vitest, Playwright surface, product-docs);
`PSProperties` for `PSServer.ms_serverProps`; URL `/` separators only (no
filesystem path joins).

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None blocking.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` **filesystem** construction
- [x] REST/SPA URLs use `/` (URL contract)
- [x] Catalog ids and SQL idents validated with regex (no OS path tokens)
- [x] H2 in-memory adaptor test uses JDBC URL, not OS temp paths
- [x] Playwright REST helpers join URL segments, not `File.separator`

## Change-class closure

| Companion | Present |
|-----------|---------|
| rest resource + DTOs + adaptor interface | yes |
| Mockito `DatabaseExplorerResourceTest` | yes (12) |
| Spring `TestDatabaseExplorerAdaptor` | yes (`@Component` `@Lazy`) |
| sitemanage `@PSSiteManageBean` adaptor + tests | yes (14) |
| CXF `rest-jax-rs` ref + catalog registration test | yes |
| WebUI panel + Vitest + paths/messages/allowlists | yes |
| perc-qa-automation Playwright + helper unit tests | yes |
| product-docs admin + developer REST | yes |
