# Erlang review — #3810 Content Types first-row Object ACL detail

**Branch:** `fix/issue-3810-content-types-acl-detail`  
**Date:** 2026-08-27  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

Parent #3706 residual: catalog Jackson unwrap is on `main`, but first-row **Open**
did not mount `[data-testid=developer-ct-detail]` / Object ACL. Causes addressed:

1. **selectionKey** fell through to display `"—"` or missed JAXB-wrapped `name`,
   so Open was missing or GET used a dummy key.
2. Nested `<tr role="button">` + inner Open button (unlike Templates/Slots).
3. `ObjectAclSection` only rendered after GET detail succeeded, so
   `developer-ct-acl-section` was absent while loading / on GET failure.

Fix: coerce name/label/description in unwrap; `contentTypeSelectionKey` (name then
guid, never `"—"`); Open is button-only with `data-testid=developer-ct-open`;
Object ACL mounts from `catalogGuid` immediately. Vitest covers helpers + Open
navigation; Playwright clicks `developer-ct-open` and asserts detail + ACL.
Product-docs step 3 documents Open + ACL on open.

Memory patterns hit: Playwright companion for WebUI screen; product-docs for
operator path; Jackson string wraps as React children / Open keys; nested
interactive catalog rows.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

Not applicable — no filesystem path/file I/O in the diff (JSON unwrap + React
catalog/detail + Playwright selectors + Markdown).

## Issues

None blocking.

- Vitest: `contentTypesApi` (58), `ContentTypesPanel` (13), `ContentTypeDetailPanel`
  (53), `DeveloperShell` (38) — 162 passed focused run.
- Change-class companions: WebUI SPA + Vitest + Playwright + product-docs.
