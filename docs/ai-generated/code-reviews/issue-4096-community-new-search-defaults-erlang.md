# Erlang review — issue #4096 community new-search defaults

**Branch:** `feat/issue-4096-community-new-search-defaults`  
**Scope:** uncommitted UI-09 Admin GET/PUT vs `origin/main`  
**Date:** 2026-09-01  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + IXxxAdaptor + sitemanage apibridge + Spring test stub + jaxrs serviceBeans ref); MainTest adaptor stub; Admin 403 / validation 400 behavioral tests; no WebUI Playwright required (no SPA)

## Summary

Adds Admin REST for Workbench UI-09 community CX new-search defaults. Persistence is the existing `PSSearch.cxNewSearch` property via `IPSUiDesignWs` load/save (same path as SOAP/Workbench). Does not create searches.

Change-class companions present: resource, adaptor interface, wire DTOs, Spring `TestCommunityNewSearchDefaultsAdaptor`, sitemanage `CommunityNewSearchDefaultsAdaptor` + tests, `rest-jax-rs` bean ref + `CatalogRestJaxrsRegistrationTest`, product-docs 8.2 REST + admin note.

## Cross-platform path checklist

- No filesystem path construction. Community/search keys reject `/`, `\`, `..`, and NUL as identity tokens (URL keys, not OS paths).
- Outcome: clean.

## Issues

None that block. Behavioral tests cover empty GET 200, unknown community 404, unknown/duplicate search 400, non-Admin 403, missing session 403, idempotent PUT (no save), replace/clear via design lock + save.

## Tests / builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 951, Failures: 0 (`CommunityNewSearchDefaultsResourceTest` 10; `MainTest` 2)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 2102, Failures: 0, Errors: 0 (`CommunityNewSearchDefaultsAdaptorTest` 18)

C5 Playwright: N/A (no WebUI/SPA this slice).
