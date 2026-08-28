# Erlang review — #3954 REST CD-15 shared field field create/delete

**Branch:** `feat/issue-3954-shared-field-field-crud`  
**Date:** 2026-08-28  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + adaptor + Spring stub + sitemanage impl + tests + product-docs); Admin 403 not a global JAX-RS filter; typed lock exception → 409; filename/field-name path-injection rejection; `PSConcurrentIterator` does not support `Iterator.remove()`.

## Scope

Uncommitted CD-15 field create/delete slice vs `origin/main`: `rest` nested POST/DELETE on `SharedFieldsResource` + adaptor interface/stub/tests, `projects/sitemanage` persistable `PSField` + display mapping, `product-docs/8.2` Developer REST + admin content-types, gap map.

## Summary

Admin-only nested REST to add and remove fields on an existing shared field group over existing `IPSContentDesignWs.loadContentEditorSharedDef` / `saveContentEditorSharedDef` (request-scoped lock + release). Peers: group CRUD #3944 / PR #3953. Persistable `TYPE_SHARED` field with backend column locator and default `sys_EditBox` mapping. Duplicate field names (including other groups) are 409. Control/choice write and SPA editor remain later slices (`designGaps`).

## Issues

None that block commit.

Fixed during review/tests: `PSDisplayMapper.removeMapping` uses `Iterator.remove()`, which `PSConcurrentIterator` rejects. Adaptor now removes mappings by index.

## Cross-platform path checklist

- No filesystem I/O. Group and field names reject `/`, `\`, `..`, NUL (same as group catalog).
- Field name charset is letters/digits/underscore (portable, not a filesystem join).

## Tests / companions

- Mockito `SharedFieldsResourceTest` (37): nested POST/DELETE 200/204/400/403/404/409 + Spring stub methods on `TestSharedFieldsAdaptor`.
- Adaptor `SharedFieldsAdaptorTest` (50): add/delete success, 403, 404, duplicate 409 (same group and other group), lock 409, persistable XML, invalid name/`dataType`.
- `MainTest` still loads after new adaptor methods (stub implements interface).
- Standalone `rest` and `projects/sitemanage` `mvnw clean install` BUILD SUCCESS.

## Product docs

`product-docs/8.2/developer/rest.md` and `admin/developer-content-types.md` updated for nested field POST/DELETE. Gap map CD-15 field create/delete marked shipped.
