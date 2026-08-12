# Erlang review — #3117 Explorer Views Playwright + a11y (V3)

**Scope:** uncommitted `modules/perc-qa-automation` spec/helpers/unit + README vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Date:** 2026-08-12

## Summary

Additive Playwright surface for Explorer Views catalog/run. Does not re-implement V2 product UI (#3116 / PR #3252). Soft-skips when `explorer-views-tree` is absent or the H2 catalog has no runnable standard view. Axe gate uses existing `expectNoSeriousA11yViolations`. `package.json` `test:unit` left unchanged to avoid same-file thrash with #3252.

## Issues

None (no bugs, no missing behavioral tests for new helper logic, no non-portable path I/O).

## Cross-platform path checklist

- [x] URL construction uses `/` (correct for URI paths)
- [x] No filesystem separator concatenation
- [x] Cache-buster encoded via `encodeURIComponent`
- [x] Tests assert URL strings, not OS file paths

## Memory patterns hit

- Soft-skip with explicit reason on missing fixture/chrome (saved-search peer)
- Do not click Inbox as a “standard view” fallback
- Avoid `package.json` `test:unit` list edits while a cluster PR owns that line

## Tests

- `node --test tests/unit/explorer-views.test.js` — 12 pass / 0 fail
- `npm run test:surface:list -- --path tests/explorer-views.spec.js` — 3 tests listed
