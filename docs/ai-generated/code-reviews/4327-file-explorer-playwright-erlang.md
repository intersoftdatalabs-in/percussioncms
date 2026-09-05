# Erlang review — #4327 Playwright File Explorer browse H2

**Date:** 2026-09-05  
**Branch:** `feat/issue-4327-file-explorer-playwright` (stacked on #4325/#4326, PRs #4331/#4332)  
**Scope:** perc-qa-automation File Explorer surface spec + helpers vs SPA tip  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Playwright companion for WebUI screen (change-class closure); URL `/` separators are REST/SPA query contract not OS filesystem joins; behavioral unit tests for unwrap + URL helper; no write/upload

## Summary

Adds surface-filtered H2 Playwright proving Developer **File Explorer** browse: Admin REST `GET /services/fileexplorer` plus SPA roots → children drill (read-only). Helpers unwrap Jackson catalog ids, build `spa.jsp?entry=developer&section=file-explorer`, and filter console noise. Unit tests cover URL shape, unsafe-id skip, and console filter. Distinct from SY-05 application files and SY-02 server configs. No upload/save chrome asserted absent.

## Issues

None (hard-gate).

## Cross-platform path checklist

- SPA URL and REST child `path` use `/` (URL/query contract), not `File.separator`.
- Spec rejects `..` and `\\` on directory relative paths from `data-fe-path`.
- Root ids match catalog token `[A-Za-z][A-Za-z0-9_-]{0,63}`; no OS path in wire assertions.
- Unit tests use `http://127.0.0.1:…` URL forms, not Unix `/tmp` or Windows `C:\`.
- README surface example is documented for both `npm run test:surface` and Windows `perc-devctl` peers already in the module README.

## Companions

| Kind | Status |
|------|--------|
| REST + SPA | Stacked #4325/#4326 (PRs #4331/#4332) — not duplicated |
| Playwright surface spec | `tests/developer-file-explorer-browse.spec.js` |
| Node unit tests | `tests/unit/developer-file-explorer-surface.test.js` + `package.json` `test:unit` |
| product-docs | N/A this slice (operator page already on #4326) |
| C5 live H2 | Required at PR time — `perc-devctl qa-up` / `qa-health` / `test:surface` |

## Build

- `cd modules/perc-qa-automation && ../../mvnw clean install` (JAVA_HOME=/usr/lib/jvm/java-21-openjdk) → **BUILD SUCCESS**. Surefire: No tests to run (Node module).
- `cd modules/perc-qa-automation/frontend && npm run test:unit` → tests 502, fail 0 (includes `developer-file-explorer-surface.test.js`).
- `npm run test:surface:list -- --path tests/developer-file-explorer-browse.spec.js` → 2 tests listed.
