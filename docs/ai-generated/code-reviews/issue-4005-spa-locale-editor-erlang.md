# Erlang review — #4005 CD-18 SPA locale editor

Independent of implementer. Parent #1690.

## Verdict

**Pass** after live H2 proof. Hard gates: unit tests, portable I/O (N/A), C5 Playwright.

## Bugs found and fixed this slice

1. **POST/PUT body must wrap `{ LocaleDetail: … }`.** A flat JSON object fails JAXB (`unexpected element label`). Client now uses `wrapLocaleDetailForWire`.
2. **Design WS `createLocales` treated `Optional.empty()` as exists** (`found != null` is always true). Every create was HTTP 409. Fixed with `localeLanguageAlreadyExists(Optional)` + unit tests.
3. **In-flight save guard** so a second click cannot POST create twice (409 on a brand-new language).

## Checklist

- [x] Create/save/delete chrome on LocalesPanel / LocaleDetailPanel
- [x] Language immutable after create; save disabled until language+label valid
- [x] 409 duplicate and 404 missing surfaced
- [x] Vitest (panels + localesApi + Optional helper)
- [x] Playwright `developer-locale-editor.spec.js` 2 passed on H2 qa-up
- [x] product-docs 8.2 admin Developer Locales
- [x] Auto-translation editor **not** in this slice

## C5

`qa-up --skip-image-build --then-qa-deploy-webui` TEST_CMS_URL=http://127.0.0.1:9993; docker cp rest + sitemanage + perc-system; in-cell StopJetty/StartJetty; qa-health RESULT:OK HTTP:200 HEALTH:healthy; `npm run test:surface -- --path tests/developer-locale-editor.spec.js` **2 passed**; console-clean=yes; server.log-clean=yes (no locale ERROR/FATAL).
