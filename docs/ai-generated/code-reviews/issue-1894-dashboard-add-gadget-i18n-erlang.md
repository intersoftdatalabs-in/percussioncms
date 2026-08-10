# Erlang review: issue #1894 Dashboard Add Gadget i18n Playwright

**Branch:** `fix/issue-1894-dashboard-add-gadget-i18n-playwright`  
**Date:** 2026-08-06  
**Reviewer persona:** Erlang (pre-commit self-review)

## Summary

Thin tests-only residual for merged PR #1863 / #1840: one Playwright bug spec under
`modules/perc-qa-automation/frontend/tests/bugs/` asserts that after Admin login
with a non-en-us locale (prefer `es`), `data-testid=dashboard-add-gadget` and the
modal title show the ship-matrix TMX segment (`perc.ui.dashboard.modern@Add Gadget`)
rather than bare English.

## Scope

|                                            Path                                             |       Change        |
|---------------------------------------------------------------------------------------------|---------------------|
| `modules/perc-qa-automation/frontend/tests/bugs/bug-1894-dashboard-add-gadget-i18n.spec.js` | New regression spec |

No product/WebUI/TMX source changes. No auth helper API expansion (locale login
stays local to the spec).

**Memory patterns:** WebUI Playwright HARD GATE companion for i18n after locale
change; QA mode env-only `TEST_CMS_URL` (no `DEV_PERCUSSION_INSTALL`).

**Cross-platform path review:** No file I/O or path joins; URLs built from
`BASE_URL` + fixed CMS path segments. Portable.

## Recommendation

**approve**

## Gate

- Bugs: none
- Behavioral coverage: live Playwright asserts label + modal title under locale
- Non-portable paths: none
- Maven: `modules/perc-qa-automation` standalone `mvnw clean install` green
- Unit: `npm run test:unit` 48/48 pass
- Live: Playwright against H2 matrix CMS (`TEST_CMS_URL=http://127.0.0.1:9993`) **pass**

**May commit/push: yes**

## Issues

None (bug severity).

### Nits (non-blocking)

1. Peer `bug-1812-bulk-upload.spec.js` still navigates to bare `/cm/app/home`,
   which 404s on current SPA entry contract. Out of scope for #1894; optional
   residual cleanup.

2. `bug-1608-1609-login-locale.spec.js` still uses `selectOption` on the modern
   LocaleSelect combobox; this PR correctly uses `data-testid=perc-login-locale-option-*`
   instead. Do not regress to native select APIs.

## Verification evidence

```
cd modules/perc-qa-automation && ../../mvnw clean install
cd frontend && npm run test:unit
TEST_CMS_URL=… ADMIN_PASSWORD=… npx playwright test \
  tests/bugs/bug-1894-dashboard-add-gadget-i18n.spec.js
# 1 passed
```

