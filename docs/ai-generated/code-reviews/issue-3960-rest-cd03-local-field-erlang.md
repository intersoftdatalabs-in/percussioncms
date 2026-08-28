# Erlang review — issue #3960 REST CD-03 local field create/delete

**Branch:** `feat/issue-3960-rest-cd03-local-field-create-delete`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + adaptor interface + sitemanage impl + Spring stub + Mockito resource tests + adaptor tests); C2 interface implementors; product-docs REST companion

## Summary

Admin REST adds `POST/DELETE /contenttypes/{idOrName}/fields` for persistable local fields via existing `IPSContentDesignWs` load/save under a held design-session lock. Optional `fieldSet` targets or creates a named complex child. Status mapping matches content-type write peers (400/403/404/409). No SPA. No new SOAP.

## Scope

Uncommitted vs `HEAD` on this branch (rest, sitemanage, product-docs/8.2). C5 Playwright N/A (no WebUI). Cross-platform path I/O: N/A (identifier sanitization only; `/` `\\` `..` rejected on field names).

## Issues

None that block. Duplicate `containsWhitespace` on `ContentTypeAdaptor` was caught at sitemanage compile and removed before this review.

## Companions

| Companion | Status |
|-----------|--------|
| rest resource + OpenAPI | yes |
| `IContentTypesAdaptor` | yes |
| sitemanage `ContentTypeAdaptor` | yes |
| rest Spring stub `TestContentTypeAdaptor` | yes |
| `ContentTypesTestAdaptor` | yes |
| Mockito resource tests | yes |
| adaptor success/403/404/409 | yes |
| product-docs 8.2 REST + admin CT | yes |
| designGaps `CT_FIELD_CREATE_DELETE` | yes |

## C2

Grep `implements IContentTypesAdaptor`: 3 types (ContentTypeAdaptor + two rest test stubs), all updated. No anonymous subclasses. sitemanage standalone `mvnw clean install` green.

## Build

- `rest`: standalone `mvnw clean install` BUILD SUCCESS; ContentTypesResourceDetailTest 91/0; module ~731 tests
- `projects/sitemanage`: standalone `mvnw clean install` BUILD SUCCESS; ContentTypeAdaptorLocalFieldTest 17/0; module 1683 tests
