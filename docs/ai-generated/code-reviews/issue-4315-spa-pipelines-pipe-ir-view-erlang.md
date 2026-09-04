# Erlang review — issue #4315 SPA Pipelines pipe IR view

## Summary

Developer → Pipelines detail loads read-only `GET /services/pipelines/{idOrName}/ir`
alongside catalog detail and renders resources / backend tanks / mapper mappings.
Vitest covers API URL encoding, stage inventory helper, IR success/empty/404
independence from catalog detail. Product-docs admin page + index/REST cross-links.

## Scope

- Branch: `feat/issue-4315-spa-pipelines-pipe-ir-view` (stacked on #4314 / PR #4318)
- Modules: `WebUI`, `product-docs/8.2`
- Prior memory: SY-01 SPA C5 deferred to Playwright sibling pattern
- Cross-platform path review: N/A (no filesystem path I/O; URL paths use `/`)

## Recommendation

**approve**

## Gate

May commit/push: **yes**

## Issues

None (bug). Playwright H2 live proof is explicitly out of scope for this slice
(sibling #4316); Vitest + WebUI clean install are the agent-safe gates for #4315.
