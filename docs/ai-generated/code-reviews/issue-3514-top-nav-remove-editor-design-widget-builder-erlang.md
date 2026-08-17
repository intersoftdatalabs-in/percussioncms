# Erlang review — issue #3514 top-nav chrome

**Branch:** `fix/issue-3514-top-nav-remove-editor-design-widget-builder`  
**Base:** `origin/main`  
**Scope:** uncommitted WebUI top-nav / Developer sub-entries, landing-select coordination, Playwright, product-docs 8.2  
**Memory patterns hit:** change-class closure (WebUI + Playwright + product-docs); behavioral tests for nav gates; no non-portable path I/O

## Summary

Removes Editor, Design, and Widget Builder from product top nav. Editor remains reachable from Explorer / Preview / Home Create. Design and Widget Builder reuse existing SPA routes (`/design`, `/widget-builder`) as Developer sub-entries. Profile and Users landing selects no longer offer Editor/Design as new choices (stale stored values still list). Login / community switch untouched.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None blocking.

### Tests

- Vitest covers `topNavItemIds`, TopNav chrome absence, `isWidgetBuilderDeveloperEntry`, Developer related links, landing-option gates + stale current.
- Playwright `top-nav-restructure.spec.js` asserts remaining chrome and Developer → Design (optional Widget Builder). Design library spec waits on `perc-spa-topnav` instead of `nav-design`.
- Cross-platform path checklist: N/A (no filesystem I/O; SPA/URL paths correctly use `/`).

### Change-class closure

- Production: `topNavConfig.ts`, `TopNav.tsx`, `DeveloperRelatedLinks.tsx`
- Tests: Vitest + Playwright surface + golden smoke (live H2 QA)
- Product-docs 8.2 admin / getting-started / glossary / users-roles / design-templates

### Suggestions (non-blocking)

- `qa-health` reports pre-existing FastForward `PSDbStorageService` import ERRORs on first H2 cell start; HTTP 200 / Health=healthy; not feature-related.
- #3536 still owns adding Explorer to the profile landing list.
