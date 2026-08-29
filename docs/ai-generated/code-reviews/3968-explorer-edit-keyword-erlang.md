# Erlang review — #3968 Explorer Edit white-screen (keyword `.trim`)

**Branch:** `fix/issue-3968-explorer-edit-keyword`  
**Scope:** uncommitted WebUI editor + perc-qa-automation + product-docs vs `HEAD` / `origin/main`  
**Date:** 2026-08-28  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (Vitest + Playwright for WebUI screen); behavioral tests for new coerce logic; URL `/` paths (not filesystem)

## Summary

Explorer Edit flashed to a blank page because `KeywordFieldWidget.keywordChoicesForField` called `.trim` on JSON-number catalog `value`/`label` (`(k.value ?? "").trim` skips `??` for `0`/`42`). The widget now coercs catalog entries via `catalogText` (`String(...)` then trim; nested `{value}`/`{label}` objects too). `EditorHost` coercs field values with `fieldValueAsString` so `<select value>` is never a number. Checkout/load failures set the existing `editor-error` (LOAD_FAILED + detail) instead of crashing render. Playwright right-click Edit asserts the editor host stays (form **or** error), not a white page.

## Issues

None that block.

## Cross-platform path checklist

- No filesystem path construction. Playwright/REST helpers use URL paths with `/` (correct).
- Helper unit tests use `path.join(__dirname, …)` for reading the spec (portable).
- No Unix-only roots, no `:`/`;` path lists, no line-ending file assertions.

## Companions

- Vitest: numeric + object catalog `keywordChoicesForField`; numeric `<select>` value; checkout reject → `editor-error`.
- Playwright: `explorer-content-editor.spec.js` right-click Edit (#3968) + helper unit tests (`isKeywordTrimCrash`, `isEditorStayVisible`).
- Product-docs: `product-docs/8.2/admin/content-explorer.md` Edit row (keyword coerce; checkout error vs blank page).
- No Java API shape change (C2 N/A).
