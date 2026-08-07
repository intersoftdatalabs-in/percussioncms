# Erlang review — issue #2206 Empty Recycling menu + confirm

**Branch:** `feat/issue-2206-empty-recycling-menu`  
**Scope:** WebUI finder Empty Recycling action + i18n + dual-tree sync  
**Parent:** #944 slice 2/3  
**Depends on:** #2205 / PR #2215 (`DELETE /pathmanagement/recycle/empty`)  
**Date:** 2026-08-06  
**Reviewer:** Erlang (pre-commit gate)

## Summary

Adds Admin-only finder Actions menu entry **Empty Recycling** with permanent-purge confirm dialog, client call to bulk empty API, path constant, dual-tree (src/war/legacy) lockstep, CmsUi.tmx keys, and Vitest behavioral coverage. Does not re-enable delete of SYSTEM / Sites / Assets roots.

## Recommendation

**approve**

## Gate

- Bugs: none found  
- Behavioral unit tests: present (`WebUI/src/test/js/percEmptyRecycling.test.js`, 8 tests)  
- Cross-platform path I/O: N/A (no filesystem path handling)  
- **May commit/push: yes**

## Memory patterns hit

- Dual-tree WebUI lockstep (src ↔ war; legacy service mirror)  
- Confirm-dialog severity for permanent purge peers  
- Admin-only destructive bulk action alignment with backend 403  
- Prefer pure helpers for enablement/summary unit tests  

## Issues

None blocking.

### Suggestions (non-blocking)

1. **#2205 merge order** — UI PR should not be merged before #2215 lands (or will 404/500 at runtime). Documented in PR body.  
2. **Full multi-locale TMX** — new keys ship `en-us` only; runtime falls back via `PSTmxResourceBundle` language chain. Residual translations optional.  
3. **Playwright E2E** — deferred to #2207 per split plan; `data-testid="perc-finder-empty-recycling"` is ready for slice 3.

## Change-class companions checked

| Companion | Status |
|-----------|--------|
| Path constant `RECYCLE_EMPTY` | src + war + legacy |
| `PercRecycleService.emptyRecycling` | src + war + legacy |
| Finder Actions menu wiring | src + war `perc_actions_button.js` |
| Script include `finder_js.jsp` | src + war |
| Minify bundle `common-bundles.json` | yes |
| i18n `CmsUi.tmx` | en-us keys |
| Vitest behavioral | 8 tests green |
| No SYSTEM-root delete re-enable | delete button untouched |

## Test evidence

- `npx vitest run ../../test/js/percEmptyRecycling.test.js` → 8 passed  
- `cd WebUI && ../mvnw clean install` → BUILD SUCCESS (war produced)  
- `cd modules/perc-i18n && ../../mvnw clean install` → BUILD SUCCESS  
