# Erlang review — #4415 json-schema IPSVirtualSiteSource CLI assemble

**Branch:** `feat/issue-4415-json-schema-virtual-site`  
**Scope:** uncommitted vs `HEAD` plus commits not in `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for fail-closed remote/path; portable NIO Path/Files; change-class companions from OpenAPI/GraphQL SDL peers (SPI + factory + `_config.yaml` spec + helper allow-list + product-docs 8.2)

## Summary

Adds `VirtualSiteSourceType.JSON_SCHEMA` (`json-schema`) with local `schema.json` / `_config.yaml` `jsonschema.file` SPI (`PSJsonSchemaVirtualSiteSource`, NIO `Path`/`Files`, factory allow-list). Maps `properties` / `$defs` (title/description fallback) so CLI assemble `pagesWritten > 0`. Fail-closed: `jsonschema.url`, leftover `virtual.remoteUrl`, credentials, cloud `rootPath`, remote `$ref`/`$id` HTTP. Stateless re-read. product-docs 8.2 `developer/virtual-sites.md` + `reference/site-config.md` only. REST/WebUI stay siblings #4416/#4417.

## Issues

None (hard-gate).

## Cross-platform path checklist

- [x] No `".../" +` / `"...\\" +` filesystem joins; `Path.of` / `resolve` / `Files`
- [x] Tests do not assert Unix-only OS `toString()` paths; expected pages use `Path.of("8.2", "sku-1.html")`
- [x] Absolute file rejection covered on Windows (`C:/…`) and Unix (`/etc/…`) via `@EnabledOnOs`
- [x] Temp trees via JUnit `@TempDir`
- [x] Line-ending-insensitive HTML/body `contains` assertions

## Build evidence

`cd system && ../mvnw clean install` → **BUILD SUCCESS**. Tests run: 3044, Failures: 0, Errors: 0, Skipped: 251.
