# WP0 Baseline Freeze — 009 Content Explorer Increments

**Frozen**: 2026-11-11 (session inventory; live PR/issue states re-checked same day)
**Branch**: `4530-content-explorer-increments`
**Source**: Epic #4530 body table (slices 43–152) + live `gh` state checks

## T004 — Goal-trio state: DONE ON MAIN

- Takedown (#5990), stage/remove-from-staging (#5991), schedule set/clear (#5992): all **closed as already-on-main** (no second PR), re-verified 2026-11-11.
- 400-naming trio on the same flows (#5942/#5943/#5944): PRs #5959/#5971/#5980 all **MERGED** (live-checked).
- Product-docs reference (`product-docs/8.2/admin/content-explorer.md`): Take Down, Stage, Schedule, Clear-scheduled-dates rows present with per-item error semantics — matches merged slice behavior, no drift.
- Multi-select variants done on main: takedown (#4820), stage (#4801 family), schedule (#4995/#4987), with Playwright multi specs.

## T005 — QA ticket list (strict-plus-named rule): FROZEN

| Ticket | State (live) | Scope basis |
|--------|--------------|-------------|
| #4553 stage/remove-from-staging verification | OPEN (To Be Tested) | Named in spec |
| #5799 tree expand-all/collapse-all | OPEN | Named in spec |
| Spawned-by-merge QA | None identified at freeze | Rule: enters scope on spawn |
| Maintainer-named additions | None at freeze | Rule: enters scope on naming |

Cross-epic QA (e.g. publishing #5405/#5800, editor #5986/#5801/#5802) stays with owning epics — explicitly out of this count.

## T006 — `pr_opened` row inventory: ZERO ORPHAN PRs

Method: extracted all 102 unique PR numbers referenced in the #4530 body table and live-checked each state on 2026-11-11.

**Result: all 102 are MERGED.** No open PRs in the body-table trail; the abandoned-PR → new-slice-issue path has zero triggers at freeze.

Merged set (live-verified): 4820, 4848, 4897, 4901, 4907, 4922, 4923, 4924, 4935, 4942, 4952, 4955, 4965, 4975, 4980, 4982, 4995, 5000, 5004, 5020, 5027, 5035, 5046, 5052, 5054, 5065, 5068, 5073, 5085, 5094, 5099, 5114, 5118, 5125, 5143, 5146, 5149, 5165, 5169, 5173, 5188, 5191, 5194, 5209, 5213, 5216, 5229, 5230, 5236, 5255, 5258, 5262, 5274, 5289, 5292, 5307, 5314, 5321, 5340, 5342, 5351, 5371, 5373, 5376, 5392, 5394, 5398, 5420, 5427, 5429, 5446, 5456, 5475, 5487, 5505, 5509, 5541, 5545, 5562, 5564, 5568, 5590, 5593, 5596, 5604, 5635, 5639, 5642, 5715, 5762, 5781, 5812, 5843, 5853, 5870, 5884, 5907, 5921, 5959, 5971, 5980, 6120.

"Done" rows without a PR number (133–135 folder/create/search, 136/138/139 column/tree features, 148–151 trio dedupe) stand as implemented-on-main per their rows; spot-verified via merged-PR cross-links where cited.

## T007 — #6119 repro: CONFIRMED LIVE on H2 cell 2026-11-11

Cell: `perc-matrix-cms-h2` (`TEST_CMS_URL=http://127.0.0.1:9993`, `TEST_DB_TYPE=h2`), healthy per `qa-health` (`RESULT:OK ... HEALTH:healthy`). QA stack torn down after the run (`qa-down`).

Live run (Admin session-cookie auth, search `repro6119t4`):

1. `POST /Rhythmyx/services/searches {SearchDef:{name,label,type:StandardSearch}}` → **200** (row created, id 1001)
2. `PUT /Rhythmyx/services/searches/repro6119t4 {SearchDef:{name,label}}` → **500** `IllegalStateException: Search was saved but is not visible to findSearches: repro6119t4`
3. `GET /Rhythmyx/services/searches/repro6119t4` → **404** `Search not found` (row permanently lost)

Matches the issue repro exactly. Before-state for US3 recorded; T019 post-fix verification must repeat these three steps and expect 200/200/200 with the renamed label persisted.

## T008 — Product-docs reference check: VERIFIED, no drift

`product-docs/8.2/admin/content-explorer.md` Take Down / Stage / Unstage / Schedule / Clear-scheduled-dates rows inspected: per-item error semantics (400/403/404/409 naming, no-refresh-on-failure, folder-skip rules) match the merged slice behavior. No doc-bug filed.
