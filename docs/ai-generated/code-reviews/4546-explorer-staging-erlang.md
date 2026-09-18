# Erlang review — #4546 Explorer Stage / Remove from Staging

**Reviewer:** Erlang (independent of implementer)  
**Date:** 2026-09-17  
**Branch:** `fix/issue-4546-explorer-stage-item`  
**Base:** `origin/main`

## Summary

Explorer slice 2 of parent #4530 wires **Stage** and **Remove from Staging** for a selected page or asset, following the Publish Now / Take Down (#4533) peer: confirm, `resolvePublishKind` enablement, injected toolbar/context-menu leaves (CX catalogs omit Finder staging), sitemanage GET URLs matching classic `PercItemPublisherService`, and `mapPublishResponse` so HTTP 200 `FORBIDDEN` / `BADCONFIG` / `NOSTAGING_SERVERS` is not success.

## Scope

- Uncommitted work on `fix/issue-4546-explorer-stage-item` vs `origin/main`.
- Files: `WebUI/src/main/ts/contentExplorer/{itemPublish,actionDispatch,actionEnablement,messages}.ts`, `WebUI/src/main/ts/publishing/itemPublishPaths.ts`, matching Vitest, Playwright `explorer-action-dispatch.spec.js`, `product-docs/8.2/admin/content-explorer.md`.
- Prior report: none for this ticket. Memory patterns hit: HTTP 200 application-level publish preflight must not look like success (`mapPublishResponse` / #3451 / #3467 / #4533).
- Cross-platform path review: no filesystem I/O. New strings are REST URL path segments using `/` (correct for URLs). Tests do not assert OS path separators.

## Recommendation

approve

## Gate

- Bugs: none
- Behavioral tests: present (itemPublish, actionDispatch, actionEnablement, ContentExplorerShell, Playwright surface)
- Portable paths: N/A (URL-only)
- **May commit/push: yes**

## Issues

None at bug severity.

### suggestion

- Injected `MenuAction.label` values remain catalog-style English (`Stage`, `Remove from Staging`), matching `EXPLORER_TAKEDOWN_ACTION`. Confirm copy goes through `EXPLORER_MSG` / `message()`. Acceptable for this slice; i18n of injected labels can follow Take Down if product later localizes catalog chrome.

### nit

- `isStageActionName` matches the single key `stage`. That is the Finder / `PSPublishingAction.PUBLISHING_ACTION_STAGE` name. No other Explorer action uses it today.

## Companions

- Playwright: `modules/perc-qa-automation/frontend/tests/explorer-action-dispatch.spec.js` (`@explorer-staging`)
- Product docs: `product-docs/8.2/admin/content-explorer.md` Stage / Remove from Staging rows
- REST/sitemanage façade: existing; no new adaptor
