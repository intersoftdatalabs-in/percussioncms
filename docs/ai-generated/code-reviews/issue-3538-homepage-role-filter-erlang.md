# Erlang review — #3538 homepage role filter

**Branch:** `fix/issue-3538-homepage-role-filter`  
**Date:** 2026-08-17  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** no bugs, behavioral tests present, no new path/file I/O

## Summary

Slice 3 of parent #3515 tightens profile and Admin → Users homepage pickers
to the same gates as `topNavItemIds` after #3514. Shared
`isLandingAllowed` / `landingGatesFromRoles` is the single source of truth:
Explorer always; Navigation / Developer / Publish for designer|admin; Admin
for admin; Editor / Design / Widget Builder omitted as new choices with
stale-once listing.

## Issues

None that block.

## Tests

- Vitest: contributor vs designer vs admin matrix on shared gates, profile
  options, and `landingOptionsForRoles`.
- Playwright: Contributor profile does not list Admin/Developer/Publish;
  Admin Users picker grows Admin/Developer when the Admin role is checked.
- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS, 2773 Vitest passed.

## Cross-platform path checklist

N/A — no new filesystem path construction.

## Memory patterns hit

- Change-class companions: product-docs + Playwright for WebUI picker UX.
- Stale-once current value so disallowed stored landings can be cleared.

> Co-Authored by Grok Build 1.0.4 using grok-4.6 with agent night-issue-prs.
