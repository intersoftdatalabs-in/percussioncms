# Erlang review — issue #2094 Spanish locale Playwright smoke

**Date:** 2026-08-06  
**Branch:** `fix/issue-2094-spanish-locale-playwright-smoke`  
**Scope:** uncommitted changes under `modules/perc-qa-automation/`  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**  
**Memory patterns hit:** QA TEST_CMS_URL freeport (not hard-coded :9993); companion Playwright for i18n surface residual; no monorepo reformat.

## Summary

Adds Playwright Spanish locale smoke residual for #961 after TMX (#2092) and Finder display wiring (#2105):

- `auth.js`: optional `{ locale }` on login helpers; `pickSpanishLoginLocale`; modern LocaleSelect option by testid.
- `bug-2094-spanish-locale-finder-dashboard.spec.js`: (1) I18N + optional classic Finder display helper / DOM for root labels; English path identity on explorer tree; (2) default Dashboard gadgets Spanish chrome (License Monitor optional).

## Issues

None (bug / missing behavioral test / non-portable path).

### Nits (non-blocking)

- `expectHiddenLocale` is best-effort; login still proceeds if hidden lag — acceptable for smoke.
- Modern Content Explorer tree still renders API `folder.name` (English); smoke gates TMX residual + path identity + classic helper when present. Full modern-tree label wiring is out of this residual's product scope.

## Cross-platform path checklist

No new file I/O or path joins. Env URL via existing `resolveCmsBaseUrl` / `BASE_URL`. **Clean.**

## Gates evidence

|                            Gate                             |     Result     |
|-------------------------------------------------------------|----------------|
| Live Playwright (`TEST_CMS_URL=http://127.0.0.1:9993`)      | 2 passed       |
| `npm run test:unit`                                         | 48 passed      |
| `cd modules/perc-qa-automation && ../../mvnw clean install` | BUILD SUCCESS  |
| Surface filter list                                         | 2 tests listed |

