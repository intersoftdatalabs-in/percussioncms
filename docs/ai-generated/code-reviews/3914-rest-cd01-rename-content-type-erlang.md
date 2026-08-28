# Erlang review — issue #3914 REST CD-01 rename content type

**Branch:** `fix/issue-3914-rest-cd01-rename-content-type`  
**Date:** 2026-08-27  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class completeness (rest resource + adaptor interface + sitemanage impl + Mockito resource tests + Spring `TestContentTypeAdaptor` stub + sitemanage adaptor tests + product-docs); lock-typed 409 vs message sniffing; bulk PUT must not silently overload name.

## Summary

Dedicated `PUT /services/contenttypes/{idOrName}/name` under a held design-session lock. Bulk PUT still ignores name. Unique (case-insensitive) new name; spaces/wildcards/invalid chars 400; unlocked/other locker 409. GET by old name 404; GET by id returns new name.

## Issues

None (hard-gate).

### Notes (non-blocking)

- Uniqueness scans `findContentTypes("*")` (same catalog used by list). Acceptable for this design surface.
- Application/editor URL rewrite is delegated to existing `PSContentTypeHelper.saveContentType` after `PSItemDefinition.setName`.
- No filesystem path I/O in this change. Cross-platform path checklist: N/A (clean).

## Tests

- rest `ContentTypesResourceDetailTest` rename 200/400/404/409/403
- rest Spring stub `TestContentTypeAdaptor.renameContentType`
- sitemanage `ContentTypeAdaptorRenameTest` persist, GET old/new, lock, spaces, collision, same-name, admin, wildcards

## Builds

- `rest`: `mvnw.cmd clean install` BUILD SUCCESS — Tests run: 663, Failures: 0
- `projects/sitemanage`: `mvnw.cmd clean install` BUILD SUCCESS — Tests run: 1649, Failures: 0, Skipped: 125; `ContentTypeAdaptorRenameTest` 13 passed

## C2 downstream

Interface method added on `IContentTypesAdaptor`. Grep `implements IContentTypesAdaptor`: rest test stubs + sitemanage `ContentTypeAdaptor` (all updated). sitemanage standalone install green.

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
