# Erlang review — issue #1695 explorer encodePath verify (Playwright)

**Branch:** `fix/issue-1695-explorer-encodepath-verify`  
**Date:** 2026-08-06  
**Reviewer persona:** Erlang (strict gate)  
**Scope:** `modules/perc-qa-automation/frontend` only (verify PR; no product churn)

## Summary

Thin verification residual after encodePath (#1680) and formatApiError (#1691). Extends `bug-1622-explorer-root-folders` with REST root-name checks, SPA network capture (no `folder//`), human-readable tree error assertion, and pure helpers + Node unit tests.

## Recommendation

**approve**

## Gate

| Check | Result |
|-------|--------|
| Bugs in new logic | none |
| Behavioral unit tests for new helpers | yes (`tests/unit/pathmanagement-url.test.js`) |
| Cross-platform path/file I/O | n/a — pure URL string helpers; no filesystem I/O |
| Module standalone clean install | `modules/perc-qa-automation` `mvnw clean install` green |
| Live evidence | H2 qa-up `TEST_CMS_URL=http://127.0.0.1:9993` — 2 Playwright + unit suite green |

**May commit/push: yes**

## Scope (files)

| Path | Change |
|------|--------|
| `modules/perc-qa-automation/frontend/tests/bugs/bug-1622-explorer-root-folders.spec.js` | extend REST + UI network / roots / error readability |
| `modules/perc-qa-automation/frontend/tests/helpers/pathmanagement-url.js` | pure double-slash + human-error helpers |
| `modules/perc-qa-automation/frontend/tests/unit/pathmanagement-url.test.js` | Node unit coverage |
| `modules/perc-qa-automation/frontend/package.json` | include unit file in `test:unit` |

## Issues

None (bug/suggestion).

### Nits

- **nit** — UI root-node match is slightly loose (testid substring). Acceptable for stock CMS paths; REST already hard-asserts names.

## Cross-platform path review

No file I/O or OS path joins in this diff. Regexes operate on URL strings with `/` separators (HTTP URLs), which is correct and portable.

## Live run evidence (agent)

```
TEST_CMS_URL=http://127.0.0.1:9993 ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from docker>
npx playwright test tests/bugs/bug-1622-explorer-root-folders.spec.js
→ 2 passed (~1.9s)

npm run test:unit → 55 pass (includes pathmanagement-url)
```

REST root children observed: Sites, Assets, Design, Search, Recycling.  
`folder//Sites` → 400; SPA requests used single-slash `folder/` / `folder/Sites`.
