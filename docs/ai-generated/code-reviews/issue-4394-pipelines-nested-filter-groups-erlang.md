# Erlang review — #4394 nested boolean filter groups

**Branch:** `feat/issue-4394-pipelines-nested-filter-groups`  
**Scope:** uncommitted vs `HEAD` plus commits not in `origin/main`  
**Recommendation:** approve  
**Gate:** pass  
**May commit/push:** yes  

## Summary

Vertical increment for Developer Pipelines nested AND/OR selector groups: native IR
`FilterGroupIr`, SQL parenthesized WHERE, HTTP in-memory row filter, REST
`PUT …/filterGroup`, sitemanage persist (classic XML not rewritten), Developer
chrome editor, Playwright surface spec, product-docs 8.2.

## Issues

None (bugs / missing behavioral tests / non-portable paths).

## Notes

- HTTP URL leftover-credential / non-local checks reuse `PSPipelineHttpUrl.requireSafe`
  on the existing tank at persist time; malformed groups fail closed before save.
- Filter evaluation is in-memory for HTTP (portable; no path I/O). IR file store
  unchanged (`Path` APIs already used).
- Cross-platform path checklist: N/A for new I/O (no new filesystem joins).

## Tests

- `PSPipelineFilterGroupTest`, HTTP nested execute, SQL planner parentheses,
  REST resource + adaptor persist/400/403, WebUI vitest, Playwright spec added.

Memory patterns hit: change-class companions (REST DTO + adaptor stub +
sitemanage impl + UI + Playwright + product-docs); fail-closed 400 for
non-local backends.
