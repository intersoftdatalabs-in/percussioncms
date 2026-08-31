# Erlang review — #4078 REST UI-02 action menu write

**Branch:** `feat/issue-4078-action-menu-write`  
**Scope:** uncommitted + vs `origin/main`  
**Memory patterns hit:** change-class closure (rest resource + `IActionMenuAdaptor` + Spring stub + sitemanage adaptor + adaptor tests + product-docs); rest `MainTest` needs exact adaptor type on test classpath; Admin/session 403 and lock-no-steal 409 from design WS peers.

## Summary

Admin POST/PUT/DELETE for user action menus over existing `IPSUiDesignWs.createActions` / `loadActions` / `saveActions` / `deleteActions`. Duplicate 409, invalid 400, missing 404, non-Admin 403, system menus 409 without lock steal. Collection POST is create; unimplemented allowed-transitions stub moved to `/find/transitions`. No SPA chrome.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

N/A for filesystem I/O. `isSystemMenuPath` splits Workbench hierarchy strings (logical `/` or `\`), not OS file paths.

## Issues

None (bugs). Companions present: Mockito `ActionMenuResourceTest`, `ActionsTestAdaptor` Spring stub, `ActionMenuAdaptorWriteTest`, `product-docs/8.2/developer/rest.md`. Standalone `rest` and `projects/sitemanage` `mvnw.cmd clean install` BUILD SUCCESS.
