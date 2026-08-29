# Erlang review — #3984 REST CD-15 shared field control properties

**Branch:** `fix/issue-3984-shared-field-control-properties`  
**Date:** 2026-08-29  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + adaptor interface + wire DTO + Spring stub + sitemanage impl + tests + product-docs); Admin 403 not a global JAX-RS filter; typed lock exception → 409; filename/field-name path-injection rejection; rest `MainTest` Spring stub must implement new interface methods.

## Scope

Uncommitted CD-15 control/choice write slice vs `origin/main`: `rest` nested GET/PUT on `SharedFieldsResource` + adaptor interface/stub/DTO/tests, `projects/sitemanage` persist via existing `IPSContentDesignWs` shared-def load/save (request lock released on save), `product-docs/8.2` Developer REST + admin content-types, gap map CD-15.

## Summary

Admin-only GET/PUT `.../fields/{fieldName}/controlProperties` for a shared field. Mirrors content-type CD-07 wire shape (`properties` full replace, empty clears; `choices` omitted leaves catalog unchanged) but uses the shared-def request-lock pattern already used by group/field write — not the content-type held design-session lock. No new SOAP methods. No SPA editor. Choice filter / null-entry / default-selected remain design gaps (same as CT CD-07).

## Issues

None that block commit.

## Cross-platform path checklist

- No filesystem I/O. Group and field names reject `/`, `\`, `..`, NUL (same as existing shared-field catalog).
- Control property values are display-mapping parameters, not paths.

## Tests / companions

- Mockito `SharedFieldsResourceTest` (47): GET/PUT 200/400/403/404/409 + Spring stub methods on `TestSharedFieldsAdaptor`.
- Adaptor `SharedFieldsAdaptorTest` (62): GET values/choices, GET 403/404, PUT persist + lock release, empty properties clears, omitted choices leave catalog, PUT 403/409/404.
- `MainTest` still loads after new adaptor methods (stub implements interface).
- C2: `ISharedFieldsAdaptor` gained methods; no `extends` / anonymous subclass in tests; reverse-dep `projects/sitemanage` standalone clean install green.
- Standalone `rest` and `projects/sitemanage` `mvnw clean install` BUILD SUCCESS.

## Product docs

`product-docs/8.2/developer/rest.md` and `admin/developer-content-types.md` updated for shared-field control GET/PUT. Gap map CD-15 control/choice write marked shipped; SPA editor still open.
