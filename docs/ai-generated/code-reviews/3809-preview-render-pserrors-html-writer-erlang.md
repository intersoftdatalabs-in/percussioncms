# Erlang review — #3809 Preview render PSErrors HTML writer

**Branch:** `fix/issue-3809-preview-render-pserrors-html-writer`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** incomplete change-class closure (Playwright companion); test fakes wrong type (`PSProperties`); missing behavioral tests; non-portable paths (none in this diff)

## Summary

Explorer Preview of FastForward Corporate Investments Home (`GET /services/pagemanagement/render/page/{id}` with browser `Accept: text/html`) mapped exceptions to `PSErrors` without a media type. CXF then tried to write `PSErrors` as `text/html` and failed with “No message body writer…”.

The change:

- Negotiates JSON vs HTML on `PSAbstractExceptionMapper`; HTML/plain responses emit a **String** HTML document (built-in writer) plus a dedicated `PSErrorsHtmlMessageBodyWriter`.
- Registers the writer on the pagemanagement JAX-RS bus.
- Calls `IPSAssemblyItem.normalize()` before in-process assemble (peer of the assembler servlet).
- FastForward template pick no longer calls lazy `PSSite.templates` with no session (`associatedTemplatesSafe` + unmodifiable reload).
- Playwright + helper reject message-body-writer / `PSErrors` class-name bodies; REST GET of pagemanagement render is asserted HTTP 200 assembled HTML.
- Product-docs Preview row documents the JAX-RS failure is gone.

No public type made `final` / no signature break. Tests use `PSValidationException` / `PSPageException` / `PSErrors` (not `Properties`). Paths in this slice are CMS/URL (`/`), not OS filesystem I/O.

## Cross-platform path checklist

- [x] No new `"/" +` / `"\\" +` filesystem path construction
- [x] CMS/URL/ZIP paths correctly use `/`
- [x] Tests do not assert Unix-only OS path strings
- [x] N/A for temp files / line endings

## Issues

None (hard-gate).

## Tests

- `PSErrorsHtmlMessageBodyWriterTest` — writeable types, writeTo HTML, escape
- `PSAbstractExceptionMapperHtmlNegotationTest` — browser Accept → HTML String entity
- `PSRenderServiceHtmlErrorMappingTest` — renderPage WAE wrap + HTML mapping
- `PSRenderAssemblyBridgeNormalizeTest` — `normalize()` called
- Playwright helper + spec (#3809) + node:test unit

## Re-review

n/a (first pass)
