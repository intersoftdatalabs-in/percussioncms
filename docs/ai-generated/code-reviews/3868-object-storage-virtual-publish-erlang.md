# Erlang review: #3868 REST object-storage virtual/publish

**Date:** 2026-08-26  
**Branch:** `feat/issue-3868-object-storage-virtual-publish`  
**Stacked on:** `origin/main` (object-storage SPI + REST GET/PUT persist)  
**Scope:** uncommitted vs `origin/main` (object-storage publish adaptor/resource tests + OpenAPI/javadoc + product-docs 8.2)  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

Memory patterns hit: portable Path/Files; behavioral tests for validation/rejection; change-class companions (adaptor tests + REST resource tests + OpenAPI/docs + product-docs); no WebUI/Playwright (explicitly out of scope — Developer Sites Build chrome is #3869, Preview chrome is #3870).

## Summary

Proves `POST /sites/{nameOrId}/virtual/publish` for `virtual.sourceKind=object-storage`. Production publish already builds-then-copies via `SitesAdaptor.publishVirtualSite` → `PSVirtualSiteFilesystemPublisher` (kind-agnostic once object-storage `IPSVirtualSiteSource` exists on main). This slice closes the remaining proof/docs gap so object-storage is not persist-only: local object-key `rootPath` fixture adaptor tests copy assembled HTML under a dedicated `IPSSite.root`; injected `buildRunner` isolates the NIO copy (`_meta` skipped); leftover `virtual.remoteUrl` is 400; unsafe Site root 400; unknown kinds remain 400; git/csv/sql/http-json publish unchanged. REST resource tests cover object-storage delegation + OpenAPI `object-storage` on the publish path. Product-docs 8.2 admin Sites/Publishing, developer REST/virtual-sites, and reference site-config drop “later phase” wording for REST Publish while keeping Developer Sites Build/Preview/Publish chrome as a later slice. No live S3/MinIO, IAM, or access keys.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Object-key fixture and publish roots use `@TempDir` + `Path` / `Files.createDirectories` / `Files.writeString` / `Path.resolve`
- [x] Unsafe root uses `Path.of("a", "..", "..", "etc")` (peer CSV/SQL/HTTP JSON)
- [x] Tests do not assert Unix-only absolute path strings; HTML assertions use `contains`
- [x] Line-ending sensitive HTML assertions use `contains` on assembled text, not raw `\n` file equality
- [x] No cloud URLs, IAM, or access keys on the REST envelope; leftover `virtual.remoteUrl` is 400

## Issues

None blocking.

### Notes (non-blocking)

- Production publish code did not need a new gate: `publishVirtualSite` already called `buildVirtualSite` then NIO-copied output. OpenAPI/javadoc now name `object-storage` explicitly.
- Dedicated REST Build/Preview tests and docs for object-storage remain slices #3857/#3858 (cluster #3867). This PR does not steal those surfaces.
- Developer Sites Build chrome is out of scope (#3869). Preview chrome is #3870. Publish chrome is a later phase.
- Live S3/MinIO / IAM / access keys remain out of scope (`virtual.remoteUrl` 400).

## Tests

- `SitesAdaptorTest.publishVirtualSite_objectStorageBuildsThenCopiesToSiteRoot` — local object-key fixture → publish → `8.2/index.html` under Site root contains Object Home; `_meta` not copied
- `SitesAdaptorTest.publishVirtualSite_objectStorageInjectedBuildRunnerCopiesToSiteRoot` — injected runner writes HTML + `_meta`; only HTML copied
- `SitesAdaptorTest.publishVirtualSite_objectStorageRejectsUnsafeSiteRoot` — remaining `..` after normalize → 400
- `SitesAdaptorTest.publishVirtualSite_objectStorageRemoteUrl400` — leftover Git remote on object-storage → 400
- `SitesAdaptorTest.publishVirtualSite_unknownSourceKind400` — `sql-api` remains 400
- Existing git/csv/sql/http-json publish + repository-reject tests unchanged
- `SitesResourceTest.publishVirtualSiteDelegatesObjectStorage` + OpenAPI `object-storage` on the publish path block

## Change-class companions

REST Virtual Site publish for a new source kind: adaptor behavioral tests (local object-key fixture + injected runner + reject paths), rest resource tests + OpenAPI/javadoc, product-docs 8.2 REST/admin/developer/reference. No public method signature / `final` API change (C2 none). No WebUI.

## Builds

- `cd rest && ../mvnw.cmd clean install` — **BUILD SUCCESS**. Tests run: 612, Failures: 0 (`SitesResourceTest` 43/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — **BUILD SUCCESS**. Tests run: 1547, Failures: 0, Skipped: 125 (`SitesAdaptorTest` 103/0)
- `scripts/ci-smoke-product-docs.bat` — OK (8.2/index.html)

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
