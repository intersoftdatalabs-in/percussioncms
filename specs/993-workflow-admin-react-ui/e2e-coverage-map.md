# E2E Coverage Map: 993 Workflow Admin (US1–US8)

**Spec**: `specs/993-workflow-admin-react-ui/` (SC-001 — no manual-only verification for P1 stories)
**Issue**: #6096 — US2–US6 workflow specs were undiscoverable
**Date**: 2026-11-11

## Story → spec file map

| # | User story (spec.md) | Prio | E2E spec (live) | Status |
|---|----------------------|------|-----------------|--------|
| US1 | Manage Workflow Definitions | P1 | `modules/perc-qa-automation/frontend/tests/us1-workflow-definitions.spec.js` | ✅ exists (pre-existing) |
| US2 | Assign Workflows to Sites and Folders | P1 | `modules/perc-qa-automation/frontend/tests/us2-site-folder-assignment.spec.js` — saved-workflow editor → assign dialog → site select → job start → poll to terminal state (#6157); shell + create-form covered by `us1-workflow-definitions.spec.js` | ✅ exists |
| US3 | Manage Roles | P1 | `tests/us2-roles-management.spec.js` | ✅ restored by #6096 |
| US4 | Manage Users | P1 | `tests/us3-users-management.spec.js` | ✅ restored by #6096 |
| US5 | Perform In-Context Item Workflow Transitions | P1 | `tests/us4-item-transitions.spec.js` (React `EditorWorkflowPanel` via editor host) + pre-existing `tests/editor-host-workflow-transitions.spec.js` (trigger mechanics) | ✅ restored by #6096 |
| US6 | Manage Categories | P2 | `tests/us5-categories-admin.spec.js` | ✅ restored by #6096 (required a product fix, note 2) |
| US7 | Manage Scheduled Tasks | P2 | `tests/us6-scheduled-tasks.spec.js` | ✅ exists (pre-existing) |
| US8 | Run System Consistency Check | P3 | `tests/us6-scheduled-tasks.spec.js` (System Tools tab + consistency-checker section) | ✅ exists (pre-existing) |

Run any row surface-filtered in QA mode:

```bash
python docker/scripts/perc-devctl.py qa-up   # then qa-health
TEST_CMS_URL=<qa-url> npm run test:surface -- --path tests/us2-roles-management.spec.js
```

## Note 1 — US2 site-assign dialog (#6157, closed)

`WorkflowSiteAssign` is reachable only from an existing workflow's editor
(`open-site-assign-button` renders for saved workflows). Covered by
`tests/us2-site-folder-assignment.spec.js`: live shell → workflow list →
saved-workflow editor → dialog, then stubbed dialog services (site catalog,
job start, two-tick `isInProgress`) prove select → start → poll → terminal
state without reassigning real folders; a cancel test proves close-without-start.
The spec self-provisions a uniquely named workflow when the list is empty and
deletes it afterwards.

## Note 2 — Categories tab product fix (#6096)

The Categories tab never loaded: `PATHS.CATEGORY_*` pointed at
`/services/category/*`, but the CXF server is registered at
`/categorymanagement` (`sitemanage-beans.xml` → service `@Path("/category")`;
legacy `perc_path_constants.js`: `CATMGT + "/category/all"`). All five
constants now carry the `categorymanagement/` prefix, and the `/undefined`
site suffixes in `CategoriesSection` were dropped in favor of the bare
`/all` and `/update` endpoints. The `us5-categories-admin` spec proves the
tab renders, locks, and opens/cancels the add dialog against live H2.

## Note 3 — US5 host clarification

`WorkflowActionsPanel` (US5 Phase-5 component) is currently unmounted — no
SPA host imports it. The live in-context transition UI is
`EditorWorkflowPanel` inside `EditorHost` (edit mode); `us4-item-transitions`
covers it (state, triggers, comment enforcement, ad-hoc assignees,
view-mode read-only, zero legacy-CE requests). Mounting the standalone
panel (or removing it if superseded) is follow-up work, not E2E scope.

**Resolution (#6158 — `EditorWorkflowPanel` supersedes, panel removed).**
The standalone `WorkflowActionsPanel` + `TransitionDialog` + `AdhocSearch`
trio under `WebUI/src/main/ts/workflowActions/` (with its two unit tests)
was deleted: zero SPA hosts imported it, and `EditorWorkflowPanel` covers
the same live surface (state, triggers, comment, ad-hoc assignees, plus
workflow-change picker and load-error/retry states the standalone panel
lacked; check-in/out locking lives in `EditorHost` via `editorCheckout`).
Live coverage is unchanged — `tests/us4-item-transitions.spec.js` +
`tests/editor-host-workflow-transitions.spec.js` prove the surviving UI.
No user-visible behavior changed, so no product-docs update.

## Note 4 — numeric filename overlap (not a conflict)

`us2`–`us5` prefixes are also used by an unrelated series
(`us2-content-browser`, `us3-menus`, `us4-acl`, `us5-search`). Full filenames
differ, and the `us1-workflow-definitions` / `us1-core-explorer` pair already
co-exists, so the tasked names were restored verbatim per `tasks.md`
T022a/T030/T040/T049 and `plan.md`. The `@workflow-admin` tag and the
`993 USn` describe-block prefix keep the series discoverable.
