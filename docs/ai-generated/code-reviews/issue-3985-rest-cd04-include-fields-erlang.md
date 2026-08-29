# Erlang review — issue #3985 REST CD-04 include system/shared fields

**Branch:** `feat/issue-3985-rest-cd04-include-fields`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + adaptor interface + sitemanage impl + Spring stub + Mockito resource tests + adaptor tests); C2 interface implementors; product-docs REST companion

## Summary

Admin REST adds `POST /contenttypes/{idOrName}/fields/include` to include an existing system or shared field under a held design-session lock. Persist is `IPSContentDesignWs.loadContentTypes` / `saveContentTypes` only (catalog read of system/shared defs is unlocked). Origin stays `system`/`shared` (excludes + display mapping; typed field is not copied as local). Status mapping matches content-type write peers (400/403/404/409). No SPA. No new SOAP.

## Scope

Uncommitted vs `HEAD` on this branch (rest, sitemanage, product-docs/8.2, gap map). C5 Playwright N/A (no WebUI). Cross-platform path I/O: identifier sanitization only (`/`, `\\`, `..` rejected on field names).

## Issues

None that block.

## Companions

| Companion | Status |
|-----------|--------|
| rest resource + OpenAPI | yes |
| `IContentTypesAdaptor` | yes |
| sitemanage `ContentTypeAdaptor` | yes |
| rest Spring stub `TestContentTypeAdaptor` | yes |
| `ContentTypesTestAdaptor` | yes |
| Mockito resource tests | yes |
| adaptor success/403/404/409/lock | yes |
| product-docs 8.2 REST + admin CT | yes |
| gap map CD-04 | yes |
| designGaps `CT_SHARED_FIELD_INCLUSION` | yes |

## C2

Grep `implements IContentTypesAdaptor`: 3 types (`ContentTypeAdaptor` + two rest test stubs), all updated. No anonymous subclasses. sitemanage standalone `mvnw clean install` green after rest install.

## Build

- `rest`: standalone `mvnw.cmd clean install` BUILD SUCCESS; ContentTypesResourceDetailTest 98/0; module Tests run: 763, Failures: 0
- `projects/sitemanage`: standalone `mvnw.cmd clean install` BUILD SUCCESS; ContentTypeAdaptorIncludeFieldTest 15/0; module Tests run: 1799, Failures: 0, Skipped: 125

## Cross-platform path checklist

- No new filesystem path joins
- Field-name sanitizer rejects `/`, `\\`, `..` (same class as CD-03)
- N/A for installers/packaging
