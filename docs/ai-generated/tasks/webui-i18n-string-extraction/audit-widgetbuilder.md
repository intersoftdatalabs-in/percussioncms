# widgetbuilder audit

Scope: `WebUI/src/main/ts/widgetbuilder/` and its sub-directory
`WebUI/src/main/ts/widgetbuilder/editor/`. Generated from the Phase 0 regex
sweep (`tmp/webui-i18n-by-area/candidates-widgetbuilder.tsv`, 13 hits) and
manually triaged against the canonical TMX
(`modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx`) and the global `MSG`
catalog (`WebUI/src/main/ts/i18n/message.ts`).

This audit covers the **string-localization gap only** — the hardcoded
`<label>`/`<legend>`/`<th>`/placeholder text in the file. The component
already wires a local `K = {...}` constant map that resolves **every button
label and required-field error** through `message(...)` (`DefinitionEditor.tsx:22-29`,
`DefinitionList.tsx:22-28`), and those TMX entries were added in an earlier
batch (`perc.ui.widgetbuilder.modern@Title`, `@Empty`, `@New`, `@Edit`,
`@Delete`, `@Deploy`, `@Save`, `@Validate`, `@Cancel`, `@Add Field`,
`@Label Required`, `@Prefix Required`, plus `@Confirm Delete`, `@Saved`,
`@Deployed`, `@Valid`, `@Disabled`) — none of those keys are in scope for
this audit. The remaining 13 raw hits have **no** TMX entry yet.

## Scope

|                             File                              | Raw regex hits | Hits in audit |                                                                              Notes                                                                              |
|---------------------------------------------------------------|---------------:|--------------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `WebUI/src/main/ts/widgetbuilder/DefinitionList.tsx`          |              4 |             4 | Table column headers (`<th>`). Editor/local `K` constants already wired through `message(...)`.                                                                 |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx` |              9 |             9 | 7 `<label>` text nodes, 1 `<legend>`, 1 `placeholder=` attribute. Other buttons (Save / Validate / Cancel / Add Field) already localized via the local `K` map. |
| **Total**                                                     |         **13** |        **13** | All real, no false positives (every hit is a `<label>`, `<legend>`, `<th>`, or `placeholder=` user-visible string).                                             |

## Reusable keys (MSG)

**None.** The global `WebUI/src/main/ts/i18n/message.ts` `MSG` table has no
`WIDGETBUILDER_*` constants today (verified by
`Select-String -Pattern 'WIDGETBUILDER' message.ts` → 0 matches). The existing
`perc.ui.widgetbuilder.modern@*` keys are referenced only from the
file-local `K = {...}` literals in `DefinitionEditor.tsx` and
`DefinitionList.tsx`; they are not yet promoted into the global `MSG`
catalog. Phase 1 should add a new `MSG.WIDGETBUILDER` nested group rather
than re-using anything from `MSG`.

## Reusable keys (TMX)

**Empty for the 13 audit hits.** Pre-flight grep
`Select-String -Pattern 'tuid="perc\.ui\.widgetbuilder\.'` against
`CmsUi.tmx` returned 17 matches, all under the `perc.ui.widgetbuilder.modern@`
prefix and all matching the existing file-local `K` constants (Title, Empty,
New, Edit, Delete, Deploy, Save, Validate, Cancel, Add Field, Label Required,
Prefix Required, Confirm Delete, Saved, Deployed, Valid, Disabled). **None**
of the 13 raw-hit English strings (`Label`, `Prefix`, `Version`, `Actions`,
`Author`, `Publisher URL`, `Description`, `Widget HTML`, `Fields`,
`field name`) have a current TMX entry, so every row below is a new `<tu>`.

Optional reuse for the list table header **Actions** (low confidence):
`perc.ui.common.label@*` exists in TMX (Back, Cancel, Submit, Confirm,
Close, Processing, Save, Finish, Wizard, Log Out, Welcome, Help, CM1
Community, Percussion Community, About, Rhythmyx UI, Next, User Name,
Continue, OK, Change Password, Change Password Success, Password Match,
Password Six Characters) but **no `@Actions`** entry — keep
`perc.ui.widgetbuilder.list@Actions` as a new widget-builder-scoped key
to avoid dragging a third area into the widgetbuilder cluster.

## New keys

All 13 rows below are real user-visible strings. Where the same English
appears in two places (editor `<label>` and list `<th>`), the row pairs
point at a **single shared tuid** per the Phase 0 rule. The MSG constant
column uses the proposed `MSG.WIDGETBUILDER.*` nested group (Phase 1);
file-level `K = {...}` constants can be replaced by these `MSG.*` lookups
during the Phase 3 PR-D wire-up.

|                             file:line                             |     english     |                          proposed tuid                          | proposed MSG constant (or `inline message(...)`) |                                                                                                           notes                                                                                                           |
|-------------------------------------------------------------------|-----------------|-----------------------------------------------------------------|--------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `WebUI/src/main/ts/widgetbuilder/DefinitionList.tsx:58`           | `Label`         | `perc.ui.widgetbuilder.@Label`                                  | `MSG.WIDGETBUILDER.LABEL`                        | Table column header — same English as the editor `<label>` at `DefinitionEditor.tsx:119`. **Shared tuid** per the Phase 0 reuse rule (the same English text in two places must map to one key).                           |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx:119` | `Label`         | `perc.ui.widgetbuilder.@Label`                                  | `MSG.WIDGETBUILDER.LABEL`                        | Duplicate of `DefinitionList.tsx:58` — listed for completeness so the audit row count matches the 13 raw hits. `htmlFor="wb-label"` is **not** localized (element id, see Hard rules).                                    |
| `WebUI/src/main/ts/widgetbuilder/DefinitionList.tsx:59`           | `Prefix`        | `perc.ui.widgetbuilder.@Prefix`                                 | `MSG.WIDGETBUILDER.PREFIX`                       | Shared with `DefinitionEditor.tsx:129`.                                                                                                                                                                                   |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx:129` | `Prefix`        | `perc.ui.widgetbuilder.@Prefix`                                 | `MSG.WIDGETBUILDER.PREFIX`                       | Duplicate of `DefinitionList.tsx:59`. `htmlFor="wb-prefix"` is **not** localized.                                                                                                                                         |
| `WebUI/src/main/ts/widgetbuilder/DefinitionList.tsx:60`           | `Version`       | `perc.ui.widgetbuilder.@Version`                                | `MSG.WIDGETBUILDER.VERSION`                      | Shared with `DefinitionEditor.tsx:139`.                                                                                                                                                                                   |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx:139` | `Version`       | `perc.ui.widgetbuilder.@Version`                                | `MSG.WIDGETBUILDER.VERSION`                      | Duplicate of `DefinitionList.tsx:60`. `htmlFor="wb-version"` is **not** localized.                                                                                                                                        |
| `WebUI/src/main/ts/widgetbuilder/DefinitionList.tsx:61`           | `Actions`       | `perc.ui.widgetbuilder.list@Actions`                            | `MSG.WIDGETBUILDER.LIST.ACTIONS`                 | Table column header for the per-row Edit/Deploy/Delete buttons. The button labels themselves already resolve through the file-local `K` map (`perc.ui.widgetbuilder.modern@Edit`, `@Deploy`, `@Delete` — already in TMX). |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx:147` | `Author`        | `perc.ui.widgetbuilder.editor.field.author@Author`              | `MSG.WIDGETBUILDER.EDITOR.FIELD.AUTHOR`          | `<label htmlFor="wb-author">` — `wb-author` is the element id and is **not** localized. The field is editor-only (no list column).                                                                                        |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx:155` | `Publisher URL` | `perc.ui.widgetbuilder.editor.field.publisherUrl@Publisher URL` | `MSG.WIDGETBUILDER.EDITOR.FIELD.PUBLISHER_URL`   | `<label htmlFor="wb-url">`. Two-word title case in `<seg>`; TMX uses `xml:lang="en-us"` text exactly.                                                                                                                     |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx:163` | `Description`   | `perc.ui.widgetbuilder.editor.field.description@Description`    | `MSG.WIDGETBUILDER.EDITOR.FIELD.DESCRIPTION`     | `<label htmlFor="wb-desc">`.                                                                                                                                                                                              |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx:182` | `Widget HTML`   | `perc.ui.widgetbuilder.editor.field.widgetHtml@Widget HTML`     | `MSG.WIDGETBUILDER.EDITOR.FIELD.WIDGET_HTML`     | `<label htmlFor="wb-html">`.                                                                                                                                                                                              |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx:219` | `Fields`        | `perc.ui.widgetbuilder.editor.legend@Fields`                    | `MSG.WIDGETBUILDER.EDITOR.LEGEND`                | `<legend>` inside the fieldset that lists `WidgetField` rows.                                                                                                                                                             |
| `WebUI/src/main/ts/widgetbuilder/editor/DefinitionEditor.tsx:232` | `field name`    | `perc.ui.widgetbuilder.editor.field.name@Field Name`            | `MSG.WIDGETBUILDER.EDITOR.FIELD_NAME`            | `placeholder="field name"` on the new-field text input. The `<seg>` uses Title Case (`Field Name`) so the placeholder reads naturally per locale; the raw JSX is lowercase only because that is the in-source style.      |

### False positives

**None.** Every one of the 13 raw hits is a real user-visible string:

- 7 `<label>` text nodes (`DefinitionEditor.tsx:119, 129, 139, 147, 155, 163, 182`) — visible form labels.
- 4 `<th>` text nodes (`DefinitionList.tsx:58, 59, 60, 61`) — visible table column headers.
- 1 `<legend>` text node (`DefinitionEditor.tsx:219`) — visible fieldset legend.
- 1 `placeholder=` attribute (`DefinitionEditor.tsx:232`) — visible input placeholder.

No comments, JSDoc, `data-testid`, enum values, regex, or machine identifiers are mixed into this batch. The `htmlFor="wb-…"` IDs are explicitly excluded from localization per the task hard rules.

### Notes for PR-D wire-up (Phase 3)

1. Replace the inline text in each `<th>` / `<label>` / `<legend>` with the `MSG.*` constant from the table above; set `placeholder={message(MSG.WIDGETBUILDER.EDITOR.FIELD_NAME)}` on the new-field input.
2. The shared `perc.ui.widgetbuilder.@Label`, `@Prefix`, `@Version` tuids intentionally have **no** sub-prefix (e.g. not `editor.field.label@Label`) because the same English text appears in both `DefinitionList.tsx` and `DefinitionEditor.tsx`; the Phase 0 reuse rule requires one tuid per unique English string. The other field labels are editor-only and use the `editor.field.<fieldname>` sub-prefix as the audit prompt requested.
3. The existing local `K = {...}` maps in `DefinitionEditor.tsx:22-29` and `DefinitionList.tsx:22-28` should be folded into the new global `MSG.WIDGETBUILDER` group during Phase 1 (or kept as thin re-exports), so the next grep for "is every widgetbuilder string resolvable through `MSG`?" returns yes.
4. **Playwright (HARD GATE — WebUI/AGENTS.md):** PR-D must add or extend `modules/perc-qa-automation/frontend/tests/widgetbuilder.spec.js` (or a `tests/widgets/widgetbuilder.spec.js` workflow) to assert the rendered label on each editor field and each list column equals the `MSG` constant after a locale switch. Cover the shared `@Label` / `@Prefix` / `@Version` strings on **both** the editor and the list to lock in the single-tuid-per-English behavior.
5. **Vitest:** add `WebUI/src/test/ts/widgetbuilder/DefinitionEditor.test.tsx` and `DefinitionList.test.tsx` asserting the rendered text equals the `MSG` constant when no `window.I18N` is present (fallback path).
6. **TMX seed (Phase 2):** 9 new `<tu>` entries total (the three shared ones plus the six editor/list-only ones plus the placeholder). All `<tuv xml:lang="en-us">` only — Phase 4 owns non-en backfill via `i18n_translate.py`. Place the new `<tu>` blocks after the existing `perc.ui.widgetbuilder.modern@*` cluster in `CmsUi.tmx` (around line 77935, end of the `widgetbuilder.modern` block) to keep the diff readable.

