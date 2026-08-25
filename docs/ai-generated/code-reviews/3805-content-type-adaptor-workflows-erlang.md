# Erlang review — issue #3805 ContentTypeAdaptor CD-08 workflows

**Branch:** `fix/issue-3805-content-type-adaptor-workflows`  
**Date:** 2026-08-25  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (bugfix of existing CD-08 adaptor; rest companions already on main); behavioral test for post-save cache miss (not structural-only); no path I/O; `resolveItemDef` after PUT save must not 404 a committed persist (CD-07 peer).

## Summary

`ContentTypeAdaptor.setAllowedWorkflows` (CD-08) now matches `setContentTypeEnabled` for existence (item-def lookup so unknown ids 404 before lock) and matches CD-07 `replaceFieldControlProperties` for post-save reload (`reloadItemDef` + fall back to the locked definition). A post-save `PSInvalidContentTypeException` is no longer swallowed as HTTP 404 after a successful `saveContentTypes`.

Cycle Verify (#3805) reported 6 failures + 2 errors in `ContentTypeAdaptorWorkflowsTest` on the full sitemanage suite: lock 409 not thrown, unknown workflow 400 not thrown, empty-list `setWorkflowInfo` never invoked, PUT returning null. Those symptoms match an early null return (existence via `loadContentTypes(lock=false)`) and/or `resolveItemDef` throwing after save.

## Issues

None blocking.

Existence via `resolveItemDef` is the same pattern as `setContentTypeEnabled` (suite-green peer). Post-save `reloadItemDef` is the CD-07 fix already approved on this class. `put_cacheMissAfterSave_fallsBackToLockedDef` stubs `getItemDef(311L)` as first-return / second-throw so existence succeeds and reload misses.

Lock / unknown-workflow / empty-list / persist tests remain and now also assert allowed-workflow list size on GET after PUT.

No public/protected signature change. No rest module change. Product-docs already describe `200` + `allowedWorkflows` (CD-08); this restores that contract.

## Cross-platform path checklist

Applied. No filesystem path construction. REST path strings in existing product-docs use URI `/`.

## Tests

- `put_cacheMissAfterSave_fallsBackToLockedDef` — behavioral coverage of cache-miss fallback
- Persist / GET round-trip list size restored
- Existing 409 lock / 400 unknown id and name / empty-list clear / 404 unknown type / 403 / session still present
- Standalone `cd projects/sitemanage && ../../mvnw.cmd clean install`: **BUILD SUCCESS**, Tests run: 1535, Failures: 0, Errors: 0, Skipped: 125; `ContentTypeAdaptorWorkflowsTest` 13/0/0

## Non-blocking notes (not issues)

- `setContentTypeEnabled` still calls `resolveItemDef` after save (same pre-existing cache-miss 404). Out of scope for #3805.
- Empty `loadContentTypes(lock=true)` after a held lock still returns null (404). Same as enable/disable; do not treat as a CD-08 gate.

## Handoff

- Reviewed: uncommitted issue #3805 work in worktree `night-issue-prs`
- Top finding: none blocking
- Recommendation: **approve**. May commit/push: **yes**.
- Artifact: `docs/ai-generated/code-reviews/3805-content-type-adaptor-workflows-erlang.md`
