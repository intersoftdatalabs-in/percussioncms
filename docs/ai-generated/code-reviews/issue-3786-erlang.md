# Erlang review — issue #3786 Content Type control property values REST (CD-07)

**Branch:** `fix/issue-3786-content-type-control-property-values`  
**Date:** 2026-08-25  
**Latest pass:** Re-review (rebase onto main + Kilo cache-miss fix)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + adaptor interface + wire DTOs + Spring stub + Mockito resource tests + sitemanage apibridge + adaptor tests + product-docs); tests.structural-only (avoided — cache-miss test is behavioral); no path I/O.

## Summary (original)

Thin REST GET/PUT for Content Type field control property **values** and choice catalogs. PUT requires a held design-session lock (409 without). Companions match rest/sitemanage AGENTS.md. Standalone `clean install` green on `rest` and `projects/sitemanage`.

## Issues (original)

None blocking.

## Cross-platform path checklist (original)

N/A — no filesystem path construction.

## Tests (original)

- rest: Mockito GET/PUT/404/409/400; Spring stubs implement new methods; JSON includes `controlProperties` values
- sitemanage: mapping helpers, GET values/choices, PUT persist, 409 lock, 400 invalid catalog, 404 field

---

## Re-review (2026-08-25) — rebase onto main + Kilo `reloadItemDef` fix

### Summary

Follow-up after Kilo WARNING on PR #3804: `replaceFieldControlProperties` now reloads via `reloadItemDef` (not `resolveItemDef`) after a successful `saveContentTypes`, so a post-save item-def cache miss cannot 404 a write that already persisted. Rebase onto `origin/main` keeps CD-09 `itemExits` GET/PUT plus CD-07 `controlProperties`. `put_cacheMissAfterSave_fallsBackToLockedDef` exercises the fallback against `PSInvalidContentTypeException` on `getItemDef(311L)` after save. No blocking issues.

### Scope

- Base: `origin/main` (`458f521a6b501f0e2f52ea0e5858db163c460f4c`)
- Head: `fix/issue-3786-content-type-control-property-values` worktree `C:/Users/Nate/.grok/worktrees/intersoft-workspace-percussioncms/pr-3804` (`a39ad4ba8226fde127412d6d13951ecfb727ac3b`)
- PR: #3804 (GitHub files snapshot still showed pre-fix `363a179`; this pass reviewed the worktree tree, including the unpushed/local follow-up)
- Files: rest + sitemanage CD-07 surface (adaptor, resource, `IContentTypesAdaptor`, two Spring stubs, resource tests, adaptor tests, product-docs); union verified against CD-09 `itemExits` already on main
- Prior report: `docs/ai-generated/code-reviews/issue-3786-erlang.md` (this file)
- Memory patterns hit: change-class closure (rest↔sitemanage companions); behavioral test for post-save cache miss (not structural-only); cross-platform I/O N/A

### Recommendation

approve

### Gate

- Blocking bugs: 0
- May commit/push: yes

### Issues

None blocking.

Kilo WARNING (`resolveItemDef` after PUT save → 404 on cache miss) is **fixed**. `replaceFieldControlProperties` at `ContentTypeAdaptor.java:846` uses `reloadItemDef`, which catches `PSInvalidContentTypeException` and returns null (`2047–2056`); caller falls back to the locked in-memory `def` (`847`). The dedicated catch that previously mapped that miss to “not found” was removed, so a successful save is no longer swallowed as 404. Test `put_cacheMissAfterSave_fallsBackToLockedDef` (`ContentTypeAdaptorControlPropertiesTest.java:251`) stubs `getItemDef(311L)` to throw after `stubDefinition()`; pre-save resolution uses numeric GUID + `loadContentTypes`, so the throw only hits the post-save reload. Asserts save was invoked and returned/in-memory values are `640`.

CD-09 + CD-07 union: `IContentTypesAdaptor` has both `get/replaceItemExits` and `get/replaceFieldControlProperties`; `ContentTypesResource` exposes both path pairs; Spring stubs `TestContentTypeAdaptor` and `ContentTypesTestAdaptor` implement both. `contentTypeDesignGaps` `CT_ITEM_EXITS` message points at GET/PUT `itemExits` (main CD-09), not the old “not exposed” text.

### Cross-platform path checklist

Applied. No filesystem path construction. Choice `lookupHref` (`../sys_lookup/foo.xml`) is a design URL, not an OS path join. REST path strings in docs/OpenAPI use `/` (URI form).

### Tests (this pass)

- `put_cacheMissAfterSave_fallsBackToLockedDef` — behavioral coverage of the Kilo fix
- Existing PUT persist / 409 / 400 / GET 404 field still present
- rest resource Mockito GET/PUT/409/400; GET field-not-found 404; Spring stubs keep **both** itemExits and controlProperties
- No new path/file I/O assertions

### Non-blocking notes (not issues)

- Kilo suggestions remain optional: PUT unknown-field 404 (GET already covers `requireFieldMapping`) and asserting `designGaps` on the control-properties envelope.
- Pre-existing on this class (not introduced by CD-07): `setContentTypeEnabled` (`545`) and `setAllowedWorkflows` (`679`) still call `resolveItemDef` after save; those paths can still 404 on cache miss. Out of scope for this follow-up; do not treat as a CD-07 gate.

### Handoff

- Reviewed: PR #3804 follow-up (rebase onto main + Kilo `reloadItemDef` fix) in worktree `pr-3804`.
- Top finding: none blocking; cache-miss 404 after successful PUT is fixed and tested.
- Recommendation: **approve**. May commit/push: **yes**.
- Artifact: `docs/ai-generated/code-reviews/issue-3786-erlang.md`
