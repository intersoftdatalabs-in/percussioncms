# Erlang review — issue #4649 PublishingShell sites list search

**Scope:** uncommitted branch `feat/issue-4649-publish-sites-search` vs `origin/main`.
**Persona:** erlang-code-review (in-session; no spawn-subagent tool on this host).

## Summary

PublishingShell Sites already listed sites; this slice adds operator **Filter Sites** (name/id substring), empty-match state, Vitest, Playwright surface spec, and product-docs.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None blocking.

- Filter is client-side on already-loaded `fetchSites()` — no new REST; matches slice (REST only if needed).
- `filterSitesByName` now matches name, `id`, and `siteId` (case-insensitive). Behavioral tests cover empty, name, and id/siteId.
- `SitesSection.filter.test.tsx` covers UI filter + no-match empty testid.
- Playwright `sitesListSearch.spec.js` uses SPA `entry=publish`, testids, pageerror/console listeners.
- Product-docs `admin-publishing` documents Filter Sites vs Logs filter.
- No filesystem path I/O. Cross-platform path checklist: N/A (no new path code).

## Memory patterns hit

None for this UI-only filter.
