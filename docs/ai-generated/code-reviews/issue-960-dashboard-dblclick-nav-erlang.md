# Erlang review: issue #960 dashboard finder double-click nav

**Branch:** `fix/issue-960-dashboard-dblclick-nav`  
**Date:** 2026-08-06  
**Reviewer persona:** Erlang (strict, independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Legacy Finder column view only treated `type === "Folder"` (plus Recycling path) as double-click navigation; repository roots Sites/Assets/Design/Search are not typed `Folder`, so dblclick fired open-content instead of navigating. Fix extracts pure `shouldNavigateOnDoubleClick` beside existing root-display helpers and wires `perc_finder.js` `doubleClick` through it, with a defensive fallback when the helper is absent.

## Scope

| Path | Change |
|------|--------|
| `WebUI/src/main/webapp/cm/plugins/perc_finder_root_display.js` | `isFinderRepositoryRoot`, `shouldNavigateOnDoubleClick` |
| `WebUI/src/main/webapp/cm/widgets/perc_finder.js` | `doubleClick` uses helper (+ fallback) |
| `WebUI/src/test/js/percFinderRootDisplay.test.js` | behavioral tests + source contract |

**Cross-platform path review:** no filesystem I/O or path joins added; CMS path segments only (`["", "Sites"]` style). N/A for portability bugs.

**Memory patterns:** pure helper + Vitest eval/load pattern matches prior `percFinderRootDisplay` work (#2093).

## Issues

None at severity `bug`.

### suggestion

- **S1 (non-blocking):** WebUI `AGENTS.md` Playwright hard gate for product screen UI. This is residual classic Finder (jQuery), not SPA React. Vitest covers decision logic; live-CMS Playwright for classic Dashboard was not run in this overnight worktree. Acceptable for residual hotfix scoped by issue; optional residual Playwright bug-regression later.

### nit

- **N1:** Fallback duplicates root name list from helper. Acceptable for offline helper; could call only helper if script load order is guaranteed.

## Tests

- Vitest: `percFinderRootDisplay.test.js` — roots navigate, Folder/FSFolder navigate, site non-leaf navigates, leaf content does not, null spec false, source contract for wiring.
- List-view `folderDblClickCallback` peers untouched (issue requirement).

## Gate checklist

| Check | Result |
|-------|--------|
| No bugs | pass |
| Behavioral unit tests for new logic | pass |
| Portable paths | N/A / pass |
| Change-class companions | pure helper + Vitest peers matched |
| Scope creep | none |
