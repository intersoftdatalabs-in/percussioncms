# Erlang review — #4325 REST File Explorer allow-listed browse

**Branch:** `feat/issue-4325-file-explorer-browse`  
**Base:** `origin/main`  
**Date:** 2026-09-05  
**Reviewer:** Erlang (pre-commit, implementer-independent)

## Summary

New Admin REST catalog `/services/fileexplorer` lists operator-configured allow-listed
roots and immediate children by relative path. rest owns resource/DTOs/`IFileExplorerAdaptor`;
sitemanage `FileExplorerAdaptor` walks NIO paths only after root-id and relative-path
validation. Distinct from SY-02 `/serverconfigs` and SY-05 application files.

Memory patterns hit: change-class completeness (Spring `TestFileExplorerAdaptor`, CXF
`restFileExplorerResource` ref, sitemanage adaptor tests); `PSProperties` for
`PSServer.ms_serverProps`; NIO `Path`/`Files` (no OS-separator filesystem joins);
unsafe-path errors do not echo raw input.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None blocking.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` **filesystem** construction (API `relativePath` uses `/` by contract)
- [x] Listing/resolve uses `Path.resolve` / `Files.newDirectoryStream` / `toAbsolutePath().normalize()`
- [x] Tests use `@TempDir` / `Path`, not Unix-only absolute roots
- [x] Configured roots: `Path.of` + `rxDir.resolve` for relative operator paths
- [x] Client `..`, absolute, drive, UNC rejected **before** `Path.normalize` collapse
- [x] Containment: `candidate.startsWith(trustedRoot)` after normalize

## Change-class closure

| Companion | Present |
|-----------|---------|
| rest resource + DTOs + adaptor interface | yes |
| Mockito `FileExplorerResourceTest` | yes (11) |
| Spring `TestFileExplorerAdaptor` | yes (`@Component` `@Lazy`) |
| sitemanage `@PSSiteManageBean` adaptor + tests | yes (19) |
| CXF `rest-jax-rs` ref + catalog registration test | yes |
| product-docs REST + admin File Explorer | yes |
| Playwright | N/A (REST-only; siblings #4326/#4327) |

## Builds

- `cd rest && ../mvnw clean install` — BUILD SUCCESS; Tests run: 1142; `FileExplorerResourceTest` 11
- `cd projects/sitemanage && ../../mvnw clean install` — BUILD SUCCESS; Tests run: 2400, Skipped: 125; `FileExplorerAdaptorTest` 19
