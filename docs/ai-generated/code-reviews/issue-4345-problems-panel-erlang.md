# Erlang review — #4345 Problems panel

**Scope:** uncommitted `feat/issue-4345-problems-panel-design-validation` vs `HEAD` / `origin/main`.  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** REST adaptor + sitemanage apibridge + rest MainTest Spring stub + CXF `jaxrs:serviceBeans` ref; Admin-only catalog; no rest→sitemanage cycle; Playwright surface filter; product-docs 8.2.

## Summary

Vertical increment for Workbench §12.4: Admin `GET /services/problems`, sitemanage `ProblemsAdaptor`, Developer SPA Problems tab, Vitest, H2 Playwright, product-docs. Known invalid-session fixture always listed; navigate-to-source opens Content Types when the peer section is allow-listed.

## Issues

None blocking.

## Cross-platform path checklist

- No new filesystem path join/I/O in production Java or tests.
- Unsafe fixture/token checks treat `/`, `\`, `..`, drive-letter `C:`, and JDBC-like text as 400 without echoing secrets.
- Playwright URL helpers use `/` for HTTP paths only.

## Tests

- rest: `ProblemsResourceTest` (9) + `TestProblemsAdaptor` Spring stub; module `Tests run: 1203, Failures: 0`
- sitemanage: `ProblemsAdaptorTest` (6) + `CatalogRestJaxrsRegistrationTest`; module `Tests run: 2456, Failures: 0, Skipped: 125`
- WebUI Vitest: 4078 passed
- perc-qa-automation unit: 522 passed; Playwright surface 3 passed; golden 2 passed
