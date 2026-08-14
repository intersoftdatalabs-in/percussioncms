# Erlang review — #3354 Navigation tree keyboard a11y

**Branch:** `fix/issue-3354-nav-tree-keyboard-a11y`  
**Date:** 2026-08-13  
**Reviewer persona:** Erlang (independent of implementer)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (WebUI screen → Vitest + Playwright + product-docs); behavioral tests for new keyboard helper; no non-portable path I/O

## Summary

Slice 5 of parent #3197 / QA #3155 steps 3 and 6. Extracts a pure ARIA-tree key resolver, wires roving tabindex + visible `:focus-visible` on `NavTree`, marks the Secure badge with TMX keys, extends Vitest, adds a route-mocked Playwright keyboard spec so C5 does not depend on sibling #3352 seed, and updates product-docs keyboard steps.

## Scope

Uncommitted work on `fix/issue-3354-nav-tree-keyboard-a11y` vs `origin/main`.

## Cross-platform path checklist

N/A — no filesystem path construction. CMS `//Sites/…` strings are repository paths. Playwright uses URL routes.

## Issues

None (hard-gate).

### Notes (non-blocking)

- Playwright keyboard depth uses a fulfilled site/tree payload (peer: `architecture-nav-tree-empty.spec.js`) so H2 without a seeded NavTree still exercises Tab/arrows/Home/End and the Secure i18n attributes. Live-tree coverage remains in `architecture-a11y-smoke.spec.js` with documented soft-skip when no treeitems exist.
- `outline: none` was replaced with `:focus-visible` so keyboard users get a visible ring (WCAG 2.4.7).
- Tab / Shift+Tab are not in `NAV_TREE_ROVING_KEYS`; `resolveNavTreeKey` returns `none` so the component does not `preventDefault`.

## Tests

- Vitest: `navTreeKeyboard.test.ts` (Tab not owned, arrows/Home/End/expand/collapse/boundary) + `NavTree.test.tsx` (Home/End, ArrowRight into child, Tab not prevented, Secure `data-i18n-key`).
- Playwright: `architecture-nav-keyboard-a11y.spec.js`; Home/End added to `architecture-a11y-smoke.spec.js`.

## Change-class closure

WebUI product screen + few existing TMX keys (no new locale matrix) + perc-qa-automation surface spec + product-docs 8.2 keyboard page.
