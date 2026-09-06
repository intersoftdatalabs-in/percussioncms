# Erlang review: #4341 llms-txt IPSVirtualSiteSource CLI assemble

**Branch:** `feat/issue-4341-llms-txt-virtual-site-source`

Independent pre-commit review of the llms-txt Virtual Site SPI/CLI slice (parent #2678).

## Change class

Adds `VirtualSiteSourceType.LLMS_TXT` (`llms-txt`), `PSLlmsTxtVirtualSiteSource` (discover+load from a local `llms.txt` under a portable-safe `virtual.rootPath`), optional `VirtualSiteConfig.LlmsSpec` parsed from `_config.yaml` `llms:`, and factory wiring through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. Default file `llms.txt`; `llms.file` overrides; `llms.url` is rejected (no live HTTP fetch). Each markdown list link maps `id`/`title`/`body`; a fixture with no links still emits one page. Remote/cloud link hrefs, Git `virtual.remoteUrl`, credentials, and cloud `rootPath` URLs fail closed. Product-docs 8.2 developer/virtual-sites + reference/site-config only. REST persist and Developer Sites chrome stay later slices.

## Gates

- [x] Bugs: fail-closed on empty fixture, blank link title, `llms.url`, remote/cloud hrefs, traversal, absolute `llms.file`
- [x] Portable Path/Files; Windows vs Unix absolute `llms.file` behind `@EnabledOnOs`
- [x] Behavioral tests for discover/load, factory peers unchanged, helper remoteUrl/credentials/cloud root, same-JVM Path/Files re-read, CLI assemble via `forSourceType` (`pageCount > 0`)
- [x] Exhaustive `switch` on `VirtualSiteSourceType` lives only in `PSVirtualSiteSourceFactory` (updated). Grep found no reverse-dep exhaustive switches in rest / sitemanage / perc-toolkit.
- [x] Product-docs 8.2 developer/virtual-sites + reference/site-config; REST persist not claimed
- [x] Standalone `cd system && ../mvnw clean install` BUILD SUCCESS (Tests run: 2921, Failures: 0, Errors: 0, Skipped: 247)
- [x] Cross-platform path checklist: no hardcoded `/` filesystem joins; `Path.of` / `resolve` / `Files.readString`; Windows drive-letter not treated as URI scheme

## Recommendation

**approve** — May commit/push: yes

## Notes

- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `llms-txt`. That is the SPI allow-list; REST round-trip tests stay a follow-on (same as robots-txt #4340).
- Git remotes rejected for any non-`git-filesystem` kind. Credential-like extra properties and cloud `rootPath` URLs are fail-closed for `llms-txt`.
- Markdown list hrefs are documented in the page body and never fetched.

Memory patterns hit: fail-closed local-only Virtual Site adapters; NIO Path portable roots; same-JVM Files re-read; no THRASH_PATH overlap with File Explorer PRs.
