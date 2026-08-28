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

- Mockito: `ContentTypesResourceDetailTest` DELETE 204/400/403/404/409/500
- Spring stub: `TestContentTypeAdaptor` (+ `ContentTypesTestAdaptor`)
- Adaptor: `ContentTypeAdaptorDeleteTest` (held lock, other-user 409, missing 404, dependents 400, Admin 403, blank-before-session 404)
- product-docs/8.2/developer/rest.md + admin/developer-content-types.md
- Standalone `rest` then `projects/sitemanage` `mvnw clean install` green

## Re-review (PR #3919 kilo-code-bot threads)

**Scope:** uncommitted follow-up vs HEAD (`deleteContentType` order, `CT_CREATE_DELETE` path wording, resource 500 test).  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for validation-order and shared error mapping; REST-GAPS-01 message consistency.

### Changes

- Blank `idOrName` now returns null (HTTP 404) **before** `requireSessionUserForLock()` (matches lock/unlock). No-session + blank is no longer 403.
- `CT_CREATE_DELETE` message uses `/contenttypes` (no `/services` prefix) for both Create and DELETE, matching `CT_ITEM_EXITS`.
- Resource `deleteContentTypeGenericFailureIs500` covers `mapMutationFailure` `IllegalStateException` → 500 (name `percBlockquote` must not become 409).

### Issues

None that block. `setContentTypeEnabled` still checks session before blank; that method is out of this PR's review threads and already returns 400 after session. No public method/ctor signature change. Cross-platform path checklist N/A.

### Evidence

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 676, Failures: 0 (`ContentTypesResourceDetailTest` 69/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1676, Failures: 0, Skipped: 125 (`ContentTypeAdaptorDeleteTest` 11/0)
