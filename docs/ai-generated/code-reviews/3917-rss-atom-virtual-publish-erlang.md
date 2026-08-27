# Erlang review: #3917 REST rss-atom virtual/publish

**Date:** 2026-08-27  
**Branch:** `feat/issue-3917-rss-atom-virtual-publish`  
**Stacked on:** `origin/main` (rss-atom persist/build/preview via cluster #3923)  
**Scope:** uncommitted vs `origin/main` (rss-atom publish adaptor/resource tests + OpenAPI/javadoc + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor tests + REST resource tests + OpenAPI/docs + product-docs); no WebUI/Playwright (explicitly out of scope — Developer Sites Publish chrome stays later).

## Summary

Proves `POST /sites/{nameOrId}/virtual/publish` for `virtual.sourceKind=rss-atom`. Production publish already builds-then-copies via `SitesAdaptor.publishVirtualSite` → `PSVirtualSiteFilesystemPublisher` (kind-agnostic once rss-atom Build exists on main). This slice closes the remaining proof/docs gap so rss-atom is not persist/build/preview-only: local RSS 2.0 fixture adaptor tests copy assembled HTML under a dedicated `IPSSite.root`; injected `buildRunner` isolates the NIO copy (`_meta` skipped); leftover `virtual.remoteUrl` is 400; leftover credential properties are 400 (secret not echoed); unsafe Site root 400; unknown kinds remain 400; git/csv/sql/http-json/object-storage publish unchanged. REST resource tests cover rss-atom delegation + OpenAPI `rss-atom` on the publish path. Product-docs 8.2 admin Sites/Publishing, developer REST/virtual-sites, and reference site-config drop “later phase” wording for REST Publish while keeping Developer Sites Build/Preview/Publish chrome as a later slice. No live internet feeds.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] RSS fixture and publish roots use `@TempDir` + `Path` / `Files.createDirectories` / `Files.writeString` / `Path.resolve`
- [x] Unsafe root uses `Path.of("a", "..", "..", "etc")` (peer CSV/SQL/HTTP JSON/object-storage)
- [x] Tests do not assert Unix-only absolute path strings; HTML assertions use `contains`
- [x] Line-ending sensitive HTML assertions use `contains` on assembled text, not raw `\n` file equality
- [x] No live feed URLs or credentials on the REST envelope; leftover `virtual.remoteUrl` and credential properties are 400; secret value is not echoed

## Issues

None blocking.

### Notes (non-blocking)

- Production publish code did not need a new gate: `publishVirtualSite` already called `buildVirtualSite` then NIO-copied output. OpenAPI/javadoc now name `rss-atom` explicitly.
- Developer Sites Publish chrome for rss-atom is out of scope (later slice, peer of object-storage #3879).
- Live internet RSS / Atom feeds remain out of scope (`virtual.remoteUrl` / credentials 400).

## Tests

- `SitesAdaptorTest.publishVirtualSite_rssAtomBuildsThenCopiesToSiteRoot` — local RSS fixture → publish → `8.2/index.html` under Site root contains RSS Home; `_meta` not copied
- `SitesAdaptorTest.publishVirtualSite_rssAtomInjectedBuildRunnerCopiesToSiteRoot` — injected runner writes HTML + `_meta`; only HTML copied
- `SitesAdaptorTest.publishVirtualSite_rssAtomRejectsUnsafeSiteRoot` — remaining `..` after normalize → 400
- `SitesAdaptorTest.publishVirtualSite_rssAtomRemoteUrl400` — leftover Git remote on rss-atom → 400
- `SitesAdaptorTest.publishVirtualSite_rssAtomCredentialProperty400` — leftover credential property → 400; secret not in message
- `SitesAdaptorTest.publishVirtualSite_unknownSourceKind400` — `sql-api` remains 400
- Existing git/csv/sql/http-json/object-storage publish + repository-reject tests unchanged
- `SitesResourceTest.publishVirtualSiteDelegatesRssAtom` + OpenAPI `rss-atom` on the publish path block

## Change-class companions

REST Virtual Site publish for a new source kind: adaptor behavioral tests (local RSS fixture + injected runner + reject paths), rest resource tests + OpenAPI/javadoc, product-docs 8.2 REST/admin/developer/reference. No public method signature / `final` API change (C2 none). No WebUI.

## Builds

- `cd rest && ../mvnw.cmd clean install` — **BUILD SUCCESS**. Tests run: 669, Failures: 0 (`SitesResourceTest` 54/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — **BUILD SUCCESS**. Tests run: 1598, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 131/0)
- `scripts/ci-smoke-product-docs.bat` — OK (8.2/index.html)

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
