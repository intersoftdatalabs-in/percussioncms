# Explorer parity (#2400) — i18n + 508 / accessibility HARD GATE

**Parent:** [#2400](https://github.com/intersoftdatalabs-in/percussioncms/issues/2400)  
**Aligns with:** FR-026 / T083 (i18n), FR-024 / SC-009 / T082 (a11y) from `specs/992-react-content-explorer/`  
**WebUI agent rule:** `WebUI/AGENTS.md` → **Content Explorer — i18n + 508 / accessibility**

## Mandatory for every Explorer UI slice

### i18n

1. Add keys to `WebUI/src/main/ts/contentExplorer/messages.ts` (`EXPLORER_MSG`).
2. Shape: `perc.ui.explorer@<English default text>`.
3. Render only via `message(EXPLORER_MSG.KEY)` (including `aria-label` text).
4. Do **not** invent parallel catalogs for Explorer chrome.
5. CMS design data (display format names, server menu labels, item titles) may display raw — product chrome may not.

### Accessibility (Section 508 / WCAG 2.1 AA target)

1. Keyboard path for every new control (toggle, select, panel, menu).
2. Toggles: `aria-pressed`, `aria-expanded`, `aria-controls` when they show/hide regions.
3. Regions/panels: `aria-label` (or labelled heading) via `EXPLORER_MSG`.
4. Errors: `role="alert"` (and `aria-live` when appropriate); status hints: `role="status"`.
5. Associate form controls with visible labels (`htmlFor` / `aria-labelledby`).

### Tests (non-negotiable)

| Layer | Gate |
|-------|------|
| Vitest | `renderA11yGate(container)` on new/changed surfaces (`contentExplorer/a11y.ts`) |
| Vitest | Assert new chrome keys are `perc.ui.explorer@…` when adding shell chrome |
| Playwright | `expectNoSeriousA11yViolations` on Explorer shell / expanded panels |
| Playwright | Behavioral coverage of user-visible flows (`perc-qa-automation`) |

## References

- `specs/992-react-content-explorer/checklists/a11y-spotcheck.md`
- `specs/992-react-content-explorer/checklists/i18n-key-presence.md`
- `docs/ai-generated/tasks/gh-codeql-alerts/` — unrelated security; do not confuse with a11y

## Sign-off (update per slice PR)

Sample window for the #6109 compliance pass (2026-11-12): the 10 most
recent `main` merges touching `WebUI/src/main/ts/contentExplorer/`
(`git log --oneline -- WebUI/src/main/ts/contentExplorer/`). Each merge
diff was checked for (a) new `EXPLORER_MSG` keys in the
`perc.ui.explorer@…` shape, (b) `renderA11yGate` in new/changed Vitest
files, (c) `expectNoSeriousA11yViolations` in new/changed Playwright
specs. Full passes stay as-is; gaps get a waiver reason here plus a
residual issue (never a silent skip).

| Slice / PR | i18n keys | Vitest a11y | Playwright a11y | Notes |
|------------|-----------|-------------|-----------------|-------|
| #2407 shell composition | DISPLAY_FORMAT_*, SERVER_ACTIONS_*, VIEW_TOOLS_*, TOGGLE_*, PANEL_REGION_*, SECURITY_SELECT_FOLDER | ContentExplorerShell.test.tsx | us1-core-explorer shell + search expanded | Product shell |
| #6079 / PR #6165 CSV export | DETAIL_EXPORT_CSV (`perc.ui.explorer@Export CSV`) | DetailList.csvExport.test.tsx (`renderA11yGate`) | explorer-detail-export-csv.spec.js | Full pass |
| #6110 / PR #6147 DCE phase-3 action depth | ITEM_PROPS_PATH, ITEM_PROPS_GENERAL_GROUP, ITEM_PROPS_DISPLAY_GROUP | GAP — no `renderA11yGate` in itemPropertiesParity / newContentMenuListing / workflowOfferMatrix / workflowBulkResults | explorer-item-properties + explorer-new-content + explorer-workflow-offers specs | Panel `aria-label`s via keys OK; Vitest gap → #6179 |
| #6094 / PR #6131 T087 quickstart evidence | N/A — no new chrome strings (JSP diff is comments + script-src only) | N/A — relationshipsApi.test.ts is API-level | us7-advanced + us8-dependency specs (incl. axe contrast fix) | Waiver: no new user-visible chrome; a11y gate exercised in Playwright |
| #6075 / PR #6120 saved-search rename/delete | SEARCH_SAVED_* (21 keys: rename/delete dialog, errors, notices) | SearchPanel.test.tsx (`renderA11yGate`) | GAP — explorer-saved-search-manage.spec.js has no a11y call | `role="dialog"`/`aria-modal`/`aria-label` via keys, `role="status"` notices OK; Playwright gap → #6180 |
| #5777 / PR #6023 cell-renderer extraction | N/A — pure refactor, no new chrome | N/A — detailCellRenderers.test.ts unit-level | N/A — no surface change | Waiver: behavior-preserving extraction; shell gates still cover |
| #5943 / PR #5971 stage 400 mapping | N/A — no new strings (widens existing 403/404/409 branch to 400) | N/A — actionDispatch.test.ts asserts named-failure routing | N/A — explorer-staging.spec.js behavioral only | Waiver: logic-only, reuses existing keyed messages |
| #5942 / PR #5959 takedown 400 mapping | N/A — same as #5943 | N/A — same as #5943 | N/A — explorer-takedown.spec.js behavioral only | Waiver: logic-only, reuses existing keyed messages |
| #5883 / PR #5921 density toggle | DETAIL_DENSITY_LABEL/COMFORTABLE/COMPACT | DetailList.density.test.tsx (`renderA11yGate` ×2) | explorer-detail-density.spec.js (×2) | Full pass |
| #5882 / PR #5907 tree inline new-folder | TREE_NEW_FOLDER, TREE_INLINE_NEW_FOLDER_LABEL/BLANK | ExplorerTree.inlineFolder.test.tsx (`renderA11yGate` ×2) | explorer-tree-inline-folder.spec.js | Full pass |
| #5881 / PR #5884 column visibility | DETAIL_COLUMNS_MENU, DETAIL_COLUMNS_KEEP_ONE | GAP — DetailList.columns.test.tsx has no `renderA11yGate` | explorer-detail-columns.spec.js (×2) | Menu carries `aria-expanded`/`aria-controls`/`aria-label` via keys; Vitest gap → #6178 |
