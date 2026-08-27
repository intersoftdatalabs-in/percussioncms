# Erlang review — #3915 REST rss-atom virtual/build

**Date:** 2026-08-27  
**Branch:** `feat/issue-3915-rss-atom-virtual-build`  
**Scope:** uncommitted rest + sitemanage + product-docs 8.2 vs `origin/main`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor + resource tests + Spring stub comment + product-docs REST/admin); no WebUI/Playwright required (explicitly out of scope persist PUT/GET #3888 / chrome #3889 / Preview/Publish).

## Summary

`POST /sites/{nameOrId}/virtual/build` already selected `PSVirtualSiteBuildService.forSourceType` for allow-listed kinds (SPI #3881 on `main`). This slice proves rss-atom REST Build: adaptor tests (`pagesWritten > 0` on a local RSS fixture, missing feed / missing `_config.yaml` / unsafe path / leftover `virtual.remoteUrl` / cloud `rootPath` / credential properties 400), resource fixture + OpenAPI `rss-atom` local/loopback guard, unknown kinds still 400, git/CSV/SQL/http-json/object-storage unchanged. Product-docs 8.2 REST/admin/developer document REST Build from a local feed file. Persist PUT/GET is sibling #3888 (not re-implemented). Preview/Publish REST and Developer Sites chrome stay later slices. No live remote feeds, no credentials on the REST envelope.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] RSS fixtures use `Path` / `Files.createDirectories` / `Files.writeString` / `@TempDir`
- [x] Output HTML asserted via `out.resolve("8.2").resolve("home.html")` (not OS separator strings)
- [x] Unsafe path uses `Path.of("a", "..", "..", "etc")` (portable)
- [x] Line-ending sensitive HTML assertions use `contains`, not raw `\n` file equality

## Issues

None blocking.

### Notes (non-blocking)

- Resource tests mock the adaptor (peer object-storage #3857); real HTML emit is in `SitesAdaptorTest` and system `PSRssAtomVirtualSiteSourceTest` (already on `main`).
- Production adaptor had no rss-atom exclusion: factory + helper already allow-listed the kind. This slice is OpenAPI, tests, docs, and `loadBuildConfig` javadoc.
- Publish still calls `buildVirtualSite`; this PR does not claim REST Publish or Preview for rss-atom (out of scope).
- Persist PUT/GET tests stay on sibling #3888 / PR #3898.

## Tests

- `SitesAdaptorTest` — local RSS fixture REST Build HTML (`pagesWritten > 0`); missing feed 400; missing `_config.yaml` 400; unsafe `rootPath` 400; leftover `virtual.remoteUrl` 400; cloud `rootPath` 400; credential property 400; unknown kind 400; existing git/CSV/SQL/http-json/object-storage tests
- `SitesResourceTest` — temp rss-atom `_config.yaml` + `feed.xml` fixture delegation; OpenAPI mentions `rss-atom` and local/loopback in the build path block
- `SitesTestAdaptor` — Spring stub comment covers REST Build for `rss-atom` (no new adaptor interface)
- System SPI tests already cover factory + `forSourceType(RSS_ATOM)` emit (merged #3881)

## Change-class companions

REST virtual/build for a new source kind (peer #3857 object-storage / #3806 http-json): factory wiring (already on main), adaptor proof, resource OpenAPI, sitemanage + rest tests, product-docs 8.2. No WebUI (later chrome slice). No new adaptor interface. Persist PUT/GET not duplicated.

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 656, Failures: 0 (`SitesResourceTest` 48)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1643, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 117)
- C2 N/A (no `final`/`sealed`/signature change). Reverse-dep `projects/sitemanage` still clean-installed as `ISiteAdaptor` implementer.
