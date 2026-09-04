# Erlang review — issue #4266 SPA SE-02 community role assignment

**Scope:** Uncommitted WebUI + product-docs on `feat/issue-4266-spa-se02-community-role-assign` (stacked on REST #4265 / PR #4272 tip).
**Base:** `origin/feat/issue-4265-rest-se02-community-role-assign`
**Date:** 2026-09-03

## Summary

SPA chrome for community role assign/unassign already shipped (P0.5c / #1616). This slice fixes a real membership-refresh bug: `updateCommunityRoles` returned the Jackson WRAP_ROOT `{ Community: … }` payload without `unwrapCommunityDetail`, so post-save `roleList` / guid normalization could be empty or wrong. Adds Vitest for API assign/unassign/clear and panel unassign+clear. Product-docs clarify Save roles refresh/status notice. Playwright H2 remains #4267.

## Recommendation

**approve**

## Gate

- Bugs: none remaining in diff (unwrap fix is the bug fix).
- Behavioral unit tests: present for API + panel assign/unassign/clear.
- Cross-platform paths: N/A (no filesystem I/O in diff).
- Change-class companions: WebUI Vitest + product-docs; REST not re-implemented; Playwright deferred to #4267 per triage split.
- May commit/push: **yes**

## Issues

None.

## Memory patterns hit

- Jackson WRAP_ROOT response must be unwrapped on SPA clients (same class as GET detail / new-search-defaults peers).
- Full-set replace semantics for dual-list membership (empty clears).
