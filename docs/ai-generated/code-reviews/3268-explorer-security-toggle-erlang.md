# Erlang review — #3268 Explorer security toggle / FolderSecurity host

**Branch:** `fix/issue-3268-explorer-security-toggle`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Date:** 2026-08-12  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** incomplete change-class closure (WebUI screen + Playwright + product-docs); multi-copy WebUI lockstep (JSP `cm/app` + `cm/pages/app`); behavioral unit tests for new host/chrome.

## Summary

Cycle-verify residual: product Explorer (`spa.jsp?entry=explorer`) did not expose visible `data-testid=explorer-toggle-security` (it lived only in a closed View dropdown), so `us4-acl.spec.js` timed out. Folder-id mount on `folderSecurityModern.jsp` did not paint `folder-security-*` until the async PercModernUI chunk resolved.

The change adds always-visible Security chrome on `explorer-view-tools` (same residual pattern as `#2733` refresh), remaps the View menu item to `explorer-menu-view-security` to avoid duplicate Playwright strict locators, and first-paints `folder-security-loading` / no-folder on the residual JSP host plus a `FolderSecurityHost` mount wrapper with a render error boundary.

`#3253` session-identity resolution is not absorbed (host still takes `currentUserIdentities` from the caller / JSP Admin list).

## Issues

None that are hard-gate bugs.

### Suggestion (low)

JSP first-paint + React remount is slightly redundant once `FolderSecurityHost` loads. Keep both: first paint is what makes `#2749` pass if the modern chunk is slow or fails.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Explorer chrome (`ContentExplorerShell` toolbar) | Done |
| Menu testid remapping (`ExplorerMenuBar`) | Done |
| Residual JSP host ×2 (`cm/app` + `cm/pages/app`) | Done |
| `FolderSecurityHost` + registry | Done |
| Vitest (shell, menubar, host) | Done |
| Playwright `us4-acl.spec.js` locators | Unchanged (`explorer-toggle-security` stable) |
| Product-docs (`content-explorer.md`, `users-roles.md`) | Done |

## Cross-platform path checklist

N/A — no new filesystem path joins; URL/`data-testid` strings and JSP query params only.

## Tests

- WebUI standalone `mvnw clean install`: BUILD SUCCESS, Vitest 2118 passed
- perc-qa-automation standalone `mvnw clean install`: BUILD SUCCESS (npm ci; no Java tests)
- C5 Playwright recorded in the PR after `qa-up` + hot-copy
