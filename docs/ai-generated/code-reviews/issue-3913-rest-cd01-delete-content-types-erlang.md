# Erlang review — #3913 REST CD-01 DELETE content types

**Branch:** `feat/issue-3913-rest-cd01-delete-content-types`  
**Scope:** uncommitted vs `origin/main` (rest, sitemanage, product-docs)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** rest+sitemanage change-class companions (resource + IXxxAdaptor + Spring test stub + apibridge impl + adaptor unit tests); Workbench replacement via IPS*DesignWs; product-docs REST note for public surface; do not steal design locks (409).

## Summary

Adds Admin-only `DELETE /services/contenttypes/{idOrName}` that requires a **held** design-session lock (peer PUT save / enabled):

- Adaptor: `IPSContentDesignWs.deleteContentTypes(..., ignoreDependencies=false)`
- 409 unlocked / other locker (`ContentTypeDesignLockException`); 404 missing; 403 non-Admin
- 400 when design WS rejects in-use types (dependents); no item cascade
- Does not steal locks; does not implement POST create/rename or SPA chrome

## Issues

None that block. Inner design-WS `IllegalStateException` is rethrown (not rewrapped) so 500 bodies keep the error map. `CT_CREATE_DELETE` gap **code** is kept stable; message now documents DELETE + remaining create/rename gap.

## Cross-platform path checklist

N/A — no filesystem path I/O.

## Tests / companions

- Mockito: `ContentTypesResourceDetailTest` DELETE 204/400/403/404/409
- Spring stub: `TestContentTypeAdaptor` (+ `ContentTypesTestAdaptor`)
- Adaptor: `ContentTypeAdaptorDeleteTest` (held lock, other-user 409, missing 404, dependents 400, Admin 403)
- product-docs/8.2/developer/rest.md + admin/developer-content-types.md
- Standalone `rest` then `projects/sitemanage` `mvnw clean install` green
