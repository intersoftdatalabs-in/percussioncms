# Erlang review — issue #4559 Explorer item publishing history

**Date:** 2026-09-18  
**Branch:** `fix/issue-4559-explorer-item-publishing-history`  
**Base:** `origin/main`

## Summary

Explorer injects a **Publishing History** toolbar/context action for a selected page or asset and opens a dialog that reuses PublishingShell `ItemPublishingHistoryPanel` (`GET /services/itemmanagement/item/pubhistory/{id}`). HTTP 404/403 surface as panel errors, not empty success. Playwright + product-docs companions are in the same change set.

## Scope

- `WebUI/src/main/ts/contentExplorer/**` (enablement, dispatch, shell, dialog)
- Reuse of `WebUI/src/main/ts/publishing/components/ItemPublishingHistoryPanel.tsx` and `itemHistoryApi`
- Vitest: enablement, dispatch, dialog, shell, panel 404/403, API rejects
- Playwright: `modules/perc-qa-automation/frontend/tests/explorer-action-dispatch.spec.js`
- Product docs: `product-docs/8.2/admin/content-explorer.md`, `product-docs/8.2/admin/publishing.md`
- Cross-platform path review: no filesystem path I/O in this diff (REST URL `/` only — allowed)
- Memory patterns hit: change-class closure (WebUI + Playwright + product-docs); behavioral tests for 404/403

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None at bug severity. Injected toolbar labels stay English catalog data (same as Schedule / Take Down). Dialog reuses the existing publishing panel strings for table chrome.
