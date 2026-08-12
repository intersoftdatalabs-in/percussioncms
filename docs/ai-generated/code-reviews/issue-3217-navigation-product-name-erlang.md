# Erlang review — issue #3217 Navigation product name

**Date:** 2026-08-12  
**Scope:** uncommitted branch `fix/issue-3217-navigation-product-name` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** incomplete change-class closure (Playwright companions for WebUI chrome); dual-ship lockstep (JSP not edited)

## Summary

Operator-visible chrome for the Navigation product surface used **Architecture** in SPA fallbacks, shell title TMX, landing-picker keys, tooltip, and product-docs. Routes, testids, homepage type `Architecture`, and package paths stay as-is. Tests now assert visible **Navigation**. Existing Playwright title matchers updated so C5 will not fail on the old name.

## Issues

None (hard-gate).

### Suggestions (non-blocking)

- Legacy JSP still uses `perc.ui.navMenu.architecture@Architecture`. Production TMX en-us for that tuid is already `Navigation`; left unchanged to keep residual hosts working without a dual-ship JSP edit.

## Cross-platform path checklist

N/A — no filesystem path construction or path assertions in this diff.

## Tests

- Vitest: shell title, top-nav fallback, profile + user landing option labels.
- Playwright: existing architecture-* smokes now expect `/Navigation/i` on `architecture-shell-title`.
