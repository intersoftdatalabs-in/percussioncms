# QA record — assembly verification battery + Compare decision docs (issue #6200)

Date (UTC): 2026-11-14
Executed by: night-issue-prs work agent (OpenCode, muse-spark-1.3-contributor)
Spec: `specs/996-react-active-assembly/` (parent #6154 CLOSED; code merged via PR #6177, MERGED)
Result: **PASS — battery reads coherently, Compare decision recorded, docs smoke clean**

## Scope note

PR #6177 was docs/specs + one Playwright test only (no product UI change, no
Java module sources). Per the issue's own quality bar (Q4/Q5 N/A), no H2 live
cell re-run is required for this QA slice: the WP1 battery evidence (Vitest
280/280 + surface 9/9) was recorded on the merged-PR tip in #6177 and the
#6154 progress row. This slice re-verifies the merged content in-tree.

## Test-plan mapping (issue #6200)

| # | QA step | What was checked | Result |
|---|---------|------------------|--------|
| 1 | Open the assembly verification battery spec; confirm T001–T017 read coherently and the Compare decision is recorded | `tasks.md`: all 17 boxes ticked (T001–T017), Phase 1→5 order matches Dependencies chain; `checklists/coverage.md` maps FR-001–006 to in-tree layers + tests with "shipped — verify (WP1)" status; `checklists/closeout.md` T012 records Compare **defer** with rationale + revisit trigger | PASS |
| 2 | Spot-check 2–3 spec cross-links resolve to live content | (a) `sys_compare` blocklist entry → `WebUI/src/main/ts/contentExplorer/actionDispatch.ts:221` inside `DATA_FLOW_PATH_MARKERS`; (b) `@aa-denied` 403 test → `modules/perc-qa-automation/frontend/tests/explorer-active-assembly.spec.js:125`; (c) T011 deferred slices → #6224/#6225 CLOSED, PRs #6231/#6247 MERGED; (d) `compareRevisions` → `PSItemService`/`IPSItemService` (revision compare, not assembly Compare) | PASS (4/4) |
| 3 | Confirm product-docs smoke still builds (no broken includes from the new pages) | `scripts/ci-smoke-product-docs.sh` → `ci-smoke-product-docs: OK` (default `8.2/index.html` emitted). #6177 added no `product-docs/` pages, so no new include risk; smoke confirms the tree still builds on this tip | PASS |

## Pass / fail criteria (per issue)

- Specs render, decision text matches merged content, docs build clean → **all three hold: PASS**.
- No broken links or inaccurate decision text found; nothing to log.

## Out of scope (per issue)

- Implementing the Compare add-on itself; editor behavior changes.
