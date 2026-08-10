# Erlang review: issue #2194 demo-sites Playwright residual

**Date:** 2026-08-06  
**Branch:** `fix/issue-2194-demo-sites-playwright-sample-site`  
**Reviewer persona:** Erlang (self-review before commit)

## Summary

Adds black-box Playwright residual for parent #1750: REST `path/folder/Sites` and
Explorer UI assert Corporate/Enterprise Investments after demo-sites install.
Pure helpers + unit tests; soft skip-with-BUG when sample data absent (default
qa-up / pre-#2192 image); hard fail when `EXPECT_DEMO_SITES=1`.

## Scope

- `modules/perc-qa-automation/frontend/tests/helpers/demo-sites.js` (new)
- `modules/perc-qa-automation/frontend/tests/unit/demo-sites.test.js` (new)
- `modules/perc-qa-automation/frontend/tests/bugs/bug-1750-demo-sites-sample-site.spec.js` (new)
- `modules/perc-qa-automation/frontend/package.json` (test:unit paths)
- `modules/perc-qa-automation/README.md` (env recipe)

Cross-platform path review: no new filesystem path I/O; URL builders use BASE_URL
string concat consistent with peer bug-1622; portable.

## Recommendation

**approve**

## Gate

- Bugs: none
- Behavioral unit tests: yes (`demo-sites.test.js`; empty Sites fails helpers;
  skip reason embeds durable issue URLs)
- Non-portable paths: none
- May commit/push: **yes**

## Issues

None blocking.

### suggestion (docs)

Live H2 silent `--demo-sites` re-verify still depends on #2192 merge into the
image under test; residual correctly documents `EXPECT_DEMO_SITES=1` rather than
claiming full E2E install in CI.

## Gates evidence

- `npm run test:unit` — 91 pass (includes demo-sites + pathmanagement-url)
- `npx playwright test --list tests/bugs/bug-1750-demo-sites-sample-site.spec.js` — 2 tests
- `cd modules/perc-qa-automation && ../../mvnw clean install` — BUILD SUCCESS

