# Erlang review: #4536 PublishingShell item publishing history

**Date:** 2026-09-17  
**Reviewer:** Erlang (independent of implementer)  
**Issue:** https://github.com/intersoftdatalabs-in/percussioncms/issues/4536  
**Branch:** `fix/issue-4536-item-publishing-history`

## Summary

React item publishing history panel on Publish Status/Logs, using existing `GET …/itemmanagement/item/pubhistory/{id}`. Empty and error states are explicit. Playwright surface spec + product-docs. No schedule/takedown in this PR.

## Scope

- Modules: `WebUI`, `modules/perc-qa-automation`, `product-docs/8.2/admin/publishing.md`
- Cross-platform path review: URL/REST paths use `/` (correct). No OS filesystem joins. `encodeURIComponent` on item id. `mapIdParam` allowlist rejects `../` and XSS-shaped ids.
- Tests: Vitest for normalize/sort/hrefs, panel, shell, API GET. Playwright `tests/publishing/itemPublishingHistory.spec.js`.

## Recommendation

**Approve**

## Gate

**May commit/push: yes**

WebUI `mvnw clean install`: BUILD SUCCESS, Java Tests run: 69 Failures: 0, Vitest 4278 passed. perc-qa-automation `mvnw clean install`: BUILD SUCCESS (no live Playwright in Maven). Live QA H2 Playwright not executed this session (qa-health hung).

## Issues

None at `bug`. Playwright live run is remaining verification, not a missing spec.
