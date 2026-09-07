# Erlang review — issue #4401 graphql-sdl persist + Developer Sites source chrome

**Date:** 2026-09-07  
**Branch:** `fix/issue-4401-graphql-sdl-persist`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** approve  

## Summary

Adds `VirtualSiteSourceType.GRAPHQL_SDL` (`graphql-sdl`) with a local `schema.graphql` / `_config.yaml` `graphql.file` SPI (`PSGraphQlSdlVirtualSiteSource`, NIO `Path`/`Files`, factory allow-list). REST GET/PUT `/sites/{nameOrId}/virtual` round-trips the kind; leftover `virtual.remoteUrl`, credentials, cloud `rootPath`, and `graphql.url` are 400. Developer Sites source-kind option + Vitest + Playwright C5. REST/UI Build/Preview/Publish stay later slices.

## Memory patterns hit

- Portable NIO `Path`/`Files` for local fixture roots (no hardcoded `/` filesystem joins).
- Fail-closed leftover remotes/credentials/cloud URLs on persist-only kinds.
- Change-class companions: SPI + REST + adaptor tests + Developer chrome + Playwright + product-docs in one PR.

## Cross-platform path checklist

- [x] Fixture resolve uses `Path.resolve` / `normalize` / `startsWith` (no `"/" +` filesystem construction)
- [x] Absolute Windows drive letters rejected as `graphql.file`
- [x] Tests use `@TempDir` + `Path.of`; OS-specific absolute-path tests are `@EnabledOnOs`
- [x] REST `rootPath` examples use `C:/…` strings as operator input, not OS joins

## Issues

None (no bugs, missing behavioral tests, or non-portable I/O).

## Evidence

- `system` / `rest` / `projects/sitemanage` / `WebUI` standalone `mvnw clean install` BUILD SUCCESS
- Playwright C5: 3 passed (`graphql-sdl` save/GET-roundtrip, live persist, source panel option list)
- console-clean=yes; server.log-clean=yes (no graphql ERROR/FATAL in test window)

> Co-Authored by Grok Build 1.0.13 using grok-4.6 with agent night-issue-prs.
