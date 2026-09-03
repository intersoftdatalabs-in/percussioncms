# Erlang review — issue #4253 Playwright SY-03 relationship type write H2

## Summary

Deepens `modules/perc-qa-automation/frontend/tests/developer-relationship-type-editor.spec.js`
into a dedicated H2 surface for SY-03: system immutability, create/edit/delete with
cloning flags, copy-from-system, duplicate 409, and invalid-name validation.
Stacks on REST #4251 / SPA #4252 tips.

## Scope

- Diff: one Playwright spec under `modules/perc-qa-automation/frontend/tests/`
- Peer patterns: `developer-locale-editor.spec.js`, `developer-control-create.spec.js`
- Live proof: `npm run test:surface -- --path tests/developer-relationship-type-editor.spec.js`
  → 5 passed on H2 QA (`TEST_CMS_URL=http://127.0.0.1:54193`); console-clean; no
  relationship-type ERROR/FATAL in server.log window
- Cross-platform path review: N/A — no filesystem path construction; URL helpers only

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.
