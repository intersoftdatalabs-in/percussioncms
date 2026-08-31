# Erlang review — issue 4062 REST POST /services/contenttypes persist lock/typeId 0

**Scope:** uncommitted vs `HEAD` / `origin/main` on `fix/issue-4062-contenttypes-post-persist-lock`. Unique work: create-lock objectId aligned with save lookup (packed NODEDEF design GUID); stamp assigned uuid over default CE template `contentType="0"`; adaptor keepTypeId on POST create (no lock steal). Base for product: `origin/main`.
**Reviewer:** Erlang (independent of implementer).
**Date:** 2026-08-31
**Memory patterns hit:** behavioral tests for new/changed lock lookup; change-class closure (system + sitemanage + product-docs); no extra agent-rule commit.

## Summary

`createContentTypes` locked `nodeDef.getGUID()` (`IPSGuid.longValue()` is uuid-only when host is 0) while `saveContentTypes` looked up `contentTypeLockObjectId(def.getTypeId())`. New defs can still report typeId 0 because `sys_Default.xml` has `contentType="0"`, so save missed the create lock (`PSErrorsException`, `server.log` typeId 0 / default workflow reset). Create now locks packed NODEDEF+uuid (same helper as load). After the default template is loaded, `applyNewContentTypeIdentity` stamps the assigned uuid onto the item def and editor. Save lookup prefers typeId and falls back to the editor content type if typeId is still 0. Import still keeps the created typeId (no steal). Duplicate 409 / invalid-name 400 paths are unchanged.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

N/A — no new filesystem path joins, temp dirs, or path-string assertions. Tests use GUID longs and in-memory `PSItemDefinition` / Mockito.

## Issues

None that block. Live H2 REST POST is Cycle verify (unit tests cover create/save lock identity including typeId 0). Not a WebUI screen change; Playwright C5 does not apply.

## Tests

- `PSContentDesignWsContentTypeLockObjectIdTest` — create GUID and save lookup share packed objectId; identity stamp over template zero; save fallback to editor content type when typeId is 0; persisted `PSObjectLock.objectId` matches save lookup.
- `ContentTypeAdaptorCreateTest` — POST create keeps assigned typeId, does not `loadContentTypes` (no steal), does not rewrite identity when typeId is still 0; existing 409/400/403 coverage retained.
- `ContentTypeAdaptorImportTest` — keepTypeId path unchanged (9 tests).
