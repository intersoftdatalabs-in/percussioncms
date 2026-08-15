# Erlang review — issue 3438 asset picker SearchPanel mount + execute

**Branch:** `fix/issue-3438-asset-picker-search`  
**Scope:** uncommitted WebUI + `host-asset-picker` Playwright vs `origin/main`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

## Summary

Residual of QA Failed #2799. Three concrete defects on the Asset Picker host:

1. Dual React from `perc-modern-ui.js?cb=` + `Date.now()` vs lazy chunk import of the unqueried URL.
2. Free-text / execute `folderPath` sent as `/Sites` (`getIdByPath` requires `//`).
3. Saved-search POST body was a bare `folderPath` object; CXF UNWRAP_ROOT_VALUE expects `SearchExecuteRequest`.

Implementation matches peers (`wrapViewExecuteRequest`, `toRepositoryCmsPath`). Tests cover path normalize, envelope wrap, JSP module URL, and live Playwright without soft-skip.

## Cross-platform path checklist

CMS repository paths (`/Sites` vs `//Sites`) are URL-style, not OS file I/O. Helper strips optional drive letters / backslashes only so a pasted path still becomes `//Sites/...`. No hardcoded OS separators for filesystem work. Playwright uses `TEST_CMS_URL`. **Pass.**

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Memory patterns hit

- Dual ESM / two React copies from cache-busted module URL vs lazy import
- JAXB WRAP_ROOT_VALUE envelopes (`ViewExecuteRequest` peer)
- Repository `//` prefix for content WS path APIs
