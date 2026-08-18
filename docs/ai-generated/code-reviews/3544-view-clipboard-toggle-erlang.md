# Erlang review — #3544 View → Clipboard toggle

**Branch:** `fix/issue-3544-view-clipboard-toggle`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Date:** 2026-08-17  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** incomplete change-class closure (WebUI screen + Playwright + product-docs); missing behavioral tests for new/changed logic.

## Summary

Parent QA #2741 failed because **View → Clipboard** (`explorer-toggle-clipboard`) stayed `aria-checked=false` and `explorer-clipboard-panel` never appeared. `ContentExplorerShell` already flipped `showClipboard` on `view-clipboard`; the menu item was gated with `disabledWhen: "noClipboardContext"` so `activateItem` returned without calling `onCommand` whenever multi-select and clipboard were both empty.

This change removes that disable so the View toggle always works (empty clipboard panel is an accepted state). `Content → Add to clipboard` still uses `noSelection`. Companions: Vitest (model, menubar, shell + a11y), Playwright surface (`explorer-menu-bar.spec.js` + `explorer-multiselect.spec.js` aria-checked), product-docs `content-explorer.md`.

## Issues

None that are hard-gate bugs.

### Suggestion (low)

`clipboardItemCount` was removed from `ExplorerMenuBarProps` because it only fed the disable predicate. If a later badge needs the count, reintroduce it as display-only — do not re-gate the View toggle.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Menu model (`view-clipboard` always enabled) | Done |
| Menu bar activate + `aria-checked` | Done |
| Shell `setShowClipboard` + panel mount | Already present; shell test added |
| Vitest (model, menubar, shell a11y) | Done |
| Playwright explorer menu-bar + multiselect | Done |
| Product-docs (`content-explorer.md` View tools) | Done |

## Cross-platform path checklist

N/A — no new filesystem path joins; URL/`data-testid` strings only.

## Tests

- Focused Vitest: ExplorerMenuBar 18, menuBarModel 9, ContentExplorerShell 49 (clipboard toggle included)
- WebUI standalone `cd WebUI && ../mvnw.cmd clean install`: BUILD SUCCESS; Surefire Tests run: 61, Failures: 0; Vitest Tests 2775 passed (374 files)
- perc-qa-automation helper unit: explorer-menu-bar ids include clipboard toggle/panel
- C5 Playwright on H2 QA (`TEST_CMS_URL=http://127.0.0.1:9993`, cell `perc-matrix-cms-h2`, Health=healthy, HTTP 200 after Jetty in-cell restart):
  - `npm run test:surface -- --path tests/explorer-menu-bar.spec.js --path tests/explorer-multiselect.spec.js` — 7 passed (including View → Clipboard empty toggle `#3544`)
  - `npm run test:golden` — 2 passed
  - console-clean=yes (pageerror/console listener on `#3544` spec)
  - server.log-clean=yes for the post-restart Playwright window (no new ERROR/FATAL after `2026-08-18 03:17:50`; remaining ERRORs are pre-existing FastForward import / search-index last-modifier noise from first cell start)
