# Erlang review: #3575 H2 Sites/Assets tree + detail-list

**Branch:** `fix/issue-3575-h2-sites-assets-tree-list`  
**Base:** `origin/main`  
**Scope:** uncommitted perc-qa-automation proof surface + skip-gate harden  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** skip-with-BUG / no silent soft-skip when fixture exists; portable URL join (no OS separators)

## Summary

Test-only operator proof for parent #3102 symptom 2. No WebUI product change. Helpers unwrap Jackson `PathItem` so H2 sample Sites are not treated as empty. Tree+list must not soft-skip on `TEST_DB_TYPE=h2` (demo-sites default) or when REST listed children.

## Issues

None (no bugs, missing behavioral tests, or non-portable path I/O).

## Cross-platform path checklist

N/A for filesystem I/O. New helpers join CMS URLs with `/` only (URL paths). Unit tests assert no `folder//`.

## Tests

- Node unit: `demo-sites.test.js`, `explorer-sites-assets-tree-list.test.js` (full `npm run test:unit` 318 pass)
- Playwright H2: `explorer-sites-assets-tree-list.spec.js` 4 passed; Sites list/tree peers in `explorer-sites-list-create` 4 passed (Create Site wizard 2 failed, out of scope)
- Golden smoke 2 passed

## Notes

- Do not claim gap-matrix Present.
- qa-health `server_log_errors` on this cell are FastForward `PSDbStorageService` import + search-index ISO date (install/index noise, not tree/list). Docker Health=healthy.
