# Erlang review — issue #4303 SPA Pipelines Slice B start/stop chrome

**Branch:** `feat/issue-4303-spa-pipelines-start-stop`  
**Base:** `origin/main`  
**Date:** 2026-09-04  

## Summary

Developer Pipelines detail gains Admin **Start** / **Stop** chrome wired to
`POST /services/pipelines/{idOrName}/start|stop` (REST sibling #4302 / PR #4308).
Vitest covers API helpers and panel behavior; product-docs admin/developer notes
added. Playwright H2 is sibling #4304 (explicit out of scope for this slice).

## Scope

- `WebUI/.../pipelinesApi.ts` — start/stop POST, unwrap, stale lifecycle gap strip
- `WebUI/.../PipelineDetailPanel.tsx` — Admin toolbar, Running meta, busy/403
- `WebUI/.../types.ts` — `active` on `ApplicationSummary`
- `WebUI/.../messages.ts` — lifecycle copy; list hint updated
- Vitest: `pipelinesApi.test.ts`, `PipelineDetailPanel.test.tsx` (+ mock peers)
- `product-docs/8.2/admin/developer-pipelines.md` + index links

Change class: WebUI product screen / Admin lifecycle chrome. Companions from
peers (VirtualSite / Extensions detail): API helpers + panel Vitest + product-docs
admin page + developer index pointer. Dual-ship war N/A (TS via Vite/Maven WAR
packaging; no JSP dual-path edit).

## Findings

No `bug` findings. Cross-platform: no new filesystem path joins (URL encode only).
Non-Admin chrome hidden (not merely disabled). Start gated on enabled / not
hidden / not active; Stop gated on active / not hidden. 403 maps to
`PIPE_FORBIDDEN`. Inflight guard prevents double-submit.

### suggestion

1. After #4302 merges, confirm server `active` is present on GET detail in H2 QA
   before relying on Running meta alone for operators (#4304 Playwright).

## Tests / build

- Vitest: `pipelinesApi.test.ts` (5), `PipelineDetailPanel.test.tsx` (11)
- Standalone: `cd WebUI && ../mvnw.cmd clean install` (recorded in PR body)
