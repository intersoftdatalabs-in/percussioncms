# Erlang review — #3712 Content Type detail non-array list fields

**Branch:** `fix/issue-3712-content-type-detail-arrays`  
**Date:** 2026-08-22  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

Developer Content Type detail crashed the Content Types section when Jackson
returned a truthy non-array for `designGaps`, `allowedWorkflows`,
`allowedTemplates`, `fields`, or `childFieldSets` (`(e || []).map` TypeError).
The change normalizes those fields in `unwrapContentTypeDetail` (and catalog
`unwrapContentTypeList` for empty-collection beans / singleton envelopes) and
again in `ContentTypeDetailPanel` so mocked GET payloads cannot throw.
`ContentTypesPanel` isolates detail with `DeveloperSectionErrorBoundary`
(`developer-ct-detail-error`), matching Slot #3554. Structured `{code,message}`
gaps still render via `formatDesignGap`.

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
- Peer of #3554 Slot `asJacksonArray` / `normalizeSlotDesignGaps`

## Issues

None (hard-gate).

### Notes (not blocking)

- `contentTypesApi` re-exports `normalizeContentType*` for callers; panel imports
  `contentTypeLists` so `vi.mock(contentTypesApi)` cannot drop the helpers.
- Nested `DeveloperSectionErrorBoundary` on detail uses the same test id as the
  in-panel load error; only one is visible at a time (Slot peer).
- Lone legacy `designGaps` strings are wrapped; JAXB wrap-key inner strings still
  fall through to `[]` (same as Slot `asJacksonArray`).

## Build evidence (C1 / C5)

- `cd WebUI && ../mvnw.cmd clean install` → **BUILD SUCCESS**
- Surefire: Tests run: 63, Failures: 0
- Vitest (Maven test phase): Tests 3016 passed / 390 files
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` → **BUILD SUCCESS**
- Playwright: `npm run test:surface -- --path tests/bugs/bug-3712-developer-content-type-detail.spec.js` → 1 passed
  (existing H2 QA `perc-matrix-cms-h2`, `TEST_CMS_URL=http://127.0.0.1:9993`; console-clean=yes;
  server.log-clean=yes for the test window; pre-existing `PSRuntimeExceptionMapper` / GUID
  ERRORs earlier in the log are not Content Type detail)
