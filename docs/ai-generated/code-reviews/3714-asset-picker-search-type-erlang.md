# Erlang review — #3714 Asset Picker search type mapping

- **Branch:** `fix/issue-3714-asset-picker-search-type`
- **Scope:** uncommitted vs `HEAD` / `origin/main`
- **Reviewer:** Erlang (pre-commit)
- **Date:** 2026-08-21
- **Memory patterns hit:** behavioral tests; WebUI Playwright companion; change-class closure; no path I/O

## Summary

ContentBrowser `passesFilters` required an exact match between host `allowedTypes` (`page`, `asset`) and the item `type`/`category`. CMS search returns content-type names (`Image`, `percPage`, `rffImage`), so Open on a valid hit showed “Selected item type is not allowed” and Confirm stayed disabled.

The mapping is extracted to `passesFilters.ts`, reuses Explorer `isFolder` / `isPageOrAssetContentType` / `isAssetContentType`, and adds stock aliases (`Image`/`File`/FastForward names). Folders and nav types still fail page/asset hosts. Search rows expose `data-item-type` for Playwright. Product-docs note the picker search behavior.

## Recommendation

`approve`

## Gate

May commit/push: **yes**

## Issues

None blocking.

### Tests

- `passesFilters.test.ts` — Image/File/rffImage → asset; percPage → page; `[page, asset]` accepts Image; folders/nav rejected; Image rejected for page-only.
- `ContentBrowser.test.tsx` — search Open of Image with `allowedTypes: [page, asset]` enables Confirm and fires `onConfirm`.
- Playwright `host-asset-picker.spec.js` — live Open + Confirm; skips folderish rows; prefers Image/file/asset hits; console/pageerror empty.

### Change-class closure

WebUI screen bug + Vitest + Playwright + `product-docs/8.2/admin/content-explorer.md` (Content Browser pickers). No Java API shape change.

### Cross-platform path checklist

N/A — no filesystem path I/O. CMS folder paths remain `/`-separated logical paths.

## Re-review

n/a (first pass; Playwright folder-row skip applied before this report).
