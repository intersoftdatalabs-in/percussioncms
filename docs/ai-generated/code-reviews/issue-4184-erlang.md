# Erlang review — issue 4184

**Branch:** `fix/issue-4184-action-menu-visibility-put`  
**Scope:** stacked on #4183 / PR #4190; visibility/uiContexts persist + GET overlay  
**Date:** 2026-09-02  
**Memory patterns hit:** change-class closure (rest resource + adaptor + tests + Spring stub already present + product-docs); behavioral tests for new persist logic; no new path I/O.

## Summary

Admin `PUT /services/actions/{idOrName}` persists Workbench Visibility (`visibilityContexts`) and mode-uicontexts (`uiContexts`) on user menus and returns them on the PUT DTO. GET catalog detail overlays an unlocked design load so the same fields round-trip. Invalid names/ids are 400. System menus remain 409; non-Admin 403. SPA tabs not touched (#4185). Does not re-implement usage/command (#4183).

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (bugs).

### Notes (non-blocking)

- Visibility replace uses `removeContext` so persisted rows are marked for delete rather than `clear()`.
- `null` arrays leave existing collections; empty arrays clear (same as URL parameters).
- GET overlay failures are debug-logged and do not 409 catalog detail.
- Cross-platform path checklist: N/A (no filesystem path logic in this diff).
- C5 Playwright: N/A (REST/Java only; no WebUI screen).

## Tests

- `rest`: `ActionMenuResourceTest` visibility delegate, invalid 400, non-Admin 403; existing 409/404.
- `sitemanage`: `ActionMenuAdaptorWriteTest` persist + DTO round-trip, invalid visibility/uiContext, omit-null, empty-array clear, GET overlay.
