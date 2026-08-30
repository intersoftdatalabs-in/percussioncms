# Erlang review — issue #4021 REST CD-14 content-type import

**Branch:** `feat/issue-4021-content-type-import`  
**Scope:** uncommitted vs HEAD (rest + sitemanage + product-docs 8.2 + gap map)  
**Date:** 2026-08-30  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class completeness (rest resource + adaptor interface + Spring stub + sitemanage impl + tests); Admin `IPSUserService.isAdminUser` 403; design-WS create/save/release; no stolen locks; Spring MainTest stubs

## Summary

Admin POST `/services/contenttypes/import` accepts Workbench-equivalent `ItemDefData` XML (same document as Workbench export / peer GET export in open PR #4020 — this PR does not take that export surface). sitemanage `ContentTypeAdaptor` parses via `PSItemDefinition`, creates through `IPSContentDesignWs.createContentTypes`, remaps type id / app name / editor URL onto the allocated GUID, and saves with `release=true`. Duplicate name is 409 (no replace). Non-Admin is 403. Invalid XML is 400. Existing types are never loaded with `overrideLock=true`.

Change-class companions are present: OpenAPI resource, `IContentTypesAdaptor.importContentType`, `TestContentTypeAdaptor` + `ContentTypesTestAdaptor` stubs, Mockito resource tests, adaptor success/403/400/409 tests with name round-trip and GET-able reload, product-docs 8.2 developer REST + admin developer-content-types, CD-14 gap-map note.

## Issues

None that block.

Create-only (409 on name collision) matches the issue's duplicate-name contract and the AS-08 template-import peer. "Creates or updates" in the slice acceptance is satisfied by applying imported design onto the newly created type; existing names are not overwritten.

## Cross-platform path checklist

N/A — no filesystem path construction. XML is an HTTP body string; parse uses `StringReader` + `PSXmlDocumentBuilder`. Tests do not write files.

## Tests / evidence

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 863, Failures: 0 (`ContentTypesResourceDetailTest` 128/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1874, Failures: 0, Skipped: 125 (`ContentTypeAdaptorImportTest` 9/0)
- Downstream: `IContentTypesAdaptor` gained `importContentType` (not `final`). Grep `implements IContentTypesAdaptor` → `ContentTypeAdaptor` (updated) + `TestContentTypeAdaptor` + `ContentTypesTestAdaptor`. Standalone sitemanage clean install after rest install.
