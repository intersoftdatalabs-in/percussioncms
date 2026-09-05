# Erlang review: #4340 robots-txt IPSVirtualSiteSource CLI assemble

**Branch:** `fix/issue-4340-robots-txt-virtual-site-source`

Independent pre-commit review of the robots-txt Virtual Site SPI/CLI slice (parent #2678).

## Change class

Adds `VirtualSiteSourceType.ROBOTS_TXT` (`robots-txt`), `PSRobotsTxtVirtualSiteSource` (discover+load from a local `robots.txt` under a portable-safe `virtual.rootPath`), optional `VirtualSiteConfig.RobotsSpec` parsed from `_config.yaml` `robots:`, and factory wiring through `PSVirtualSiteBuildService.forSourceType` / CLI 4th arg. Default file `robots.txt`; `robots.file` overrides; `robots.url` is rejected (no live crawl). Each `User-agent` group maps `id`/`title`/`body`; a fixture with no `User-agent` still emits one page. Remote `Sitemap:` schemes, Git `virtual.remoteUrl`, credentials, and cloud `rootPath` URLs fail closed. Product-docs 8.2 developer/virtual-sites + reference/site-config only. REST persist and Developer Sites chrome stay later slices.

## Gates

- [x] Bugs: fail-closed on empty fixture, blank User-agent, `robots.url`, remote/cloud `Sitemap:`, traversal, absolute `robots.file`
- [x] Portable Path/Files; Windows vs Unix absolute `robots.file` behind `@EnabledOnOs`
- [x] Behavioral tests for discover/load, factory peers unchanged, helper remoteUrl/credentials/cloud root, same-JVM Path/Files re-read, CLI assemble via `forSourceType` (`pageCount > 0`)
- [x] Exhaustive `switch` on `VirtualSiteSourceType` lives only in `PSVirtualSiteSourceFactory` (updated). Grep found no reverse-dep exhaustive switches in rest / sitemanage / perc-toolkit.
- [x] Product-docs 8.2 developer/virtual-sites + reference/site-config; REST persist not claimed
- [x] Standalone `cd system && ../mvnw clean install` BUILD SUCCESS (Tests run: 2892, Failures: 0)
- [x] Cross-platform path checklist: no hardcoded `/` filesystem joins; `Path.of` / `resolve` / `Files.readString`; Windows drive-letter not treated as URI scheme

## Recommendation

**approve** — May commit/push: yes

## Notes

- Adding an enum constant expands `PSVirtualSiteHelper` allow-list, so `PUT …/virtual` can persist `robots-txt`. That is the SPI allow-list; REST round-trip tests stay a follow-on (same as sitemap-xml #4113 / icalendar #3986).
- Git remotes rejected for any non-`git-filesystem` kind. Credential-like extra properties and cloud `rootPath` URLs are fail-closed for `robots-txt`.
- `Sitemap:` in the fixture is documented in the page body and never fetched.

Memory patterns hit: fail-closed local-only Virtual Site adapters; NIO Path portable roots; same-JVM Files re-read; no THRASH_PATH overlap with File Explorer PRs.
