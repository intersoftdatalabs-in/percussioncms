# Erlang review: #4057 AS-08 SPA template import/export

**Date:** 2026-08-31  
**Branch:** `feat/issue-4057-spa-template-import-export`  
**Base:** `origin/main`  
**Reviewer:** Erlang (pre-commit / night-issue-prs)

## Summary

Developer Templates catalog copies the CD-14 content-type import wizard onto AS-08 REST (`GET /services/templates/{idOrName}/export`, `POST /services/templates/import`). Export is a detail toolbar download. Import is create-only (no overwrite). Client strips exported binding `<id>` values so H2 PK collisions do not fail unique import. Vitest covers 400/409/404/403. Playwright H2 surface-filter passed.

## Scope

Uncommitted WebUI + perc-qa-automation + product-docs 8.2 vs `origin/main`.

Memory patterns hit: change-class companions (Playwright + product-docs), behavioral tests for 400/409/404/403, regex instead of `DOMParser` (CodeQL `js/xss-through-dom`), portable download (blob URL, not filesystem paths).

Cross-platform path review: no new filesystem path joins. `templateExportFilename` is an HTTP Content-Disposition basename sanitizer (quotes, controls, `/` `\` `:`), not OS path construction. Download uses `Blob` / `URL.createObjectURL`. Playwright uses `path.join` only via existing helpers.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None at bug severity.

### suggestion

SPA strips binding `<id>` so create-only import of a Workbench/REST export succeeds. REST `TemplateAdaptor.importTemplate` still persists XML binding ids when callers POST export XML without the SPA sanitizer. Out of scope for this slice (do not re-implement REST); integrators should omit binding ids or wait for a REST follow-up.

### nit

`qa-health` after an earlier failed unique import still matches the leftover `PSX_TEMPLATE_BINDING` ERROR in `server.log`. Subsequent unique import (after strip) did not add new ERROR lines.
