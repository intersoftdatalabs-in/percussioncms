# Erlang review — issue #4304 Pipelines Slice B Playwright H2

**Scope:** `fix/issue-4304-pipelines-slice-b-playwright-h2` vs `origin/main` (stacked tips #4308/#4309 + Playwright surface + `data-pipe-name` + javadoc unblock).

**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** surface-filtered Playwright companions; exact-name catalog selectors; C5 qa-up → qa-health → deploy → surface → qa-down; no host-install-only paths.

## Summary

Adds H2 QA Playwright proof for Developer Pipelines Admin start/stop round-trip. Unique delta beyond stacked REST/SPA tips: surface spec, `data-pipe-name` on catalog open controls, product-docs pointer, `restPipelinesResource` CXF `rest-jax-rs` registration (live H2 returned 404 without it — same class as GH-2142), CatalogRestJaxrsRegistrationTest lock, and a one-line javadoc fix that unblocked `rest` `javadoc:jar`.

## Issues

None blocking.

### Suggestions (non-blocking)

- Spec prefers non-critical / `sys_*` apps and restores lifecycle state; optional CI env `PIPELINE_APP_NAME` if catalog order shifts.

## Cross-platform path checklist

N/A — no new filesystem path construction; Playwright uses URL paths and CSS attribute selectors only.

## Tests / companions

- Playwright: `developer-pipelines-start-stop.spec.js` (console guards, POST wait, Running meta, restore).
- Vitest: PipelinesPanel asserts `data-pipe-name`.
- Product-docs: `developer-pipelines.md` points at the surface spec.
- Change-class: Playwright H2 companion for SPA start/stop (peers: community-roles, server-configs-write).
