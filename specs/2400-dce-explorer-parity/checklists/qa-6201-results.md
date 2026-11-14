# QA results — issue #6201: i18n/a11y sampling docs for Explorer slices (PR #6181)

**Parent:** #6109 (`specs/2400-dce-explorer-parity/`, `checklists/i18n-a11y-hard-gate.md`)
**PR under test:** #6181 — docs(specs): sample i18n/a11y gate compliance across 10 Explorer slices (MERGED)
**Verdict: PASS** — sampling content accurate, links resolve, docs build clean.

## T001 — Sample table present with stated verdicts

`specs/2400-dce-explorer-parity/checklists/i18n-a11y-hard-gate.md` (as merged by #6181)
contains the compliance sample table: the #2407 shell baseline row plus 10 recent
`main` merges touching `WebUI/src/main/ts/contentExplorer/`:

| Slice / PR | Verdict recorded |
|------------|------------------|
| #2407 shell composition | Product shell (baseline) |
| #6079 / PR #6165 CSV export | Full pass |
| #6110 / PR #6147 DCE phase-3 action depth | Vitest gap → #6179 |
| #6094 / PR #6131 T087 quickstart evidence | Waiver (no new chrome) |
| #6075 / PR #6120 saved-search rename/delete | Playwright gap → #6180 |
| #5777 / PR #6023 cell-renderer extraction | Waiver (pure refactor) |
| #5943 / PR #5971 stage 400 mapping | Waiver (logic-only) |
| #5942 / PR #5959 takedown 400 mapping | Waiver (logic-only) |
| #5883 / PR #5921 density toggle | Full pass |
| #5882 / PR #5907 tree inline new-folder | Full pass |
| #5881 / PR #5884 column visibility | Vitest gap → #6178 |

Every row states an i18n-keys verdict, a Vitest-a11y verdict, a Playwright-a11y
verdict, and a notes cell (full pass / waiver reason / residual issue link).
No row is a silent skip: waivers cite a reason, gaps cite a residual issue.

## T002 — Spot-checked slice paths exist on main

Spot-checks against this tip (`origin/main`), all resolve:

1. `WebUI/src/main/ts/contentExplorer/messages.ts` — keys present with the
   `perc.ui.explorer@…` shape: `DETAIL_EXPORT_CSV` ("Export CSV"),
   `DETAIL_DENSITY_LABEL` ("Row density"), `TREE_NEW_FOLDER` ("New folder"),
   `DETAIL_COLUMNS_MENU` ("Column visibility").
2. Vitest files (live under `WebUI/src/test/ts/contentExplorer/`):
   `DetailList.csvExport.test.tsx`, `DetailList.density.test.tsx`,
   `ExplorerTree.inlineFolder.test.tsx`, `DetailList.columns.test.tsx` —
   all contain `renderA11yGate` (the #5881 gap noted in the sample has since
   been closed by #6178 / PR #6234, MERGED).
3. Playwright specs (all contain `expectNoSeriousA11yViolations`):
   `modules/perc-qa-automation/frontend/tests/explorer-detail-density.spec.js` (×3),
   `explorer-tree-inline-folder.spec.js` (×2),
   `explorer-detail-columns.spec.js` (×3).
4. Peer checklists resolve: `specs/992-react-content-explorer/checklists/a11y-spotcheck.md`,
   `specs/992-react-content-explorer/checklists/i18n-key-presence.md`.
5. Residual follow-ups recorded in the sample are all CLOSED with MERGED PRs:
   #6178 → PR #6234, #6179 → PR #6270, #6180 → PR #6300.

## T003 — Docs build clean

`bash scripts/ci-smoke-product-docs.sh` on this tip:

- `Built 54 page(s)`, `ci-smoke-product-docs: OK` (`8.2/index.html` emitted).
- #6181 touched no `product-docs/` pages (specs + Erlang report only), so no
  new include risk; smoke confirms no regression.

## Out of scope (per #6201)

- Fixing sampled i18n/a11y gaps themselves; editor behavior.
- Observation only: the three sample-table gaps (#6178/#6179/#6180) are now
  closed upstream, so a future sampling refresh could upgrade those rows to
  full-pass. Not changed here — this slice records QA evidence only.
