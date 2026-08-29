# Erlang review: issue 4004 — REST AS-08 template export

**Branch:** `fix/issue-4004-template-export`  
**Base:** `origin/main`  
**Date:** 2026-08-29  
**Recommendation:** approve  
**Gate:** pass — May commit/push: yes  
**Memory patterns hit:** change-class completeness (rest resource + interface + Mockito + Spring stub + sitemanage adaptor tests); fail-closed Admin; lock=false on design-WS load; no nested Mockito `when` inside `thenReturn`.

## Scope

Uncommitted AS-08 export slice: `rest` resource/adaptor/DTO/tests, `projects/sitemanage` `TemplateAdaptor` export via `IPSAssemblyDesignWs`, product-docs 8.2 REST + Design templates.

## Summary

Admin `GET /services/templates/{idOrName}/export` returns Workbench-equivalent design XML from `IPSAssemblyDesignWs.loadAssemblyTemplates(..., lock=false, overrideLock=false, ...)`. Content-Disposition filename is derived from the template name (HTTP basename sanitizer, not filesystem joins). 404 unknown; 403 non-Admin. No import, no SPA, no lock steal.

## Issues

None that are bugs, missing behavioral tests, or non-portable path I/O.

## Cross-platform path checklist

- [x] No new filesystem `"/" +` / `"\\" +` construction
- [x] Filename helper sanitizes HTTP download names (`"`, `/`, `\`, controls) — not a path join
- [x] Tests do not write files (no `@TempDir` needed)
- [x] No Unix-only absolute roots or path-string OS assertions

## Change-class companions

| Artifact | Present |
|----------|---------|
| rest resource + OpenAPI | yes |
| rest `ITemplatesAdaptor.exportTemplate` + `TemplateExport` | yes |
| Mockito `TemplatesResourceDetailTest` 200/403/404/filename | yes |
| Spring `TestTemplatesAdaptor` stub method | yes |
| sitemanage `TemplateAdaptor` + `TemplateAdaptorExportTest` success/403/404/no-lock | yes |
| product-docs 8.2 | yes |
| Playwright / SPA | N/A (no UI) |

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 761, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1789, Failures: 0; `TemplateAdaptorExportTest` 5/0
