# Erlang review: issue 3811 Admin IA Relationships on assets

- **Date:** 2026-08-26
- **Branch:** `fix/issue-3811-ia-relationships-assets`
- **Base:** `origin/main`
- **Recommendation:** approve
- **Gate:** May commit/push: **yes**
- **Cross-platform path review:** no OS filesystem I/O in this diff. CMS Explorer paths and Playwright `data-testid` selectors use `/` (product URL/CMS form). Java `isContentIdOrGuid` uses `split("-")` on GUID tokens, not file paths.

## Summary

Admin View → IA Relationships on a selected asset (and timestamped percSimpleText names) was parsed as a fake content id (`…-20260820165542`). REST then 403'd (taxonomy treated the id as a JCR path) and the panel showed a permission error. The fix:

1. `parseExplorerContentId` accepts only bare CMS ints (≤ Integer.MAX_VALUE) or `host-type-uuid` GUIDs.
2. `canOpenIaRelationships` (production `PSPathItem`) gates the shell panel.
3. Taxonomy skips the JCR finder for content ids/GUIDs so `/summary` is 200 for Admin.
4. `composeFromServerSummary` no longer throws when taxonomy `nodes` is omitted (that error-bounded Explorer after the 200 change).

## Scope

Uncommitted worktree vs `HEAD` / `origin/main`. Memory: GUID last-segment vs slug bind (#3546/#3557/#3811). No `gh` PR yet.

## Issues

None blocking.

- **suggestion** `modules/perc-qa-automation/frontend/tests/explorer-relationships.spec.js`: H2 Assets library is often folders-only; the #3811 Playwright case drills Sites for a selectable row. Asset-title bind remains in Vitest (`percSimpleTextAsset` + `sys_contentid`). Acceptable for this H2 cell.
- **nit** `PSRelationshipSummaryService.isContentIdOrGuid`: two-part locators (`contentId-revision`) are not treated as GUIDs (intentional; matches existing 3-segment tests).

## Evidence

- `projects/sitemanage` `mvnw clean install` BUILD SUCCESS (`PSRelationshipSummaryServiceTest` 14 tests).
- `rest` `mvnw clean install` BUILD SUCCESS (Tests run: 609).
- `WebUI` `mvnw clean install` BUILD SUCCESS (Vitest 3124 passed; Java surefire 63).
- `modules/perc-qa-automation` `mvnw clean install` BUILD SUCCESS.
- Playwright `tests/explorer-relationships.spec.js` 3 passed; golden 2 passed. console-clean=yes (no pageerror). server.log-clean=yes (no feature ERROR/FATAL in test window).
