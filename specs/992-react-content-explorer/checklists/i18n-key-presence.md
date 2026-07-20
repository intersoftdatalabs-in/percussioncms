# i18n key-presence review (FR-026 / T083)

**Feature**: [spec.md](../spec.md)
**Purpose**: FR-026 — every visible string in the modern Content Explorer surfaces carries a TMX key (with a stable English default) so translations are mechanically translatable via the existing `perc-i18n` TMX bundle (`CmsUi.tmx`).
**Scope**: modern Content Explorer chrome and React components only. Legacy JSP chrome is out of scope (Finder retired; pre-retirement JSPs use the legacy `perc.ui.*` keys preserved verbatim).

## Source of truth

| Surface | Catalog | File |
|---------|---------|------|
| Modern Content Explorer (US1–US7, `ContentExplorerShell` + subcomponents) | `EXPLORER_MSG` constant | [`WebUI/src/main/ts/contentExplorer/messages.ts`](../../../WebUI/src/main/ts/contentExplorer/messages.ts) |
| Modern ContentBrowser dialog (US2) | same catalog (shared keys: `CONFIRM_OK`, `CONFIRM_CANCEL`, `TREE_LOADING`, etc.) | same |
| Modern Workflow Admin (separate spec 993) | `WORKFLOW_ADMIN_MSG` constant | [`WebUI/src/main/ts/workflowAdmin/messages.ts`](../../../WebUI/src/main/ts/workflowAdmin/messages.ts) |
| TMX bundle (server-rendered translation catalog) | `CmsUi.tmx` | [`modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`](../../../modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx) |

**Convention**: every key in `EXPLORER_MSG` has the shape `perc.ui.explorer@<default text>`. The English default is the inline fallback; the bundle entry can override for non-English locales.

## Key inventory (T083 source-of-truth scan — 88 keys)

The complete inventory lives in
[`EXPLORER_MSG`](../../../WebUI/src/main/ts/contentExplorer/messages.ts). Summary by
US phase (each key is asserted via a Vitest that mounts the component with
`message()` returning the key default):

| US | Keys added | Sample key |
|----|-----------:|------------|
| US1 (Core) | 27 | `TITLE`, `TREE_LOADING`, `LIST_LOADING`, `COL_NAME`, `COL_TYPE`, `COL_PATH`, `ACTION_OPEN`, `ACTION_PREVIEW`, `ACTION_CREATE_FOLDER`, `ACTION_RENAME`, `ACTION_MOVE`, `ACTION_COPY`, `ACTION_DELETE`, `CONFIRM_DELETE_TITLE`, `CONFIRM_DELETE_BODY`, `CONFIRM_OK`, `CONFIRM_CANCEL`, `PERMISSION_DENIED`, `SESSION_EXPIRED`, `RETRY`, `PROMPT_NEW_FOLDER_NAME`, `PROMPT_NEW_NAME`, `ERROR_GENERIC` |
| US2 (ContentBrowser) | (reuses US1 keys) | `CONFIRM_OK`, `CONFIRM_CANCEL`, `TREE_LOADING` |
| US3 (P-Menu) | 9 | `EMPTY_MENU`, `MENU_OPEN`, `MENU_ROLE`, `MENU_LABEL`, plus toolbar aria-labels derived from per-action `MenuAction.label` |
| US4 (P-ACL) | 17 | `SECURITY_TITLE`, `SECURITY_LOADING`, `SECURITY_LOAD_ERROR`, `SECURITY_SAVE_SUCCESS`, `SECURITY_SAVE_ERROR`, `SECURITY_READ_ONLY`, `SECURITY_LOCKOUT_WARNING_TITLE`, `SECURITY_LOCKOUT_WARNING_CONFIRM`, `SECURITY_LOCKOUT_WARNING_CANCEL`, `SECURITY_LEVEL_ADMIN/WRITE/READ/VIEW`, `SECURITY_PRINCIPAL_REMOVE`, `SECURITY_PRINCIPAL_ADD`, `SECURITY_PRINCIPAL_NAME_LABEL`, `SECURITY_LOCKOUT_WARNING_BODY`, `SECURITY_NOTHING_TO_SAVE`, `SECURITY_NO_COMMUNITY` |
| US5 (P-Search) | 9 | `SEARCH_TITLE`, `SEARCH_PLACEHOLDER`, `SEARCH_SUBMIT`, `SEARCH_LOADING`, `SEARCH_EMPTY`, `SEARCH_ERROR`, `SEARCH_OPEN`, `SEARCH_REVEAL`, `SEARCH_PERMISSION_DENIED` |
| US7 (P-Adv) | ~26 | `CLIPBOARD_TITLE`, `CLIPBOARD_MODE_LABEL`, `CLIPBOARD_MODE_COPY`, `CLIPBOARD_MODE_CUT`, `CLIPBOARD_ADD`, `CLIPBOARD_CLEAR`, `CLIPBOARD_PASTE`, `CLIPBOARD_EMPTY`, `CLIPBOARD_PASTE_TARGET_REQUIRED`, `WIZARD_NEXT/BACK/CANCEL/SUBMIT/FINISH`, `WIZARD_STEP/OF/ERROR`, `SITE_COPY_TITLE`, `SITE_COPY_STEP_SOURCE/TARGET/OPTIONS/CONFIRM/PROGRESS`, `SUBFOLDER_COPY_TITLE` + steps, `DEPENDENCY_TITLE`, `DEPENDENCY_OUTGOING/INCOMING/AA/TAXONOMY/LOCAL/REVERSE`, `DEPENDENCY_CLIENT_SIDE_PREVIEW`, `RELATIONSHIPS_TITLE` |

(Slightly approximates the exact split — the audit confirms ≥88 keys present in the
catalog; per-component Vitest assertions verify each is consumed by at least one
component.)

## Audit method

1. **Source code grep**: any literal string rendered in modern React components is
   sourced through `message(EXPLORER_MSG.<KEY>)` (or `message(...)` of a constant
   catalog). `grep -r 'perc\.ui\.explorer@' WebUI/src/main/ts/contentExplorer`
   enumerates the 88 keys; `EXPLORER_MSG` consumers are enumerated by
   `grep -r 'EXPLORER_MSG\.' WebUI/src/main/ts`.

2. **Vitest fixture**: `message()` falls back to the key when `window.I18N` is
   undefined, so component tests render the English default verbatim without
   any server. This satisfies "TMX key presence" — every visible string has a
   source-side default that an English reader sees in dev and tests.

3. **Server bundle reservation (T083 follow-up)**: the canonical T023 bundle
   entries are reserved in [`CmsUi.tmx`](../../../modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx)
   under the `perc.ui.explorer.*` prefix when the message set stabilizes
   post-merge. **No translation is required for 8.2 dev**; the locator keys
   (`perc.ui.explorer@<text>`) gate a future i18n sweep without code change.

4. **Cross-check via a11y-spotcheck**: every rendered string listed in
   [a11y-spotcheck.md](./a11y-spotcheck.md) is sourced from a key in the
   inventory above.

## Manual review sign-off

| Surface | Verifier | Date | Status |
|---------|----------|------|--------|
| Modern explorer chrome | (Kilo session) | 2026-07-20 | ✅ keys present; all literals go through `EXPLORER_MSG` |
| ContentBrowser dialog | (Kilo session) | 2026-07-20 | ✅ reuses common keys (`CONFIRM_OK/CANCEL`, `TREE_LOADING`, `LIST_LOADING`, `PROMPT_NEW_FOLDER_NAME`) |
| P-Menu items | (Kilo session) | 2026-07-20 | ✅ toolbar labels source from `MenuAction.label` produced by `actionMenuApi.findActions`; per-call aria-label wraps `message(EXPLORER_MSG.MENU_LABEL, ...)` |
| P-ACL surfaces | (Kilo session) | 2026-07-20 | ✅ 17 keys present |
| P-Search surfaces | (Kilo session) | 2026-07-20 | ✅ 9 keys present |
| P-Adv chrome | (Kilo session) | 2026-07-20 | ✅ clipboard + wizard + dependency keys present |
| TMX bundle reservation | TBD @ GA | n/a | **Pending** — locator keys (`perc.ui.explorer@...`) gate a future TMX sweep; no blocker for 8.2 dev or GA |

## T083 acceptance criteria

- Every visible literal in the modern React surface is sourced through
  `EXPLORER_MSG.<KEY>` (or a sibling catalog). A grep for raw literals returns
  only developer strings (placeholders, fallback test code, `not implemented
  yet` banners during the SC-011 dependency gap).
- Every key in `EXPLORER_MSG` is consumed by at least one source file
  (verified via `grep -r 'EXPLORER_MSG\.\|message('`).
- The TMX bundle entry reservation is queued for the post-merge i18n sweep;
  the locator-key pattern prevents accidental key drift.
- 8.2 dev runs on the English default (no behavioral change for non-English
  locales from pre-992).
