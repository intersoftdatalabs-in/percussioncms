# Erlang review: #3446 Inbox execute + results on H2

**Branch:** `fix/issue-3446-inbox-execute-results-h2`  
**Date:** 2026-08-15  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** no blocking bugs; behavioral unit + hardened Playwright present.

## Summary

H2 QA `GET /services/views` listed seven remapped `View_All` rows and omitted Inbox even though `PSX_SEARCHES` seeds Inbox. Explorer injected a stub leaf; click POSTed execute and 404'd; `explorer-inbox.spec.js` soft-skipped. This slice reconciles `findViews` names with `loadViews` results, synthesizes a runnable Inbox design (`../sys_cxViews/inbox.xml`) when missing, hardens Playwright to skip only when the catalog truly omits Inbox, and updates operator/integrator docs. Gap-matrix stays not Present.

## Cross-platform path checklist

- No new filesystem path construction.
- Inbox URL/DCE path use `/` only (`../sys_cxViews/inbox.xml`, `//Views//MyContent/Inbox`).
- Playwright helpers join CMS origin + URL paths (not OS file I/O).

## Issues

None blocking.

## Tests

- `ViewAdaptorExecuteTest` — list/execute Inbox when `loadViews` remaps to `View_All`; empty-catalog Inbox; reconcile/well-known URL.
- `explorer-inbox.test.js` — skip only for missing catalog; expand-if-collapsed; execute URL + wrap.
- Live H2: `GET /services/views` includes Inbox; `POST .../Inbox/execute` 200 empty; `npm run test:surface -- --path tests/explorer-inbox.spec.js` 2 passed (no skip).

## Memory patterns hit

- Change-class companions: sitemanage adaptor + unit tests + perc-qa-automation spec/helpers + product-docs.
- Do not rewrite C1 `runCustomUrlView` from scratch; restore catalog/execute resolution.
- Playwright must not toggle an already-expanded Views group (hides Inbox).
