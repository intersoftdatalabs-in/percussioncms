# Erlang review — #4344 Object Sorter auxiliary organization

**Change class:** WebUI product screen (Developer supporting navigator) + Playwright + product-docs

**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** session-only when no REST peer; H2 Playwright surface + Vitest companions; no layer-split REST vs SPA vs Playwright; indexed catalog row testids

## Summary

Workbench §12.3 Object Sorter as a Developer tab that sorts the current Content Types catalog. Preference is `sessionStorage` only (documented; no Preference REST peer). Content Types catalog reads the same session preference. Playwright + Vitest + product-docs in the same change.

## Issues

none

## Cross-platform path checklist

- No filesystem path concatenation in new code
- Playwright helper builds URL query with `URLSearchParams` (not OS path join)
- Unit tests assert SPA URL has no `..` / `C:`
- sessionStorage key is a logical string, not a path

## Tests

- Vitest: `objectSorter.test.ts`, `ObjectSorterPanel.test.tsx`, Content Types session order, DeveloperShell tab, parseEntryQuery aliases
- Playwright: `developer-object-sorter.spec.js` (sort + spa.jsp re-entry session stick; do not `page.reload()` client paths — 404 without SPA fallback)
- perc-qa-automation unit helper + smoke-set entry
