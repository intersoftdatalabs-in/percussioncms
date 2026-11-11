# WP0 Coverage Map — 008 vs #4530-shipped surfaces (Option C)

**Date**: 2026-11-11 · **Branch**: `dce-phase3-action-depth` · **Task**: T004
**Rule**: every FR dispositioned as reference / verify / build-or-rework with evidence. Unchanged shipped surfaces are referenced, not rebuilt.

## Disposition

| FR | Disposition | Evidence / rationale |
|---|---|---|
| FR-001 (offer only permitted transitions) | **verify** | Offer projection shipped: `WebUI/src/main/ts/contentExplorer/workflowMenuActions.ts` (prefix-routed transition actions from `GET .../itemmanagement/workflow/getTransitions/{id}`) + `workflowEligibility.ts` guards (folders / `workflowId == -1` skip server call) — #2732, #3330/#3329. Needs SC-001 sample-matrix proof (≥20 items, ≥5 states, 100% DCE match), not rebuild. |
| FR-002 (never offer disallowed) | **verify** | Same files as FR-001; eligibility gate is the enforcement point. Proof via the same matrix (empty-offer case included). |
| FR-003 (folder props beyond ACL) | **build-or-rework** | `FolderSecurityPanel` + `FolderPropertiesEditor` cover ACL + community/locale/DF/workflow. "Same groups and values as DCE" beyond that is unproven — T006 inventory decides build vs reference per group. |
| FR-004 (item props groups/values) | **build** | `ItemPropertiesPanel` covers display title only (load/save + 400/403/409 handling). Full group parity is new work per `contracts/properties-parity.md`. |
| FR-005 (edits persist + visible on reopen) | **build** | Rides the FR-004 dialog work; round-trip proof is the acceptance (Q3–Q4). |
| FR-006 (creatable types listed) | **verify** | `contentTypeChoices.ts` + `ContentTypePickerDialog` + `createPageInFolder`/`createAssetInFolder` exist. Needs Q5 proof (exact-type-set per folder). |
| FR-007 (templates offered) | **verify** | `TemplatePickerDialog` + `templatePickerSession.ts` exist. Needs Q6 proof. |
| FR-008 (create flow opens) | **verify** | `openInEditor` + create-flow entry exist. Needs Q7 proof. |
| FR-009 (denied messaging, no silent omission) | **verify** | `itemPropertiesErrors.ts` (`formatItemPropertiesError`) + per-action `*Errors.ts` modules exist. Needs proof that denied edits message rather than omit (US2/US4 tests). |
| FR-010 (consistent tree/list/host) | **verify** | Offer computation is shared (`actionEnablement.ts`, `actionDispatch.ts`); T011 confirms post-execute recompute across surfaces. No rework unless T011 probe fails. |
| FR-011 (bulk per-item permissions/results) | **verify** | `multiSelectedIds` + `multi*` modules + `ReducedActions` exist (#2408/PR #2522). Needs bulk acceptance matrix proof per `contracts/bulk-result.md`; partial-failure reporting is the open question. |

## Backend (T005 go/no-go)

**GO — no new adaptor work.** `POST /actions/find/transitions` (`ActionMenuResource.getAllowedTransitions`) → `IActionMenuAdaptor.findAllowedTransitions` → `ActionMenuAdaptor` in sitemanage apibridge (line 249). Same pattern for content-type/template menus (`getAllowedContentTypeMenus`, `getAllowedTemplateMenus`). Probe: existing wiring + server workflow service; US1 Vitest matrix exercises the client projection, Playwright exercises the live path.

## Frontend inventory (T006 deltas feeding WP1–WP3)

- Menus: `ContextMenu.tsx`, `ExplorerMenuBar.tsx`, `menuCatalogLoad.ts`, `actionDispatch.ts`, `actionEnablement.ts` + `workflowMenuActions.ts`/`workflowEligibility.ts` (WP1 delta: post-execute recompute T011).
- Properties: `ItemPropertiesPanel.tsx` (display-title only → WP2 build), `FolderSecurityPanel.tsx` + folder dialogs (WP2 inventory per group).
- New-content: `contentTypeChoices.ts`, `ContentTypePickerDialog.tsx`, `TemplatePickerDialog.tsx`, `CreatePageDialog.tsx`, `CreateAssetDialog.tsx` (WP3 delta: exact-set proof).
- Bulk: `ReducedActions.tsx`, `multi*.ts` (WP4 delta: per-item result reporting).

## Conventions (T007)

- i18n: `EXPLORER_MSG` in `contentExplorer/messages.ts`, catalog entries in `modules/perc-i18n/.../CmsUi.tmx` (new menu strings → TMX keys per T012 convention).
- a11y: axe pattern referenced in task brief (`renderA11yGate`/axe usage on changed menus/dialogs); Playwright specs include axe checks on every changed surface.
