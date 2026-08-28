# Erlang review — #3944 REST CD-15 shared field write

**Branch:** `feat/issue-3944-shared-fields-rest-write`  
**Date:** 2026-08-28  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + adaptor + Spring stub + sitemanage impl + tests + product-docs); Admin 403 not a global JAX-RS filter; typed lock exception → 409; filename path-injection rejection.

## Scope

Uncommitted CD-15 write slice vs `origin/main`: `rest` sharedfields resource/adaptor/DTOs/tests/stub, `projects/sitemanage` `SharedFieldsAdaptor` + tests, `product-docs/8.2` Developer REST + admin content-types, gap map.

## Summary

Admin-only REST create / save / delete for shared field groups over existing `IPSContentDesignWs.loadContentEditorSharedDef` / `saveContentEditorSharedDef` (request-scoped lock + release). Peers: Keywords CRUD, CT PUT 400/403/404/409, GET catalog #3929. Field create/delete, control/choice write, SPA editor remain later slices (`designGaps`).

## Issues

None that block commit.

## Cross-platform path checklist

- No filesystem I/O. Group name / filename reject `/`, `\`, `..`, NUL (same as GET catalog).
- Object-store `.xml` suffix is a filename constraint, not an OS path join.

## Tests / companions

- Mockito `SharedFieldsResourceTest` (23): POST/PUT/DELETE 200/204/400/403/404/409 + Spring stub methods on `TestSharedFieldsAdaptor`.
- Adaptor `SharedFieldsAdaptorTest` (29): create/save/delete success, 403, 404, duplicate 409, lock 409, field patch, persistable empty group XML.
- `MainTest` still loads after new adaptor methods (stub implements interface).
- Standalone `rest` and `projects/sitemanage` `mvnw clean install` BUILD SUCCESS.

## Product docs

`product-docs/8.2/developer/rest.md` and `admin/developer-content-types.md` updated for POST/PUT/DELETE. Gap map CD-15 write marked shipped (group CRUD); field create/delete + SPA still open.
