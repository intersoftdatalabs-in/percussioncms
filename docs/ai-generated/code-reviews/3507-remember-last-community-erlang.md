# Erlang review — #3507 remember last community

**Branch:** `fix/issue-3507-remember-last-community`  
**Date:** 2026-08-17  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Change class:** WebUI product screen (profile checkbox + login restore)

## Summary

Slice 3 of #3505 adds an opt-in **Remember last community on next login** checkbox on
My profile → Account. Last community is persisted via PreferenceResource list/PUT
(not named GET). Session start restores with POST `/services/communities/switch/{name}`
when the stored name is still in membership; otherwise login default is left in place.

## Scope

Uncommitted work vs `origin/main` on this branch. Modules: `WebUI`,
`product-docs/8.2/admin/users-roles.md`, `modules/perc-qa-automation` Playwright.

## Gate

No bugs, missing behavioral tests, or non-portable path I/O.

Cross-platform path checklist: **N/A** — no new filesystem path construction.

## Issues

None remaining.

Fixed during C5: `parseRememberLastCommunityFlag` threw when PreferenceResource
returned a JSON boolean (`(e ?? "").trim is not a function`). Coerce with
`String(value)` / boolean short-circuit. Covered by unit + profile Playwright.

## Tests

- `rememberLastCommunity.test.ts` — parse, restore decision, list vs named GET, PUT
- `restoreLastCommunity.test.ts` — switch / skip / revoked / swallow errors
- `AccountSection.test.tsx` — reload checkbox, persist on enable, revert on save fail
- `UserMenu.test.tsx` — write-only last persist, persist fail does not undo switch
- Playwright: profile checkbox persist/reload + login landing community spec

## Memory patterns hit

- Do not GET `/preferences/{name}` from chrome (#3468 / #3458) — list on profile
  and one-shot session restore; chrome writes last community only.
- Consume #3508 default community as fallback only (no re-implemented editor).
