# workflowAdmin + workflowActions audit

Source plan: `docs/ai-generated/tasks/webui-i18n-string-extraction/plan.md` (§Phase 0 Phase 1).

Raw scan (2026-08-01, counted from the regex sweep in `plan.md` §"Screen inventory"):
- `workflowAdmin/**` — 9 hits across 4 files
- `workflowActions/**` — 5 hits across 2 files

No local `messages.ts` / `i18n.ts` exists in either subfolder
(`WebUI/src/main/ts/workflowAdmin/**/messag*.ts` and `i18n.ts` returns no
files; same for `workflowActions/**`). Reuse candidates are limited to the
global `WebUI/src/main/ts/i18n/message.ts` `MSG` map and any existing TMX
tuids under `perc.ui.workflowadmin.*` / `perc.ui.workflowactions.*`.

---

## Scope

Per-file hit counts grouped by sub-area (sourced from
`tmp/webui-i18n-by-area/candidates-workflowAdmin.tsv` and
`tmp/webui-i18n-by-area/candidates-workflowActions.tsv`).

### workflowAdmin (9 hits)

|                               File                                | Hits |                     Type                      |                  Source                  |
|-------------------------------------------------------------------|-----:|-----------------------------------------------|------------------------------------------|
| `WebUI/src/main/ts/workflowAdmin/category/CategoriesSection.tsx`  |    3 | 1 `title=`, 2 text nodes                      | `candidates-workflowAdmin.tsv` lines 1–3 |
| `WebUI/src/main/ts/workflowAdmin/role/RoleEditor.tsx`             |    3 | 3 text nodes (`<label>` / empty-state `<li>`) | lines 4–6                                |
| `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowSiteAssign.tsx` |    1 | 1 text node (`<label>Site</label>`)           | line 7                                   |
| `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowStepList.tsx`   |    2 | 2 `aria-label=` (Move Up / Move Down)         | lines 8–9                                |

### workflowActions (5 hits)

|                             File                             | Hits |                                      Type                                      |                 Source                  |
|--------------------------------------------------------------|-----:|--------------------------------------------------------------------------------|-----------------------------------------|
| `WebUI/src/main/ts/workflowActions/AdhocSearch.tsx`          |    1 | 1 `placeholder=`                                                               | `candidates-workflowActions.tsv` line 1 |
| `WebUI/src/main/ts/workflowActions/WorkflowActionsPanel.tsx` |    4 | 4 text nodes (loading, CURRENT STATE / AVAILABLE ACTIONS headers, empty-state) | lines 2–5                               |

---

## Reusable keys (MSG)

The global `MSG` catalog
(`WebUI/src/main/ts/i18n/message.ts`) groups constants by **area/screen** but
contains **no `WORKFLOW_*` constants** today. A grep for `WORKFLOW_` in
`message.ts` returns zero rows. The closest neighbours are the `HOME_*`,
`PUBLISH_*`, `NAV_*`, and `DASHBOARD_*` blocks — none of which describe
workflow admin / workflow actions chrome.

Therefore **no existing `MSG` constants are reusable** for the 14 strings in
this audit. Every row in §"New keys" inserts a net-new `MSG.WORKFLOWADMIN.*`
or `MSG.WORKFLOWACTIONS.*` constant in Phase 1 and a net-new TMX `<tu>` in
Phase 2.

(No local `messages.ts` / `i18n.ts` exists in either subfolder to draw from
either — see the opening check.)

---

## Reusable keys (TMX)

`Select-String -LiteralPath modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx -Pattern 'tuid="perc\.ui\.(workflowadmin|workflowactions)\.'` returns **0 matches**.

Cross-checked separately:

- `tuid="perc\.ui\.workflowadmin\.` → 0 rows
- `tuid="perc\.ui\.workflowactions\.` → 0 rows

The two prefixes are **entirely absent** from `CmsUi.tmx`. Every proposed
key in §"New keys" is a net-new TMX entry and must pass the Phase 2
pre-flight check (no exact-match `tuid` already present) before insert.

No other TMX file (`SystemResources.tmx`, `DeveloperUi.tmx`) is a candidate
for these strings — workflow admin / workflow actions UI is product chrome
served from the React SPA, so per `perc-i18n/AGENTS.md` the target file is
`CmsUi.tmx`.

---

## New keys

Conventions (per `plan.md` §Phase 1):

- Sub-prefix per file:
  - `perc.ui.workflowadmin.role.<field>@…`
  - `perc.ui.workflowadmin.categories.<field>@…`
  - `perc.ui.workflowadmin.steps.<field>@…`
  - `perc.ui.workflowadmin.workflow.siteassign.<field>@…`
  - `perc.ui.workflowactions.panel.<field>@…`
  - `perc.ui.workflowactions.adhocsearch.<field>@…`
- All key strings live in `MSG` as `MSG.WORKFLOWADMIN.*` /
  `MSG.WORKFLOWACTIONS.*` nested objects (Phase 1), then get TMX `<tu>`
  entries in `CmsUi.tmx` (Phase 2). Each constant carries an
  `// audited in audit-workflow.md` comment per the plan §Phase 1 §2.
- Constants are grouped by screen so the audit row ↔ MSG constant ↔ TMX
  tuid mapping is one-to-one.

### workflowAdmin

|                               file:line                               |            English            |                                  Proposed tuid                                   |                Proposed MSG constant                |                                          Notes                                           |
|-----------------------------------------------------------------------|-------------------------------|----------------------------------------------------------------------------------|-----------------------------------------------------|------------------------------------------------------------------------------------------|
| `WebUI/src/main/ts/workflowAdmin/role/RoleEditor.tsx:163`             | `Description`                 | `perc.ui.workflowadmin.role.description@Description`                             | `MSG.WORKFLOWADMIN.ROLE.DESCRIPTION`                | `<label>` for the role description input.                                                |
| `WebUI/src/main/ts/workflowAdmin/role/RoleEditor.tsx:182`             | `No users assigned`           | `perc.ui.workflowadmin.role.no_users_assigned@No users assigned`                 | `MSG.WORKFLOWADMIN.ROLE.NO_USERS_ASSIGNED`          | Empty-state for the assigned-users list.                                                 |
| `WebUI/src/main/ts/workflowAdmin/role/RoleEditor.tsx:223`             | `No available users`          | `perc.ui.workflowadmin.role.no_available_users@No available users`               | `MSG.WORKFLOWADMIN.ROLE.NO_AVAILABLE_USERS`         | Empty-state for the available-users picker.                                              |
| `WebUI/src/main/ts/workflowAdmin/category/CategoriesSection.tsx:268`  | `System Category (Read-Only)` | `perc.ui.workflowadmin.categories.system_lock_title@System Category (Read-Only)` | `MSG.WORKFLOWADMIN.CATEGORIES.SYSTEM_LOCK_TITLE`    | `title=` attribute on the lock indicator (`🔒`). Screen-reader text — must be localized. |
| `WebUI/src/main/ts/workflowAdmin/category/CategoriesSection.tsx:367`  | `Hierarchy Tree`              | `perc.ui.workflowadmin.categories.hierarchy_tree@Hierarchy Tree`                 | `MSG.WORKFLOWADMIN.CATEGORIES.HIERARCHY_TREE`       | Section header above the categories tree.                                                |
| `WebUI/src/main/ts/workflowAdmin/category/CategoriesSection.tsx:382`  | `No categories available.`    | `perc.ui.workflowadmin.categories.empty@No categories available.`                | `MSG.WORKFLOWADMIN.CATEGORIES.EMPTY`                | Empty-state for the tree section.                                                        |
| `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowSiteAssign.tsx:173` | `Site`                        | `perc.ui.workflowadmin.workflow.siteassign.site_label@Site`                      | `MSG.WORKFLOWADMIN.WORKFLOW.SITE_ASSIGN.SITE_LABEL` | `<label>` for the site picker.                                                           |
| `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowStepList.tsx:142`   | `Move Up`                     | `perc.ui.workflowadmin.steps.move_up@Move Up`                                    | `MSG.WORKFLOWADMIN.STEPS.MOVE_UP`                   | `aria-label=` on the up-arrow button.                                                    |
| `WebUI/src/main/ts/workflowAdmin/workflow/WorkflowStepList.tsx:151`   | `Move Down`                   | `perc.ui.workflowadmin.steps.move_down@Move Down`                                | `MSG.WORKFLOWADMIN.STEPS.MOVE_DOWN`                 | `aria-label=` on the down-arrow button.                                                  |

### workflowActions

|                            file:line                             |           English            |                               Proposed tuid                               |                Proposed MSG constant                |                                                                                         Notes                                                                                         |
|------------------------------------------------------------------|------------------------------|---------------------------------------------------------------------------|-----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `WebUI/src/main/ts/workflowActions/AdhocSearch.tsx:137`          | `Search users to add...`     | `perc.ui.workflowactions.adhocsearch.placeholder@Search users to add...`  | `MSG.WORKFLOWACTIONS.ADHOCSEARCH.PLACEHOLDER`       | `placeholder=` for the adhoc user search input.                                                                                                                                       |
| `WebUI/src/main/ts/workflowActions/WorkflowActionsPanel.tsx:136` | `Loading workflow status...` | `perc.ui.workflowactions.panel.loading@Loading workflow status...`        | `MSG.WORKFLOWACTIONS.PANEL.LOADING`                 | Loading-state placeholder while workflow state is fetched.                                                                                                                            |
| `WebUI/src/main/ts/workflowActions/WorkflowActionsPanel.tsx:156` | `CURRENT STATE`              | `perc.ui.workflowactions.panel.current_state_label@CURRENT STATE`         | `MSG.WORKFLOWACTIONS.PANEL.CURRENT_STATE_LABEL`     | Section header above the current-state readout. All-caps is the visual style; the TMX `<seg>` value preserves the English casing for non-en locales (locales may further capitalize). |
| `WebUI/src/main/ts/workflowActions/WorkflowActionsPanel.tsx:193` | `AVAILABLE ACTIONS`          | `perc.ui.workflowactions.panel.available_actions_label@AVAILABLE ACTIONS` | `MSG.WORKFLOWACTIONS.PANEL.AVAILABLE_ACTIONS_LABEL` | Section header above the available transitions. Same casing note as above.                                                                                                            |
| `WebUI/src/main/ts/workflowActions/WorkflowActionsPanel.tsx:196` | `No transitions available.`  | `perc.ui.workflowactions.panel.no_transitions@No transitions available.`  | `MSG.WORKFLOWACTIONS.PANEL.NO_TRANSITIONS`          | Empty-state below the AVAILABLE ACTIONS header.                                                                                                                                       |

**Total new keys: 14** (9 workflowAdmin + 5 workflowActions).

---

## False positives

None. The 14 hits are all user-visible chrome (JSX text nodes, `<label>` /
`<li>` content, `placeholder=`, `aria-label=`, `title=`). No JSDoc, console
strings, throw messages, HTTP error codes, regexes, file paths, data-testid
values, or enum-class strings appear in the candidate TSV. The `style={…}`
inline literals and `data-testid` props on the same lines are deliberately
out of scope per `plan.md` §Phase 0 §2.

Two adjacent items that **look** like candidates but are already routed
through `message(...)` and are therefore not in this audit:

- `WorkflowStepList.tsx` and `CategoriesSection.tsx` already call
  `message(WF_ADMIN_MSG.SELECTABLE)` (and similar) — those are
  already-localized strings whose `WF_ADMIN_MSG` table is local to the
  workflow admin subtree. That table is **not** a `messages.ts` / `i18n.ts`
  module (it is a per-file `as const` object), so it is not surfaced as a
  reusable catalog and is not audited here. It can be reconciled into the
  Phase 1 `MSG.WORKFLOWADMIN.*` block in PR-E as a follow-up if the team
  wants a single global handle.

