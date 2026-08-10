# Erlang review: fix/issue-1876-home-gadget-locale-playwright

## Summary

Playwright residual locale regression for Home gadget body/modal keys (#1876 / parent #1852), plus optional `locale` on auth login helpers and pure `pick-locale-tag` unit tests.

## Scope

- `modules/perc-qa-automation/frontend/tests/bugs/bug-1876-home-gadget-locale.spec.js` (new)
- `modules/perc-qa-automation/frontend/tests/helpers/pick-locale-tag.js` (new)
- `modules/perc-qa-automation/frontend/tests/unit/pick-locale-tag.test.js` (new)
- `modules/perc-qa-automation/frontend/tests/helpers/auth.js` (locale option)
- `modules/perc-qa-automation/frontend/package.json` (unit test path)
- `modules/perc-qa-automation/README.md` (how to run surface path)

Cross-platform path review: no new path joins or install I/O; auth already uses `path.join`. Spec uses BASE_URL only.

## Recommendation

**approve**

## Gate

- Behavioral unit tests: pure pick-locale-tag helpers covered (node:test). Playwright is live-CMS companion; listed via surface filter.
- No non-portable path/file I/O.
- Maven `modules/perc-qa-automation` clean install: SUCCESS.
- `npm run test:unit`: 58 pass.
- Live Playwright against qa-up not executed in this session (optional gate; documented).

## Issues

None at bug severity.

### suggestion

- Live QA run when stack available: `npm run test:surface -- --path tests/bugs/bug-1876-home-gadget-locale.spec.js`

## May commit/push

yes
