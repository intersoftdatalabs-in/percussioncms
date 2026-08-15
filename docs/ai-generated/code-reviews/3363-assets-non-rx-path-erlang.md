# Erlang review — #3363 Assets stay on pathmanagement

**Date:** 2026-08-15  
**Branch:** `fix/issue-3363-assets-non-rx-path`  
**Scope:** uncommitted vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (Vitest + Playwright + product-docs); CMS paths use `/` (not OS separators).

## Summary

Content Explorer dual-run treated `/Folders/$System$/Assets` as RX because `isRxCapableFolderPath` used a `/folders/` prefix. Explorer selection prefers `PathItem.folderPath`, so Assets create/rename/move hit `/content-explorer/folders` and failed JAXB (`unexpected element name`). The fix excludes the Assets (and Recycling) library in finder and `$System$` repository forms. Sibling folders under `$System$` remain RX-capable.

## Issues

None (no bugs, no missing behavioral tests, no non-portable filesystem I/O).

## Cross-platform path checklist

- [x] No OS filesystem path construction; CMS paths always use `/`
- [x] Existing drive-letter / backslash normalize kept
- [x] Tests assert CMS path strings, not host OS paths
- N/A: temp files, scripts, line endings

## Companions

| Kind | Evidence |
|------|----------|
| Production | `WebUI/src/main/ts/api/contentExplorer/rxFolderMutationsFlag.ts` |
| Vitest | `$System$/Assets` finder + repo; flag-on stays on pathmanagement |
| Playwright | `tests/bugs/bug-3363-assets-non-rx-path.spec.js` |
| Product-docs | `product-docs/8.2/admin/content-explorer.md`, `developer/rest.md` |

## Build

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS; Surefire 59/0; Vitest 2440 passed
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS
