# Erlang review: #4538 PublishingShell site-workspace takedown

**Date:** 2026-09-18  
**Branch:** `fix/issue-4538-publishing-takedown`  
**Base:** `origin/main`  
**Reviewer:** Erlang (independent of implementer)

## Summary

PublishingShell site workspace gains an item **Take down / Remove From Site** panel that reuses Explorer `takedownSelectedItem` / `loadLinkedPagesForTakedown` (GET/PUT sitemanage `takedown/page|resource/{id}` + `findLinkedItems`). Confirm lists up to ten linked paths. HTTP 200 `FORBIDDEN` / `BADCONFIG` / `NOSTAGING_SERVERS` / `INVALID` and HTTP 403 are treated as failures. Companions: Vitest (panel + href/kind + SiteWorkspace mount), Playwright `@publishing-takedown`, `product-docs/8.2/admin/publishing.md`.

## Scope

- Uncommitted WebUI TS/TSX, perc-qa-automation Playwright spec, product-docs
- Memory patterns hit: change-class closure (WebUI screen + Playwright + product-docs); HTTP 200 preflight not success; CMS `/` paths are URI/CMS not filesystem
- Prior report: none for #4538
- Cross-platform path review: CMS item paths (`/Sites/{id}`, `/Assets/{id}`) and SPA hrefs use `/` as URL/CMS paths — allowed. No OS filesystem joins, no Unix-only temps, no `:`-only lists.

## Recommendation

**approve**

## Gate

- Bugs: none
- Missing behavioral tests: no (panel: need-id, linked review, page GET contract, PUT linked, resource kind, FORBIDDEN/BADCONFIG/INVALID/403; href/kind helpers; 10-path cap)
- Non-portable path I/O: none
- **May commit/push: yes**

## Issues

None at bug severity.

### suggestion

- Playwright happy-path does not assert PUT body when linked pages exist (Vitest covers PUT). Optional live assertion on request method.

### nit

- `onTakeDown` when `!reviewed` only runs review (user must click Take down again). Matches Load-then-Save schedule panel; submit is hidden until review.

## Change-class closure

| Companion | Present |
|-----------|---------|
| Shared takedown contract (`itemPublish.ts`) | yes |
| SiteWorkspace panel | yes |
| Vitest | yes |
| Playwright surface spec | yes |
| product-docs 8.2 admin publishing | yes |
| REST/sitemanage new adaptor | N/A (existing GETs) |
