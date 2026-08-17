# Erlang review — feat/issue-3475-aa-contenteditable-overlay

**Date**: 2026-08-17  
**Scope**: uncommitted vs `origin/main` on `feat/issue-3475-aa-contenteditable-overlay`  
**Reviewer**: Erlang (independent of implementer)  
**Memory patterns hit**: change-class completeness (WebUI screen + Playwright + product-docs); reuse itemmanagement persist (not Data Flow CE HTML); behavioral tests for mapping + save; leftover AA chrome must be stripped; no new jQuery/Dojo

## Summary

Active Assembly overlay maps known **scalar text** fields from `GET /services/itemmanagement/item/fields/{id}` onto the assembled iframe (`PsAaField` object ids, `data-perc-field` markers, or unique text values). Save uses `PUT` on the same itemmanagement contract as the React editor (#3470). Leftover Dojo AA images/`ps.aa` handlers are removed. Rich/binary/keyword/community stay on the Content Editor host. Slot add/arrange unchanged.

## Recommendation

`approve`

## Gate

May commit/push: **yes**

## Change-class closure

| Companion | Status |
|-----------|--------|
| Pure mapping + persist (`overlayFields.ts`) | Present |
| AssemblyHost overlay bar + iframe apply + save | Present |
| Vitest (`overlayFields.test.ts` + host save paths) | Present |
| Playwright (`explorer-active-assembly.spec.js` `@aa-overlay`) | Present |
| product-docs `8.2/admin/content-explorer.md` + `developer/rest.md` | Present |
| Spec 996 status + 995 later pointer | Present |
| New files Intersoft 2026 Apache headers | Present |
| No leftover AA/CE HTML requests | Present (strip + Playwright blocklist) |

## Cross-platform path checklist

N/A — no filesystem path construction. URL/query paths stay `/` (correct).

## Issues

None blocking.

### Suggestion

Owner checkout at overlay load is best-effort; a failed checkout still shows the preview and surfaces save failure on PUT. Acceptable for this slice.

## Tests

- `WebUI` Vitest assembly: 29 passed (including 11 overlayFields + 2 new host cases)
- Playwright surface `tests/explorer-active-assembly.spec.js` required for C5
