# Erlang review — issue 4873 Explorer multi-select force check-in

Machine pass: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --diff` (persona erlang 0.1.1).

## Summary

Machine analysis found 1 finding, 0 in-diff bugs.

## Scope

- Files: actionDispatch.ts, messages.ts, actionDispatch.test.ts, product-docs/8.2/admin/content-explorer.md, explorer-multi-force-checkin.spec.js
- In-diff findings: 0. Preexisting: 1 (`dispatchAction` cognitive complexity, actionDispatch.ts:1433, out of this slice's new helpers).
- Cross-platform path review: no new filesystem path construction.

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 — Severity: bug (preexisting, not in-diff)

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:1433
- Rule: complexity.cognitive
- Description: Function `dispatchAction` cognitive=307 (max 15).
- Status: open, not introduced by the force-check-in batch helper.
