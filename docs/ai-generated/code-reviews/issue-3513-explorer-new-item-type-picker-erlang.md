# Erlang review — issue #3513 Explorer New Item type picker

**Date:** 2026-08-17  
**Branch:** `fix/issue-3513-explorer-new-item-type-picker`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** action-dispatch toast vs picker; Explorer i18n/`data-testid`; Playwright surface filter

## Summary

New Item host (`New` / `Create_New_Item`) no longer returns `ACTION_NEEDS_TYPE` when clicked as a leaf. Dispatch loads allowed types (action children, then `POST /actions/find/types`, then `GET /contenttypes`), auto-selects a single type, or opens a picker peer to the page-template dialog, then uses the existing create + editor path.

## Gate

- **Bugs:** none found. Host cancel does not create. Empty catalog still uses `ACTION_NEEDS_TYPE`. Folder required first.
- **Behavioral tests:** dispatch, choices loader, picker session/dialog (a11y), shell invoke, Playwright surface (picker + create intercept).
- **Cross-platform paths:** no filesystem I/O. CMS URL/path strings use `/`.

## Issues

None blocking.
