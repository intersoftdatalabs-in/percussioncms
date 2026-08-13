# Erlang review — #3332 editor icon 404s / stuck loading chrome

**Branch:** `fix/issue-3332-editor-icon-404s`  
**Scope:** uncommitted WebUI + perc-qa-automation vs `HEAD` / `origin/main`  
**Date:** 2026-08-13  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (Vitest + Playwright for WebUI chrome); URL `/` paths are valid (not filesystem); dual-tree JSP lockstep; behavioral tests for remapper reject/accept

## Summary

Editor toolbar JSPs requested `/cm/pages/app/images/icons/editor/{delete,edit}.png`, which 404 because assets live at `/cm/images/icons/editor/`. The SPA fallback filter now remaps `/cm/{pages/}app/images/**` (safe image extensions only) onto `/cm/images/**`. Toolbar JSPs request the canonical URLs and hide on `onerror`. PercViewReadyManager clears a stuck processing overlay after 10s. Explorer route loading chrome has a testid so tests can assert it is not left up.

## Issues

None that block.

## Cross-platform path checklist

- Remap paths are **URL** paths (`/cm/images/...`) — `/` is correct.
- Vitest uses `path.resolve` + `existsSync`; line endings normalized (`\r\n` → `\n`).
- No filesystem separator concatenation. No Unix-only roots.

## Companions

- Java remapper + JUnit accept/reject (including traversal / non-image).
- Dual-tree `content_toolbar.jsp` (`cm/app` + `cm/pages/app`).
- Dual-tree `PercViewReadyManager.js` (`app/js/legacy` + `cm/plugins`).
- Vitest static-resource contract + ExplorerRoute loading chrome.
- Playwright surface spec `bug-3332-editor-icons.spec.js`.
- Product-docs N/A (no new operator chrome; existing icons now load).
