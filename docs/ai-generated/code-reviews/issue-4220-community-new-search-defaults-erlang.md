# Erlang review — issue #4220 community new-search defaults (UI-09)

**Branch:** `feat/issue-4220-community-new-search-defaults`  
**Base:** `origin/main`  
**Reviewer:** Erlang (pre-commit, implementer session)  
**Date:** 2026-09-03

## Summary

SPA Developer Communities detail gains Admin GET/PUT chrome for CX new-search
defaults (`/services/communities/{idOrName}/new-search-defaults`). Picker is
the existing searches catalog. Empty PUT clears. 400 unknown search and 403
non-Admin surface in the panel. Vitest covers wrap/unwrap, load, replace,
empty-clear, 400, 403. Product-docs 8.2 remove the “chrome later” gap.
Playwright spec added for live H2.

## Scope

- `WebUI/src/main/ts/api/developer/{types,assemblyApi}.ts`
- `WebUI/src/main/ts/developer/{CommunityDetailPanel,communityNewSearchDefaults,messages}.tsx?`
- Vitest under `WebUI/src/test/ts/`
- `product-docs/8.2/admin/{developer-communities,developer-searches,users-roles}.md`
- `product-docs/8.2/developer/rest.md`
- `modules/perc-qa-automation/frontend/tests/developer-community-new-search-defaults.spec.js`
- Smoke-set entry

`WebUI/war` does not mirror SPA TS; no dual-ship.

Cross-platform path review: no filesystem path construction; REST URLs use `/`.

Memory patterns hit: WRAP/UNWRAP_ROOT_VALUE on PUT; Admin 403/400 surfaced;
empty set is 200 not 404.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None at bug severity.

- suggestion: DeveloperUi.tmx does not yet list the new `perc.ui.developer@`
  new-search-default strings (English fallback after `@` still works).
- nit: Playwright restore-toggle after save is best-effort so later H2 tests
  keep the original default set.
