# Erlang review — #4091 display-format DELETE persist

**Branch:** `fix/issue-4091-display-format-delete-persist`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Change class:** sitemanage apibridge DELETE persist (Workbench component XML) + Playwright companion + product-docs REST notes  
**Memory patterns hit:** behavioral tests for new persist path; change-class Playwright + product-docs companions; do not treat locator-only SOAP as success when child XML app fails  

## Summary

Admin REST `DELETE /services/displayformats/{idOrName}` previously called locator-only `IPSUiDesignWs.deleteDisplayFormats`. `updateDisplayFormats` is an XML datasource (`PSTransactionSet`: `Xml Document Expected, none supplied`), so DELETE returned success-shaped errors or 400 on mark-for-deletion + `saveDisplayFormats` (that save still locator-deletes first when a lock version is present). GET stayed 200.

This slice loads with a design lock, `markForDeletion()`, and persists via `PSComponentProcessorProxy.delete(IPSDbComponent)` (Workbench objectstore XML). Locator `deleteDisplayFormats` is not used on the REST single-delete path. Playwright creates a **user** format via POST (does not re-implement SPA create chrome, does not delete packaged system formats), asserts DELETE 204, GET 404, catalog omits the row. `product-docs/8.2/developer/rest.md` updated.

## Recommendation

approve (re-review after live H2)

## Gate

**May commit/push: yes**

## Re-review

Live H2 showed locator XML delete of a **replayed** `By_Type` with `displayId=-1` (GET-by-key replay). Added `identityMatchesKey` / catalog exact-name copy and `nativeForDelete` stub from catalog `displayId`. Playwright: 1 passed (DELETE 204, GET 404, catalog omits row). server.log: `XML delete name=qa4091… deletedRows=1`; no new ERROR/FATAL for the path.

- Behavioral unit tests cover mark-for-deletion XML persist hook, no locator/saveDisplayFormats on REST delete, unknown/lock/in-use/non-admin, persist failure mapping.
- Standalone `projects/sitemanage` `mvnw clean install` BUILD SUCCESS (Tests run: 2013, Failures: 0, Errors: 0, Skipped: 125; `DisplayFormatAdaptorWriteTest` 21/0).
- Playwright spec added under `modules/perc-qa-automation` (C5 live H2 required before PR).
- Cross-platform path checklist: N/A (no filesystem path joins; URL paths correctly use `/`).
- No agent-rule file changes.

## Issues

None (hard-gate).

### Nits (non-blocking)

- `DisplayFormatResource` OpenAPI text still mentions `IPSUiDesignWs.deleteDisplayFormats`. REST module was intentionally not retouched; product-docs is the operator source of truth for this slice.
- Production `deleteLockedViaComponentXml` (dependency check + processor + lock release) is exercised live in Playwright, not with a real processor in Surefire (injected hook in unit tests). That is the same adaptor-test split as other design-WS writes.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Playwright uses URL paths with `/` (correct)
- [x] Unique REST names are alphanumeric (`qa4091…`); no OS path assertions
