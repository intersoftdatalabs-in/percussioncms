# Coverage: React Content Editor (995) — WP0 inventory

**Issue**: #6153 · **Spec**: `specs/995-react-content-editor/` · **Date**: 2026-11-12
**Method**: verify-first — map each FR to in-tree files + tests; build only proven gaps.

## FR-001–009 (shipped host — verify)

| FR | Requirement | In-tree implementation | Vitest | Playwright (H2) | Status |
|----|-------------|------------------------|--------|-----------------|--------|
| 001 | Edit/Quick Edit open host in edit mode; View opens view mode (no checkout) | `WebUI/src/main/ts/editor/EditorHost.tsx`, `editorHostUrl.ts`, `openEditorHost.ts` | `EditorHost.test.tsx`, `editorHostUrl.test.ts`, `openEditorHost.test.ts` | `editor-host-checkout.spec.js` (edit/view, 403 view-only) | covered |
| 002 | Edit mode checkout via `GET …/workflow/checkOut/{id}` | `editorCheckout.ts`, `itemFieldsApi.checkoutEditorItem` (`POST /rest/editor/items/{id}/checkout`) | `editorCheckout.test.ts`, `itemFieldsApi.test.ts` (#6153) | `editor-host-checkout.spec.js` | covered |
| 003 | Scalar load/save `GET`/`PUT …/item/fields/{id}` (`sys_*` except `sys_title` omitted; binary omitted) | `itemFieldsApi.ts`, `editorSave.ts`, `EditorHost.tsx` | `editorSave.test.ts`, `EditorHost.saveReload.test.tsx`, `itemFieldsApi.test.ts` (#6153) | `editor-host-save-fields.spec.js`, `editor-host-text-save.spec.js`, `editor-host-save-reload.spec.js` | covered |
| 004 | Labels from `GET /services/contenttypes/{type}` | `EditorHost` `loadType`, label fallback in row render | `EditorHost.test.tsx`, `EditorHostTranslations.test.tsx` | `editor-host-translations.spec.js` | covered |
| 005 | Save + Check In via checkIn REST | `editorSave.ts`, `itemFieldsApi.checkinEditorItem` | `editorSave.test.ts`, `itemFieldsApi.test.ts` (#6153) | `editor-host-checkout.spec.js`, `editor-host-checkin-comment.spec.js` | covered |
| 006 | New Item create-in-folder (`POST /item/create`) opens editor | `editorCreate.ts`, `itemCreateApi.ts` | `editorCreate.test.ts`, `itemCreateApi.test.ts` | `editor-host-create-item.spec.js` | covered |
| 007 | percPage template flow (one auto / many picker, `templateId` → `IPSPageService`) | `editorPageTemplate.ts`, `pageTemplates.ts` | `editorPageTemplate.test.ts`, `pageTemplates.test.ts` | `editor-page-template.spec.js` | covered |
| 008 | Home/TopNav entries; TopNav Editor = `/editor` | `app/routes/EditorRoute.tsx`, Home Create | `EditorHost.create.test.tsx`, `editorCreate.test.ts` | golden shell specs | covered |
| 009 | Denied/failed saves message, never silent loss | `editorFieldErrors.mapSaveApiErrorToFieldErrors`, `EDITOR_MSG.SAVE_FAILED` | `editorFieldErrors.test.ts`, `messages.test.ts` (#6153) | `editor-host-save-reload.spec.js` (400 PUT → saveError), `editor-host-field-error-summary.spec.js` | covered |

## FR-010–014 (Later widgets — verify, build only gaps)

| FR | Widget | REST path | Vitest | Playwright (H2) | Status |
|----|--------|-----------|--------|-----------------|--------|
| 010 | TipTap rich HTML (`TipTapFieldWidget`), `HtmlFieldWidget` escape hatch | fields API; binary N/A | `widgets/TipTapFieldWidget.test.tsx`, `widgets/HtmlFieldWidget.test.tsx`, `tiptap/*` (5 files) | `editor-rich-controls.spec.js`, `editor-host-html-tiptap*.spec.js`, `editor-host-default-tiptap.spec.js` | covered |
| 011 | File / image upload (`FileFieldWidget`, `ImageFieldWidget`, `editorBinary.ts`, `itemBinaryApi.ts`) | `GET …/item/binary/{id}/{field}`, `PUT` multipart | `widgets/FileFieldWidget.test.tsx`, `widgets/ImageFieldWidget.test.tsx`, `editorBinary.test.ts`, `itemBinaryApi.test.ts` | `editor-host-file-upload.spec.js`, `editor-host-binary-*.spec.js`, `editor-host-file-replace.spec.js` | covered |
| 012 | Keyword (`KeywordFieldWidget`) + community (`CommunityFieldWidget`) | fields API; keyword catalog `GET /services/keywords` | `widgets/KeywordFieldWidget.test.tsx`, `widgets/CommunityFieldWidget.test.tsx` | `editor-keyword-field.spec.js`, `editor-community-field.spec.js`, `editor-host-keyword-*.spec.js`, `editor-host-community-*.spec.js` | covered |
| 013 | Revision promote form (`PromoteForm.tsx`) | revisions API (`editorRevisions.ts`) | `widgets/PromoteForm.test.tsx`, `editorRevisions.test.ts` | `editor-rich-controls.spec.js` (promote), `editor-host-revision*.spec.js` | covered |
| 014 | TMX/fallback strings, axe-clean host | `EDITOR_MSG` (`perc.ui.editor@*`) → TMX | `messages.test.ts` (#6153: prefix/non-blank/no-invisible-chars/UPPER_SNAKE), `EditorHostTranslations.test.tsx`, `tiptap/tiptapI18n.test.tsx` | `editor-host-translations.spec.js`, `editor-host-html-tiptap-i18n.spec.js`; axe via `expectNoSeriousA11yViolations` in specs | covered |

## Gaps found by this battery

- **T005 (closed)**: `itemFieldsApi.ts` (`visibleEditorRevision`, fetch/save/checkout/checkin wrappers),
  `pageSiteImpactApi.ts` (`fetchPageSiteImpact`), and `messages.ts` (FR-014 convention guard) had no
  dedicated Vitest file. Added `itemFieldsApi.test.ts` (9 tests), `pageSiteImpactApi.test.ts` (2 tests),
  `messages.test.ts` (5 tests) — all green. No production change required: every case passed against
  current behavior, including canonical single-slash `/Sites/…` server paths.
- **T010**: no WP1 failures to date (Vitest 946/946 incl. 17 new; Playwright surface below). No production
  gap fixes required.
- **T011**: no new editor strings added by this slice; `perc-i18n` TMX change not required (N/A — test-only
  change, fallback-only convention already covered by `messages.test.ts`).

## WP1 Playwright battery (H2 cell `perc-matrix-cms-h2`)

Run 2026-11-12 (see closeout note for counts):

- T006: `editor-host-checkout.spec.js`, `editor-host-text-save.spec.js`
- T007: `editor-host-create-item.spec.js`, `editor-page-template.spec.js`
- T008: `editor-rich-controls.spec.js`
- T009: `editor-host-save-reload.spec.js`
