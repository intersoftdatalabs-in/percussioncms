# Erlang review — #3520 Page site create

**Branch:** `feat/issue-3520-page-site-create`  
**Scope:** uncommitted vs `origin/main` (WebUI wizard, validation, site create body, Playwright, product-docs)  
**Date:** 2026-08-17  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (Vitest + Playwright + product-docs); do not invent REST resources; Traditional must not regress.

## Summary

Slice 2 of #3512. Adds a Create Site type picker on main (slice 1 #3523 not merged) and enables **Page**: managed navigation locked on, page/base template step restored, `pageBased: true` on existing `POST /sitemanage/site/`. Traditional skips the template step and omits `pageBased`. Virtual remains blocked.

No Java API shape change. `PSSitePublishDao.saveSite` still hardcodes `setPageBased(true)` for all sitemanage creates (including classic `perc_newsitedialog`). Sending `pageBased: true` documents the Page contract without regressing Traditional persistence.

## Issues

None that block.

### Notes (non-blocking)

- Server still persists every sitemanage create as page-based. Traditional vs Page is UX + request field until a later slice honors `pageBased=false` without breaking classic CM1 create (which omits the field).
- Type picker is included because slice 1 is not merged. Expect merge conflict with #3523 (same files; Page is additive).

## Tests

- Vitest: type picker, Traditional skip-template + nav opt-out, Page lock-nav + template required + `pageBased` body, Virtual blocked, a11y gate.
- Playwright: Traditional chrome/happy path updated; new `explorer-site-create-page.spec.js` (picker + live Page create).
- product-docs 8.2 admin Create Site + Navigation + reference site-config.

## Cross-platform path checklist

N/A — no filesystem path I/O. URL paths use `/` (REST/SPA). Playwright uses `path.join` only in existing helpers.

## Change-class companions

WebUI screen: Vitest + Playwright + product-docs present.
