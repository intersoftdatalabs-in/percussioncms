# Erlang review — #4560 Explorer compare two revisions

**Scope:** uncommitted / branch `fix/issue-4560-explorer-revision-compare` vs `origin/main`  
**Change class:** Explorer product screen + smallest itemmanagement compare façade  
**Memory patterns hit:** incomplete change-class closure (Playwright + product-docs + behavioral tests); happy-path-only coverage for 403/404; JAXB unwrap; Data Flow URL still unavailable  

## Summary

Operators compare two revisions from Content Explorer `RevisionsPanel` via
`GET /services/itemmanagement/item/compare/{id}/{rev1}/{rev2}`. DCE
`sys_Compare/compare.html` stays classified as Data Flow (unavailable). Missing
revision is HTTP 404; no assignment is HTTP 403. Field diffs reuse
`PSItemEditorFieldsMapper` scalar mapping.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (bugs).

### Notes (non-blocking)

- Compare lives on existing itemmanagement (same surface as revisions/restore),
  not public `rest` JAX-RS. That matches Explorer PATHS and avoids
  CatalogRestJaxrsRegistrationTest thrash.
- Cross-platform path checklist: no filesystem I/O in this slice (URL paths use `/`).
