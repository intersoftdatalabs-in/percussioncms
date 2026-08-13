# Erlang review — #3325 All Content duplicate views

**Branch:** `fix/issue-3325-all-content-duplicate-views`  
**Date:** 2026-08-13  
**Persona:** Erlang (independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path checklist:** N/A — no filesystem path I/O in the diff (TS grouping + product-docs + Playwright selectors).

## Summary

Explorer Views → All Content (parentCategory 3) could render the same logical view as seven identical **All** leaves when `GET /services/views` repeated `View_All` (or unlabeled rows). Grouping now dedupes by name/guid/id, drops unlabeled identity-less duplicates from the tree (no executable key), keeps distinct names/guids as separate leaves, and disambiguates shared display labels with the internal name.

## Memory patterns hit

- Missing behavioral unit tests for new/changed non-trivial logic — covered (`viewCatalog.test.ts`, `ViewsCatalogTree.test.tsx`).
- Incomplete change-class closure — Playwright companion + product-docs updated.

## Issues

None that block commit.

## Tests

- Vitest: seven `View_All` copies collapse to one leaf; distinct `View_All` / `All_Sites` stay two leaves with disambiguated labels; string `parentCategory` `"3"` buckets to All Content; `id: 0` is not an identity.
- Playwright: `explorer-views.spec.js` expands group 3 and asserts at most one exact **All** leaf and unique leaf testids.

## Product docs

`product-docs/8.2/admin/content-explorer.md` All Content row documents unique-logical-view listing.
