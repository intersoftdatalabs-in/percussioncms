# Erlang review — #4296 Playwright SY-06 workflow content types H2

**Reviewer persona:** Erlang (independent of implementer)
**Scope:** `modules/perc-qa-automation/frontend/tests/developer-workflow-content-types.spec.js` (+ stacked tip merges for C5)
**Date:** 2026-09-04

## Change class

Surface-filtered Playwright companion for Developer Workflow → Allowed content types (SY-06). Tips #4294/#4295 stacked for live H2 proof; unique delta is the Playwright spec.

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| Bug (C5) | Tip REST missing CXF `restWorkflowsResource` in `sitemanage-beans.xml` → SPA GET 404 | Fixed in this PR |
| Bug (C5) | Jetty 64-char session vs `PSX_LOCKS.LOCKSESSION` VARCHAR(50) → PUT 500 | Fixed: `clampLockSession` |
| Bug (C5) | CXF UNWRAP of JSON `[]` → null list → PUT 400 | Fixed: null list = clear |
| — | Surface spec: console guards + restore + `rx:` name normalize | Pass |
| — | Portable paths: URL/`BASE_URL` only | Pass |

## Companions checked

- Peer: `developer-content-type-workflows.spec.js` (CD-08 CT→workflow)
- Peer: `bugs/bug-3562-developer-workflow-detail.spec.js` (workflow open)
- Tip REST/SPA stacked from #4298/#4299; wire gaps closed for C5

## Gate

**PASS** — C5 surface 2/2 green on H2 QA.

