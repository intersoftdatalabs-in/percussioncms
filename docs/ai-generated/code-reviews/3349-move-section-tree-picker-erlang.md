# Erlang review — #3349 Move section tree picker

**Branch:** `feat/issue-3349-move-section-tree-picker`  
**Base:** `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Date:** 2026-08-14

## Summary

Architecture Navigation gains a **Move section** action for a selected non-root navon. The dialog reuses `SectionTreePickerDialog`, omits the source subtree (cycle guard), validates the target parent (regular section/blog only), optional sibling `targetIndex` (`-1` append, CM1 `POST /section/move`), cancel does not POST, invalid target is a local alert. Tree refresh uses existing `runMutation` → `moveSiteSection`.

Memory patterns hit: change-class companions (Vitest + Playwright + product-docs), no new filesystem path I/O, Intersoft 2026 headers on new files.

## Cross-platform path checklist

N/A — no new file I/O, path joins, or installer scripts.

## Issues

None (no bugs / missing behavioral tests / non-portable I/O).

## Tests / companions

- Vitest: `sectionMutations` reparent helpers; `MoveSectionDialog` picker/cancel/invalid; `ArchitectureShell` cancel vs POST.
- Playwright: `architecture-nav-move-section.spec.js` (H2 QA, cancel, console-clean).
- Product-docs: `product-docs/8.2/admin/architecture-navigation.md`.
- TMX en-us keys in `CmsUi.tmx` (no multi-locale backfill).
