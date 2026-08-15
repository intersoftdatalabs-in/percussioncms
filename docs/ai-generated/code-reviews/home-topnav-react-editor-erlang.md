# Erlang review — feat/home-topnav-react-editor

**Date**: 2026-08-15  
**Scope**: uncommitted vs `origin/main` (`dc8f1a5100` #3455)  
**Reviewer**: Erlang  
**Memory patterns hit**: change-class completeness (WebUI + Playwright + product-docs); behavioral tests for open-by-id / open-by-path / no leftover `?view=editor`

## Summary

Home Recent/Bookmarks/Search/Library and Home → Create (page/blog) open the React Content Editor host (`spa.jsp?entry=editor`). TopNav **Editor** is an SPA `/editor` route. Leftover `?view=editor` is no longer requested from those shells.

## Recommendation

`approve`

## Gate

May commit/push: **yes**

## Cross-platform path review

CMS folder paths stay `/` repository URLs. No OS filesystem I/O. Clean.

## Change-class closure

| Companion | Status |
|-----------|--------|
| WebUI Home + TopNav + create wizards | yes |
| Vitest (openEditorHost, Home, wizard, TopNav) | yes |
| Playwright (top-nav + home-react-editor) | yes |
| product-docs 8.2 admin + getting-started | yes |

Home **Create asset** still uses leftover `editAsset.jsp` (out of this slice; needs asset create on the React host). Residual `index.jsp?view=editor` is the leftover CM1 document, not the SPA shells.

## Issues

None blocking.

## Tests run

- Focused Vitest: 54 passed
- `cd WebUI && ../mvnw clean install` — BUILD SUCCESS; Vitest 2554 passed

## Re-review (PR #3462 kilo threads)

**Date**: 2026-08-15  
**Scope**: review-fix pack

Kilo WARNING: discarded `window.open` result / always-true return; Home test rewrote `location.href` without restore; missing blocked-popup test.

### Recommendation

`approve`

### Gate

May commit/push: **yes**

### Issues

None remaining. `openEditorHost` now returns `opened != null`. Home test no longer touches `location.href`. Blocked-popup unit test added.
