# Erlang review: #4342 openapi-yaml IPSVirtualSiteSource CLI assemble

**Branch:** `feat/issue-4342-openapi-yaml-virtual-site-source`

Independent pre-commit review of the openapi-yaml Virtual Site SPI/CLI slice (parent #2678).

## Change class

Adds `VirtualSiteSourceType.OPENAPI_YAML` (`openapi-yaml`), `PSOpenApiYamlVirtualSiteSource` (discover+load from a local OpenAPI 3 `openapi.yaml` under a portable-safe `virtual.rootPath`), optional `VirtualSiteConfig.OpenApiSpec` parsed from `_config.yaml` `openapi:`, and factory wiring through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. Default file `openapi.yaml`; `openapi.file` overrides; `openapi.url` is rejected (no live HTTP spec fetch). Each path/operation maps `id`/`title`/`body`; a fixture with no operations still emits one page from `info`. Remote `$ref`, Git `virtual.remoteUrl`, credentials, and cloud `rootPath` URLs fail closed. Product-docs 8.2 developer/virtual-sites + reference/site-config only. REST persist and Developer Sites chrome stay later slices.

## Gates

- [x] Bugs: fail-closed on empty fixture, Swagger 2, `openapi.url`, remote `$ref`, traversal, absolute `openapi.file`
- [x] Portable Path/Files; Windows vs Unix absolute `openapi.file` behind `@EnabledOnOs`
- [x] Behavioral tests for discover/load, factory peers unchanged, helper remoteUrl/credentials/cloud root, same-JVM Path/Files re-read, CLI assemble via `forSourceType` (`pageCount > 0`)
- [x] Exhaustive `switch` on `VirtualSiteSourceType` lives only in `PSVirtualSiteSourceFactory` (updated). Grep found no reverse-dep exhaustive switches in rest / sitemanage / perc-toolkit.
- [x] Product-docs 8.2 developer/virtual-sites + reference/site-config; REST persist not claimed
- [x] Standalone `cd system && ../mvnw clean install` BUILD SUCCESS (Tests run: 2949, Failures: 0, Errors: 0, Skipped: 248)
- [x] Cross-platform path checklist: no hardcoded `/` filesystem joins; `Path.of` / `resolve` / `Files.readString`; Windows drive-letter not treated as URI scheme

## Recommendation

**approve** — May commit/push: yes

## Notes

- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `openapi-yaml`. That is the SPI allow-list; REST round-trip tests stay a follow-on (same as llms-txt #4341 / robots-txt #4340).
- Git remotes rejected for any non-`git-filesystem` kind. Credential-like extra properties and cloud `rootPath` URLs are fail-closed for `openapi-yaml`.
- OpenAPI `servers.url` values are not fetched; remote `$ref` is fail-closed.

Memory patterns hit: fail-closed local-only Virtual Site adapters; NIO Path portable roots; same-JVM Files re-read; no THRASH_PATH overlap with File Explorer / Problems panel PRs.
