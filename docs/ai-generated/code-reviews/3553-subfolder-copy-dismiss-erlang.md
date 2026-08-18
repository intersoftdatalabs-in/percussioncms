# Erlang review — #3553 Subfolder Copy Cancel/item-click dismiss

**Branch:** `fix/issue-3553-subfolder-copy-dismiss`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Reviewer:** Erlang (independent of implementer)  
**Date:** 2026-08-18  

**Memory patterns hit:** missing behavioral tests; incomplete change-class closure (WebUI screen requires Playwright + product-docs); click-away vs menu-toggle races.

## Summary

Human QA on #2797 failed because Subfolder Copy **Cancel** called `resetWizard()` (step 1, overlay stays mounted) and selecting an Explorer item left the overlay up. This change adds `onDismiss` on `SubfolderCopyWizard`, host unmount + Escape/focus restore (`useDialogEscape`), tree/detail/click-away dismiss without POST, Vitest, Playwright, and product-docs 8.2 Explorer Subfolder Copy.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No hard-gate bugs, missing behavioral tests, or non-portable path I/O.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Wizard `onDismiss` vs `resetWizard` | Yes — Cancel with host callback does not reset steps |
| Shell unmount + focus restore | Yes — `useDialogEscape` + Content menu opener |
| Item select / click-away | Yes — `handleSelectFolder` / `handleSelectItem` / document mousedown excluding panel + menu bar |
| Vitest | Wizard dismiss vs reset; shell Cancel/Escape/Back/item/click-away |
| Playwright | `explorer-subfolder-copy.spec.js` Cancel + item-click; console/pageerror + no copy POST |
| product-docs 8.2 Explorer | Subfolder Copy table for Cancel / Escape / item / click-away |

## Cross-platform path checklist

N/A — no filesystem path construction. Playwright/helper URLs use `/` (URL paths).

## Issues

None (hard gate).

### Notes (non-blocking)

- Click-away excludes the Explorer menu bar so Content → Subfolder Copy remains a toggle instead of close-then-reopen.
- Standalone wizard (no `onDismiss`) still `resetWizard` for residual US7 mounts.
- Full multi-folder submit remains soft-skipped on H2 (out of scope).
