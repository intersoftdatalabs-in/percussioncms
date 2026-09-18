# Erlang review — #4541 editor required field save errors

## Summary

React Content Editor (`EditorHost`) now validates content-type required flags
before save and check-in, keeps the field form on screen for save failures, and
maps HTTP 400 bodies onto named field rows. Logic lives in a pure helper
(`editorFieldErrors.ts`) with Vitest coverage; Playwright surface spec stubs
itemmanagement + contenttypes on QA H2. Product-docs updated under
`product-docs/8.2/admin/content-explorer.md` and `product-docs/8.2/developer/rest.md`.

## Scope

Uncommitted / branch `fix/issue-4541-editor-required-field-errors` vs `origin/main`.

Memory patterns hit: WebUI Playwright companion, product-docs companion,
behavioral tests for validation (happy + rejection), URL paths using `/`
(CMS URL not filesystem).

Cross-platform path review: Playwright/helpers use logical CMS URL `/` only
(`spa.jsp`, `/services/itemmanagement/...`). No filesystem path joins.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None at bug severity.

### suggestion (fixed in this change set)

- Save-error banner no longer repeats the i18n fallback when `mapped.banner`
  equals `message(SAVE_FAILED)`.

### nit

- File/image required checks treat “no pending file and empty scalar” as empty;
  an already-attached binary with a blank GET fields value can still block
  check-in. Acceptable for this slice (text required + named 400s are the
  product increment).
