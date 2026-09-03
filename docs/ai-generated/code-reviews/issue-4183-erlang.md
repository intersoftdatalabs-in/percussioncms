# Erlang review — issue 4183

**Branch:** `fix/issue-4183-action-menu-usage-command-put`  
**Scope:** uncommitted vs HEAD + not in origin/main  
**Date:** 2026-09-02  
**Memory patterns hit:** change-class closure (rest resource + adaptor + tests + Spring stub already present + product-docs); behavioral tests for new persist logic; no new path I/O.

## Summary

Admin `PUT /services/actions/{idOrName}` now persists Workbench Usage/Command fields (`handler`, URL, URL parameters, command/usage properties) on user menus and returns them on the PUT DTO (GET-round-trip). System menus remain 409; non-Admin 403; missing 404. Visibility contexts are ignored (slice #4184). SPA tabs not touched (#4185).

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (bugs).

### Notes (non-blocking)

- Parameter replace uses `removeParameter` so persisted rows are marked for delete rather than `clear()`.
- `sys_restUserMenu` is not overwritten from the PUT body.
- Cross-platform path checklist: N/A (no filesystem path logic in this diff).
- C5 Playwright: N/A (REST/Java only; no WebUI screen).

## Tests

- `rest`: `ActionMenuResourceTest` usage/command delegate; existing 403/404/409.
- `sitemanage`: `ActionMenuAdaptorWriteTest` persist + DTO round-trip, invalid handler, omit-null parameters, empty-array clear.
- Spring `ActionsTestAdaptor` already implements `saveActionMenu` (no new interface).
