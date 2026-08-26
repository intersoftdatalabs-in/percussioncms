# Erlang review: #3818 REST http-json virtual/publish

**Date:** 2026-08-25  
**Branch:** `feat/issue-3818-http-json-virtual-publish`  
**Stacked on:** `origin/main` (cluster #3817 HTTP JSON REST Build/Preview + Developer Sites Build chrome)  
**Scope:** uncommitted vs `origin/main` (http-json publish adaptor/resource tests + OpenAPI/javadoc + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor tests + REST resource tests + OpenAPI/docs + product-docs); no WebUI/Playwright (explicitly out of scope — Developer Sites Preview chrome is #3819, Publish chrome is #3820).

## Summary

Proves `POST /sites/{nameOrId}/virtual/publish` for `virtual.sourceKind=http-json`. Production publish already builds-then-copies via `SitesAdaptor.publishVirtualSite` → `PSVirtualSiteFilesystemPublisher` (kind-agnostic once HTTP JSON Build exists on cluster #3817). This slice closes the remaining proof/docs gap so HTTP JSON is not build-only: local JSON fixture adaptor tests copy assembled HTML under a dedicated `IPSSite.root`; injected `buildRunner` isolates the NIO copy (`_meta` skipped); leftover `virtual.remoteUrl` is 400; unsafe Site root 400; repository still cannot publish. REST resource tests cover HTTP JSON delegation + OpenAPI `http-json` on the publish path. Product-docs 8.2 admin Sites/Publishing, developer REST/virtual-sites, and reference site-config drop “later phase” wording for REST Publish while keeping Developer Sites Preview/Publish chrome as a later slice.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] JSON fixture and publish roots use `@TempDir` + `Path` / `Files.createDirectories` / `Files.writeString` / `Path.resolve`
- [x] Unsafe root uses `Path.of("a", "..", "..", "etc")` (peer CSV/SQL)
- [x] Tests do not assert Unix-only absolute path strings; HTML assertions use `contains`
- [x] Line-ending sensitive HTML assertions use `contains` on assembled text, not raw `\n` file equality
- [x] Catalog URL/file stay in `_config.yaml`; no Authorization/API keys on the REST envelope

## Issues

None blocking.

### Notes (non-blocking)

- Production publish code did not need a new gate: `publishVirtualSite` already called `buildVirtualSite` then NIO-copied output. OpenAPI/javadoc now name `http-json` explicitly.
- Developer Sites Preview chrome is out of scope (#3819). Publish chrome is #3820.
- Remote authenticated JSON catalogs / secrets remain out of scope (`virtual.remoteUrl` 400).

## Tests

- `SitesAdaptorTest.publishVirtualSite_httpJsonBuildsThenCopiesToSiteRoot` — local JSON fixture → publish → `8.2/index.html` under Site root contains HTTP Home; `_meta` not copied
- `SitesAdaptorTest.publishVirtualSite_httpJsonInjectedBuildRunnerCopiesToSiteRoot` — injected runner writes HTML + `_meta`; only HTML copied
- `SitesAdaptorTest.publishVirtualSite_httpJsonRejectsUnsafeSiteRoot` — remaining `..` after normalize → 400
- `SitesAdaptorTest.publishVirtualSite_httpJsonRemoteUrl400` — leftover Git remote on http-json → 400
- Existing git/csv/sql publish + repository-reject tests unchanged
- `SitesResourceTest.publishVirtualSiteDelegatesHttpJson` + OpenAPI `http-json` on the publish path block

## Change-class companions

REST Virtual Site publish for a new source kind: adaptor behavioral tests (local JSON fixture + injected runner + reject paths), rest resource tests + OpenAPI/javadoc, product-docs 8.2 REST/admin/developer/reference. No public method signature / `final` API change (C2 none). No WebUI.

## Builds

- `cd rest && ../mvnw.cmd clean install` — **BUILD SUCCESS**. Tests run: 597, Failures: 0 (`SitesResourceTest` 40/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — **BUILD SUCCESS**. Tests run: 1554, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 93/0)
- `scripts/ci-smoke-product-docs.bat` — OK (8.2/index.html)

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
