# Erlang review — #3554 Slot detail non-array associations/designGaps

**Branch:** `fix/issue-3554-slot-detail-nonarray-lists`  
**Date:** 2026-08-18  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

Developer Slot detail crashed the Developer shell when Jackson returned a
truthy non-array for `associations` or `designGaps` (`(x || []).map` TypeError),
or a JAXB `{entry:[{key,value}]}` finder-arguments map (invalid React child).
The change normalizes those fields in `unwrapSlotDetail` and again in
`SlotDetailPanel` (so mocked GET payloads cannot throw). Slots detail is also
isolated with `DeveloperSectionErrorBoundary` (`developer-slot-detail-error`).
Structured `{code,message}` gaps still render via `formatDesignGap`.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs, no missing behavioral tests for the new list-normalization logic, no
non-portable path/file I/O.

## Cross-platform path checklist

Not applicable — no filesystem path joins, temp dirs, or path assertions.

## Memory patterns hit

- Behavioral unit tests for new/changed non-trivial logic (unwrap + panel + shell)
- Change-class closure: Vitest + Playwright companion for WebUI screen + product-docs
- Do not treat focused `-Dtest` as sufficient — full `WebUI` `mvnw clean install` green

## Issues

None (hard-gate).

### Notes (not blocking)

- `assemblyApi` re-exports `normalizeSlot*` for callers; panel imports `slotLists`
  so `vi.mock(assemblyApi)` cannot drop the helpers.
- Nested `DeveloperSectionErrorBoundary` on detail uses the same test id as the
  in-panel load error; only one is visible at a time.

## Build evidence (C1)

- `cd WebUI && ../mvnw.cmd clean install` → **BUILD SUCCESS**
- Surefire: Tests run: 61, Failures: 0
- Vitest (Maven test phase): Tests 2785 passed / 375 files
- Playwright: `npm run test:surface -- --path tests/bugs/bug-3554-developer-slot-detail.spec.js` → 1 passed
  (H2 QA `perc-matrix-cms-h2`, `TEST_CMS_URL=http://127.0.0.1:9993`; console-clean=yes;
  FastForward `PSDbStorageService` import ERRORs pre-exist / not feature-related)
