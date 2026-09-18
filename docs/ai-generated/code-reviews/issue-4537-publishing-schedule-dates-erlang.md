# Erlang review: #4537 PublishingShell schedule publish dates

**Date:** 2026-09-18  
**Reviewer:** Erlang (independent of implementer)  
**Issue:** https://github.com/intersoftdatalabs-in/percussioncms/issues/4537  
**Branch:** `fix/issue-4537-publishing-schedule-dates`

## Summary

React schedule publish/removal dates panel on the Publishing **site workspace**, using existing `GET …/getitemdates/{id}` and `POST …/setitemdates`. Invalid dates (HTTP 400) and forbidden (HTTP 403 / application `FORBIDDEN`) are errors, not success. Playwright surface spec + product-docs. No takedown, no Explorer schedule rewrite.

## Scope

- Modules: `WebUI`, `modules/perc-qa-automation`, `product-docs/8.2/admin/publishing.md`
- Cross-platform path review: URL/REST paths use `/` (correct). No OS filesystem joins. `encodeURIComponent` on item id. `mapIdParam` allowlist rejects `../` and XSS-shaped ids. Deep-link hrefs are SPA URL paths.
- Tests: Vitest for API 400/403, panel load/save/range, hrefs, `itemPublishPaths` dates, SiteWorkspace panel presence. Playwright `tests/publishing/itemScheduleDates.spec.js`.
- Memory patterns hit: UI companions (Vitest + Playwright + product-docs); HTTP 200 application-level FORBIDDEN treated as failure; `formatApiError` for plain `ApiError` objects.

## Recommendation

**Approve**

## Gate

**May commit/push: yes**

WebUI `mvnw clean install`: BUILD SUCCESS, Java Tests run: 69 Failures: 0, Vitest 4403 passed. perc-qa-automation `mvnw clean install`: BUILD SUCCESS (no live Playwright in Maven).

## Issues

None at `bug`.
