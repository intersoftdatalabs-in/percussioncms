# Erlang review — issue #4221 (SPA UI-05 display format sort-order)

**Branch:** `feat/issue-4221-display-format-sort-order`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Recommendation:** approve  
**Gate:** pass — May commit/push: yes  
**Memory patterns hit:** change-class companions (SPA + adaptor persist + Vitest + Playwright + product-docs); packaged formats stay read-only.

## Summary

Developer Display Formats lets Admin set default sort column and direction on
user formats and persist via PUT `columns` + `sortedColumnNames`. Packaged
names stay read-only. `DF_GAP_COLUMNS_EDIT` is dropped from the detail panel.
REST GET now returns `sortedColumnNames`; PUT applies Workbench `sortColumn` /
`sortDirection` when `sortedColumnNames` is present.

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Cross-platform path checklist

Not applicable — no new filesystem path construction.

## Tests / evidence

- WebUI `mvnw clean install`: BUILD SUCCESS, Vitest 3769 passed
- sitemanage `mvnw clean install`: BUILD SUCCESS, `DisplayFormatAdaptorWriteTest` 45 passed
- Playwright `test:surface --path tests/developer-display-format-sort.spec.js`: 2 passed; console-clean; no new display-format ERROR in recent `server.log`
