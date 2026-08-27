# Erlang review — #3905 CD-09 itemExits persist PUT after lock

**Date:** 2026-08-27  
**Branch:** `fix/issue-3905-itemexits-persist-put`  
**Scope:** sitemanage `ContentTypeAdaptor.applyItemExits` / SAVE_FAILED surfacing, rest OpenAPI 400 text, Playwright persist spec, product-docs  
**Memory patterns hit:** change-class closure (adaptor tests with production types + Playwright); CE pipe `setInputDataExtensions` UOE; SAVE_FAILED error map as 400 when validation.

## Summary

PUT `/contenttypes/{id}/itemExits` after a held lock no longer calls `PSPipe.setInputDataExtensions` on `PSContentEditorPipe` (percPage UOE). Unchanged GET rows reuse cloned `PSConditionalExit` objects (apply-when / param types). `PSErrorsException` error-map text is included in the thrown message; SAVE_FAILED that looks like design validation / wrong extension interface is `IllegalArgumentException` (HTTP 400). `saveContentTypes` must not treat a null lock version as create (`-1`) — that deleted existing node defs on failure. Playwright persist uses `sys_cleanReservedHtmlClasses` + `sys_title` (item-level IPSItemInputTransformer), not field UDF `sys_ToUpperCase`. H2 C5: 2 passed.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

None blocking.

### Companions (checked)

| Artifact | Present |
|----------|---------|
| sitemanage adaptor apply/save | yes |
| Unit tests (`PSContentEditorPipe`, `PSErrorsException`, `PSConditionalExit`) | yes (18 in `ContentTypeAdaptorItemExitsTest`) |
| Playwright `developer-content-type-item-exits.spec.js` | yes (REST persist + 409) |
| product-docs 8.2 admin + REST | yes |
| SPA chrome | out of scope (#3901) |

### Notes

- Cross-platform path checklist: N/A (REST URL `/` only; Playwright uses `page.request` + `encodeURIComponent`).
- `isValidationSaveFailure` sniffs error-map text (`does not implement`, `validation`, …), not content-type names.
- `ContentTypeDesignLockException` is an `IllegalStateException`; rethrow catch is `IllegalStateException | IllegalArgumentException | WebApplicationException`.
