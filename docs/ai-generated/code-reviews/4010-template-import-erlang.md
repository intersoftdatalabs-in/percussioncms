# Erlang review — issue #4010 REST AS-08 template import

**Branch:** `feat/issue-4010-template-import`  
**Scope:** uncommitted vs HEAD (rest + sitemanage + product-docs 8.2)  
**Date:** 2026-08-29  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class completeness (rest resource + adaptor interface + Spring stub + sitemanage impl + tests); Admin `IPSUserService.isAdminUser` 403; design-WS create/save/release; no stolen locks

## Summary

Admin POST `/services/templates/import` accepts Workbench-equivalent `<assembly-template>` XML (same document as Workbench export / peer GET export). sitemanage `TemplateAdaptor` parses via `PSAssemblyTemplate.fromXML`, creates through `IPSAssemblyDesignWs.createAssemblyTemplates`, applies XML while keeping the new GUID, and saves with `release=true`. Duplicate name is 409 (no replace). Non-Admin is 403. Invalid XML is 400. Reload uses `lock=false`, `overrideLock=false`.

Change-class companions are present: OpenAPI resource, `ITemplatesAdaptor.importTemplate`, `TestTemplatesAdaptor` stub, Mockito resource tests, adaptor success/403/400/409 tests with name round-trip, product-docs 8.2 developer REST + admin design-templates.

## Issues

None that block.

## Cross-platform path checklist

N/A — no filesystem path construction. XML is an HTTP body string; `toXML`/`fromXML` in tests is in-memory.

## Tests / evidence

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 822, Failures: 0 (`TemplatesResourceDetailTest` 22/0; `MainTest` ApplicationContext)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1874, Failures: 0 (`TemplateAdaptorImportTest` 8/0)
