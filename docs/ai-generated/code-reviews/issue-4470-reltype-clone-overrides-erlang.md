# Erlang review — issue 4470 relationship-type cloning field overrides

## Summary

Admin can GET/PUT cloning field overrides (`PSCloneOverrideField`) on user
relationship types via REST, Developer SPA editor, Playwright, and product-docs.
`DESIGN_GAPS` drops the cloning-override line; effect-condition gap remains
(#4471). SPA coerces a one-element `designGaps` string so Jackson/JAXB unwrap
cannot crash the detail panel (`gaps.map`).

## Scope

Uncommitted work on `feat/issue-4470-reltype-clone-overrides` vs `origin/main`.
Modules: `rest`, `projects/sitemanage`, `WebUI`, `modules/perc-qa-automation`,
`product-docs/8.2/`.

Memory patterns hit: change-class companions (REST DTO + adaptor + Spring stub
already present + SPA + Playwright + product-docs); Jackson single-element list
unwrap; QA hot-deploy SPA after Jetty restart.

Cross-platform path review: no new filesystem path joins; Playwright uses
`path` filter only; clone override params are product URL-style strings, not OS
paths.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None at bug severity.

- suggestion: `copyRules` uses raw `PSCollection` like surrounding objectstore
  code; acceptable for this slice.
- nit: `RT_GAP_CLONE` remains in `messages.ts` for TMX stability; unused as SPA
  fallback.

## Tests / evidence

- rest `mvnw clean install` BUILD SUCCESS, Tests run: 1397
- sitemanage `mvnw clean install` BUILD SUCCESS, Tests run: 2728
- WebUI `mvnw clean install` BUILD SUCCESS, Vitest 4242, Java Tests run: 69
- perc-qa-automation `mvnw clean install` BUILD SUCCESS
- Playwright `npm run test:surface -- --path tests/developer-relationship-type-editor.spec.js`
  6 passed on QA H2 (`TEST_CMS_URL=http://127.0.0.1:9993`); console-clean=yes;
  server.log-clean=yes
