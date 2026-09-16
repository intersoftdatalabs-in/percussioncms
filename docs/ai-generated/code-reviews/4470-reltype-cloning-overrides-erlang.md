# Erlang review — #4470 relationship-type cloning field overrides

- **Branch:** `fix/issue-4470-reltype-cloning-overrides`
- **Commit:** `e6d60d1367`
- **Base:** `origin/main` (`c5b4eafa98`)
- **Reviewer:** Erlang (strict independent)
- **Date:** 2026-09-16
- **Memory patterns hit:** change-class closure (REST + sitemanage adaptor + SPA + Playwright + product-docs); behavioral tests for validation/403/round-trip; no new adaptor Spring stub required (existing `IRelationshipTypeAdaptor`); portable paths N/A (no filesystem I/O)

## Summary

Admin GET/PUT of relationship-type cloning field overrides (`cloneOverrides`: field name + UDF ref + literal params) with SPA editor, surface Playwright, and product-docs. Cloning-override `designGaps` entry dropped; effect-condition gap remains. Change-class companions are present.

## Recommendation

`approve`

## Gate

- **May commit/push:** yes (already committed; no uncommitted product diffs at review time)
- **Bugs:** none
- **Missing behavioral tests:** none
- **Non-portable paths:** N/A (no path/file I/O)
- **Change-class closure:** complete (DTO + resource docs + adaptor apply/copy + SPA editor + Vitest + Playwright + product-docs)

## Issues

### bug (fixed in follow-up)

- Dropping the cloning-override `designGaps` entry left a **single** `List<String>` on detail GET. CXF/JAXB JSON serializes that as a **bare string**. SPA `gaps.map` then throws, ErrorBoundary replaces the catalog, Playwright cannot open/save types. Fix: coerce JAXB scalars to arrays on unwrap (`coerceStringList` / `coerceObjectList`) and guard `Array.isArray` in the detail panel.

### nit (non-blocking)

- `product-docs/8.2/developer/rest.md` copyFrom bullet lists copied mutable fields but omits `cloneOverrides`, which `copyMutableFromSource` does copy. Integrators may miss that copy-from includes overrides.

## Re-review

JAXB single-element list crash addressed in SPA unwrap + panel guards. Remaining nits unchanged. Gate: **May commit/push: yes** after WebUI tests + Playwright re-run.

## Cross-platform path checklist

Not applicable — no new filesystem path construction, temp files, or path assertions.

## Tests reviewed

- `RelationshipTypeResourceTest` — GET includes overrides; invalid update → 400
- `RelationshipTypeAdaptorWriteTest` — round-trip, leave-when-omitted, clear-empty, invalid field/ref, non-Admin 403
- SPA `RelationshipTypeDetailPanel.test.tsx` — save payload includes cloneOverrides
- Playwright `developer-relationship-type-editor.spec.js` — add / round-trip / clear + console guards
