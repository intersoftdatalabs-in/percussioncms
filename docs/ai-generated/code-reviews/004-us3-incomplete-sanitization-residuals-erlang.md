# Erlang review — 004/us3-incomplete-sanitization-residuals

**Date:** 2026-07-18  
**Branch:** `004/us3-incomplete-sanitization-residuals`  
**Scope:** uncommitted WebUI JS fixes for remaining open `js/incomplete-sanitization` CodeQL alerts (#1110–#1113, #1115–#1116, #1134–#1135, #1454, #1456, #1467–#1468, #1470, #1485)  
**Reviewer:** Erlang (independent pre-commit)

## Summary

Closes all 14 remaining open GitHub CodeQL `js/incomplete-sanitization` alerts by converting first-only `.replace(string, …)` / incomplete regex-escape patterns to global regex replaces, full RegExp-escape, or identifier allow-lists. Lockstep product copies under `cm/`, `cm/app/…/legacy/`, `cm/pages/…`, and `WebUI/war/**` are updated together. Regression coverage: `percGetDashboardColumn.test.js` (extended) + new `percIncompleteSanitization.test.js` (43 tests, all green).

## Recommendation

**approve**

## Gate

| Check | Result |
|-------|--------|
| Bugs | none |
| Behavioral unit tests for non-trivial logic | present |
| Cross-platform path/file I/O | N/A (string sanitisation only; test paths use `path.resolve` / `fileURLToPath`) |
| May commit/push | **yes** |

## Issues

None (blocking).

### Nits (non-blocking)

1. **perc_common_ui.js is a committed concatenation** that includes vendored moment `unescapeFormat`. The one-line global-replace fix is correct for the open alert; a longer-term paths-ignore or regenerate-from-source path (similar to `shared-common.js`) would reduce residual risk if more moment alerts appear. Out of scope for this residual close.
2. **siteimprove `dataStr.replace(/\\/g, "")`** preserves the historical intent (strip backslashes before `JSON.parse`) and only makes it complete. If metadata ever contains legitimate escaped JSON, this strip is still lossy — pre-existing design, not introduced here.
3. **Unrelated untracked** `docs/ai-generated/code-reviews/pr-1356-…` and `pr-1358-…` were left out of this commit (belong to other work).

## Memory patterns hit

- Prefer runtime global replace / allow-list over dismiss-only for `js/incomplete-sanitization` (prior PRs #1307, #1323).
- Lockstep all product copies that CodeQL still scans (`cm/` + legacy + war mirrors where present).
- Source-pattern + behavioral tests that fail on pre-fix first-only replace.

## Cross-platform path checklist

Not applicable — no filesystem path construction, no OS temp hardcodes, no path string assertions against OS separators.

## Tests run

```text
cd WebUI && npm test -- --run src/test/js/percGetDashboardColumn.test.js src/test/js/percIncompleteSanitization.test.js
# 43 passed
```
