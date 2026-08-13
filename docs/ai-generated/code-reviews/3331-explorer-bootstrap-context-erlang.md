# Erlang review — #3331 ContentExplorerShell BootstrapContext

**Date:** 2026-08-13  
**Branch:** `fix/issue-3331-explorer-bootstrap-context`  
**Scope:** uncommitted WebUI + perc-qa-automation + product-docs vs `origin/main`  
**Recommendation:** approve  
**Gate:** pass — May commit/push: yes  
**Memory patterns hit:** missing behavioral tests (covered); WebUI Playwright companion (covered); change-class closure for Explorer i18n/a11y (covered)

## Summary

Slice 2 of parent #3329. Explorer remount/bridge used `useSpaBootstrap` → `useContext` when React’s dispatcher or the provider was missing (`Cannot read properties of null (reading 'useContext')`). The change makes context optional, shows an i18n error state on the shell, and always wraps Explorer route + PercModernUI bridge mounts with `BootstrapProvider`.

## Issues

None (no bugs, no missing behavioral tests, no non-portable path I/O).

## Companions

| Kind | Status |
|------|--------|
| Vitest without provider (no throw + error chrome) | Yes |
| Vitest with provider (existing shell suite) | Yes (module suite) |
| ExplorerRoute wrap without outer provider | Yes |
| Playwright folder open + no useContext console | Yes (`bug-3331-…spec.js`) |
| `renderA11yGate` / Playwright a11y | Yes |
| `EXPLORER_MSG` i18n | Yes |
| product-docs | Yes (`admin/content-explorer.md`) |

## Cross-platform path checklist

N/A — no filesystem path construction. Playwright uses `BASE_URL` from env.

## Evidence

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS; Vitest 309 files / 2214 tests; Surefire 51 tests
- Playwright QA `TEST_CMS_URL=http://127.0.0.1:9993`: `bug-3331` 1 passed; `test:golden` 2 passed
