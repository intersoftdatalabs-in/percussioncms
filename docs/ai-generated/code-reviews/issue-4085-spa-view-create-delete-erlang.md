# Erlang review — issue #4085 SPA UI-07 view create/delete

**Date:** 2026-08-31  
**Branch:** `feat/issue-4085-spa-view-create-delete`  
**Base:** `origin/main`  
**Reviewer:** Erlang (independent of implementer)

## Summary

Developer **Views** catalog gains SearchesPanel-peer create (POST `/services/views`) and
detail delete (DELETE), plus identity-field save (PUT). REST write is reused, not
re-implemented. Inbox-family / custom-URL views hide Delete. Vitest covers 400 / 409 /
404 / 403 and protected Inbox. Playwright surface spec + product-docs 8.2 admin/REST
pages ship in the same change class.

## Scope

- Uncommitted WebUI SPA (`viewsApi.ts`, `ViewsPanel.tsx`, `ViewDetailPanel.tsx`,
  `messages.ts`) and matching Vitest
- `modules/perc-qa-automation` Playwright `developer-view-editor.spec.js`, catalog
  smoke views section, smoke-set inventory
- `product-docs/8.2` Developer Views admin page + REST/index links
- Memory patterns hit: change-class closure (Playwright + product-docs + Vitest);
  behavioral tests for write status codes; no filesystem path joins in this diff
- Prior report: none for this ticket

## Recommendation

approve

## Gate

May commit/push: **yes** (no bug-severity findings; behavioral tests present;
Playwright companion present; product-docs present)

## Cross-platform path review

No new filesystem path construction. URL/REST paths correctly use `/`. Playwright
`BASE_URL` + `/Rhythmyx/cm/app/spa.jsp` is a URL, not an OS join. Unique names are
ASCII `[a-z0-9]` suffixes (no path separators).

## Issues

_(none at bug severity)_

### suggestion

1. Playwright Inbox assertion assumes the H2 catalog still lists `Inbox`. REST
   adaptor synthesizes Inbox when design-WS collapses siblings; if a future cell
   omits that row, treat as product/adaptor defect (do not flake-skip). No code
   change required for this slice.

### nit

1. `VIEW_DESIGN_GAPS` still documents Inbox-family mutate as a gap (correct —
   REST 409). Keep that distinct from the dropped create/update/delete claim.
