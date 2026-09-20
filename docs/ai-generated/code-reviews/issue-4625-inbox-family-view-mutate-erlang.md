# Erlang review — issue #4625 Inbox-family view mutate

**Date:** 2026-09-20  
**Branch:** `fix/issue-4625-inbox-family-view-mutate`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path review:** URL matching uses `/` only for classic application/URL paths (`sys_cxViews/...`), not filesystem joins. No OS path construction.

## Scope

REST `IViewAdaptor` / `ViewResource` docs, `ViewAdaptor` packaged-URL 409, WebUI catalog badge + SPA guard, Playwright `developer-view-inbox-family.spec.js`, product-docs 8.2.

## Issues

None (bug). Behavioral tests added for create/save packaged URL 409, SPA disabled save, catalog Protected badge.

## Note

Independent Erlang sub-agent spawn is not available in this Grok Build session; this pre-commit report follows the skill checklist. Host peer Erlang should still COMMENT (not self-APPROVE).
