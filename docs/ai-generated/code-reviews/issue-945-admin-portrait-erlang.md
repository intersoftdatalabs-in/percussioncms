# Erlang review — issue #945 Admin portrait layout

**Branch:** `fix/issue-945-admin-portrait-layout`  
**Date:** 2026-08-07  
**Scope:** uncommitted WebUI Admin/Workflow shell chrome + classic adminWorkflow dual-ship + Vitest + Playwright

## Summary

Portrait / narrow Admin chrome was clipped by fixed min-widths and non-wrapping tab rows. Fix is CSS-first: shared `AdminChrome.module.css` for SPA shells; classic `adminWorkflow.jsp` drops `min-width:500px` and gains overflow-y scroll; `percWorkflow.css` dual-ship adds GH-945 media queries.

## Recommendation

**approve**

## Gate

| Check | Result |
|-------|--------|
| Bugs | none found |
| Behavioral tests | Vitest shell + CSS contract; Playwright bug-945 |
| Portable paths | Vitest uses `node:path` `resolve` only; no OS-specific path strings |
| Dual-ship consistency | webapp + war + pages/app JSP; three percWorkflow.css trees |
| Module clean install | `cd WebUI && ../mvnw clean install` SUCCESS |

## Issues

None blocking.

### Nits (non-blocking)

- Classic finder `min-width: 952px` remains product-wide; out of scope for Admin tab chrome (residual if full classic design admin portrait still clipped by finder).
- Playwright not executed against live CMS in this session (no DEV install required for unit gate); spec is landed for CI/dev.

## Cross-platform path checklist

N/A for production I/O. Tests use `path.resolve` / `readFileSync` — portable.

## Memory patterns hit

- Dual-ship WebUI war/webapp consistency
- Playwright companion for WebUI screen change
- CSS contract tests peer (`loginStylesContract`)

## May commit/push

**yes**
