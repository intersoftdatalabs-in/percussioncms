# Erlang review — #3613 US6 hard-cut T082b axe include

**Date:** 2026-08-19  
**Branch:** `fix/issue-3613-us6-hard-cut-axe-login`  
**Base:** `origin/main`  
**Reviewer:** Erlang (pre-commit / pre-PR)

## Summary

The FR-022 describe in `us6-hard-cut.spec.js` called `spa.jsp?entry=explorer`
without `loginAsAdmin`. Axe then `include`d
`[data-testid=content-explorer-shell], [data-testid=explorer-tree]` on the
Sign-in page and threw `No elements found for include`. The miller-column
describe already logged in; this residual adds the same `beforeEach`, waits
for `content-explorer-shell`, and uses a **single** axe include.

## Scope

- Uncommitted: `modules/perc-qa-automation/frontend/tests/us6-hard-cut.spec.js`
- Memory patterns hit: false-green on login snapshot; missing behavioral
  wait before axe include (peer: `us1-core-explorer.spec.js` T082b)
- Prior report: `docs/ai-generated/code-reviews/992-react-content-explorer-us6-hard-cut-erlang.md`
  (original US6 cutover; this is a test-host residual)
- Cross-platform path review: N/A (no file I/O / path joins)

## Recommendation

**approve**

## Gate

- Bugs: none
- Behavioral tests: the Playwright spec **is** the behavior (login + wait +
  single include). No new production logic.
- Non-portable paths: none
- **May commit/push: yes**

## Issues

None.

## Notes (not blocking)

- Miller-column `SHELLS` (including `siteArchitecture` →
  `perc-architecture-shell`) unchanged.
- T082b is not skipped. Login snapshot cannot pass: shell must be visible
  before axe.
- Console/pageerror assertion on T082b is extra C5 proof; filters known
  ResizeObserver / React DevTools noise only.

## Evidence (implementer)

- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` → BUILD SUCCESS
- `python docker/scripts/perc-devctl.py qa-health` → RESULT:OK HTTP:200 HEALTH:healthy
- `npm run test:surface -- --path tests/us6-hard-cut.spec.js` → 11 passed
- `python docker/scripts/perc-devctl.py qa-down` → RESULT:OK
