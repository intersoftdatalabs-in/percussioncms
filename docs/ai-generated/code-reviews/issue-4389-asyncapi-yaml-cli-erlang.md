# Erlang review: #4389 asyncapi-yaml SPI/CLI

**Scope:** uncommitted + `feat/issue-4389-asyncapi-yaml-cli` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for new logic; portable Path/Files; fail-closed remotes; change-class companions from `PSOpenApiYamlVirtualSiteSource` peer

## Summary

Adds local AsyncAPI 2/3 YAML Virtual Site source (`asyncapi-yaml`) as a peer of OpenAPI YAML. Factory allow-list, `_config.yaml` `asyncapi.file` / default `asyncapi.yaml`, channel/operation mapping with `info` fallback, fail-closed `asyncapi.url` / remote `$ref` / credentials / cloud `rootPath`. Product-docs only `virtual-sites.md` and `site-config.md` (no REST/WebUI/Playwright thrash paths).

## Issues

None that block.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction (logical `/` only for YAML keys and URL detection)
- [x] Path resolve uses `Path` / `Files.readString`
- [x] Absolute Windows and Unix `asyncapi.file` rejected in tests (`@EnabledOnOs`)
- [x] Temp fixtures via JUnit `@TempDir`
- [x] Relative-path compare uses `Path.of("8.2", "…")`

## Companions

Peer of `PSOpenApiYamlVirtualSiteSource`: source + factory enum + helper local-only + config loader spec + helper tests + SPI/CLI assemble test (`pagesWritten` via `pageCount() > 0`) + product-docs. REST/WebUI/Playwright deferred to #4390/#4391 as specified.

C1: `cd system && ../mvnw clean install` BUILD SUCCESS, Tests run: 2984, Failures: 0.  
C2: added enum constant; only exhaustive switch is `PSVirtualSiteSourceFactory` (updated). No `final` type change. Downstream rest/sitemanage not rebuilt (no adaptor/API signature change in this slice).
