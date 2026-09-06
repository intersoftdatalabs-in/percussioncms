# Erlang review — #4326 Developer SPA File Explorer browse

**Date:** 2026-09-05  
**Branch:** `feat/issue-4326-file-explorer-spa` (stacked on #4325 / `feat/issue-4325-file-explorer-browse`)  
**Scope:** uncommitted WebUI File Explorer SPA + product-docs vs REST sibling tip  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** incomplete change-class closure (Playwright companion is sibling #4327, not this PR); URL `/` separators are REST relative paths not OS filesystem joins; behavioral tests required for path-safety helpers

## Summary

Adds Developer **File Explorer** chrome: Admin REST client (`GET /services/fileexplorer` and children), `FileExplorerPanel` browse (roots → directories, breadcrumb, read-only files), shell/allowlist wiring, Vitest, and product-docs. Does not invent a second REST surface or call SY-05 application files. CXF `restFileExplorerResource` serviceBeans already land in stacked #4325.

## Issues

None (hard-gate).

## Cross-platform path checklist

- Client `relativePath` uses REST `/` (URL/query contract), not `File.separator`.
- Root ids are catalog tokens (`[A-Za-z][A-Za-z0-9_-]{0,63}`); unsafe ids/paths rejected before `GET`.
- `encodeURIComponent` on root id and `path` query; no OS path concatenation.
- Tests assert slash-separated REST forms and reject `..`, drive letters, UNC, and backslash.

## Companions

| Kind | Status |
|------|--------|
| REST adaptor/resource | Stacked #4325 / PR #4331 — not duplicated |
| CXF serviceBeans | Already on REST branch (`restFileExplorerResource`) |
| Vitest (API + panel + shell) | Present |
| product-docs `product-docs/8.2/` | Admin + developer REST/index updated |
| Playwright H2 | Out of scope here — sibling #4327 |

## Build

`cd WebUI && ../mvnw clean install` (JDK 21): **BUILD SUCCESS**. Surefire Tests run: 69, Failures: 0. Vitest Test Files 430 passed, Tests 3992 passed.
