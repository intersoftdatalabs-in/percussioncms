# Erlang review: #3560 Explorer toolbar MENU parents

**Branch:** `fix/issue-3560-explorer-toolbar-menu-parents`  
**Date:** 2026-08-18  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI Vitest + Playwright + product-docs); do not flatten nested menus; Workflow one-click group stays.

## Scope

Uncommitted WebUI ActionToolbar / `filterToolbarActions` + perc-qa-automation surface + `product-docs/8.2/admin/content-explorer.md`. No assembler / `GET /actions/find` changes.

## Summary

Live H2 `GET /actions/find` already nests Paste / Arrange / View / Create. Residual fail was SPA chrome: `children` envelopes or dumped descendant roots rendered as top-level buttons. Filter now unwraps + collapses before toolbar enablement; ActionToolbar prepares the same tree. Workflow still uses the labeled one-click group (name match is case-insensitive). Playwright no longer soft-skips when the catalog has MENU parents; it requires the four static parents as `aria-haspopup=menu`.

## Issues

None (no bugs, missing behavioral tests, or non-portable path I/O).

## Cross-platform path checklist

N/A — no new filesystem path construction.

## Tests / evidence

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS; Surefire Tests run: 61; Vitest 2858 passed
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS
- H2 QA `TEST_CMS_URL=http://127.0.0.1:9993`: Playwright surface 2 passed; golden 2 passed; pageerror clean
