# Closeout Note — 009 Content Explorer Increments Acceptance & Closure

**Epic**: [#4530](https://github.com/intersoftdatalabs-in/percussioncms/issues/4530) (open; this note is the FR-008 closeout record)
**Branch**: `4530-content-explorer-increments` (uncommitted working tree at time of writing)
**Date**: 2026-11-11

## US1 — Goal-trio acceptance (CLOSED)

H2 QA cell (`perc-matrix-cms-h2`), fresh SPA deployed (stale-bundle guard documented in the evidence packet):
`explorer-takedown` 4 passed, `explorer-multi-takedown` 3 passed, `explorer-staging` 7 passed,
`explorer-multi-stage` 3 passed, `explorer-multi-remove-staging` 3 passed,
`explorer-schedule` 5 passed, `explorer-multi-schedule` 4 passed — 29/29.
Full evidence: [acceptance-evidence.md](acceptance-evidence.md). SC-001 met.

## US2 — QA tail (CLOSED, pending QA sign-off)

Evidence for #4553 (stage/unstage) and #5799 (tree expand/collapse) posted as issue comments with run details;
labels intentionally left for QA. No spawned/named QA beyond the frozen list. SC-002 reconciled:
remaining open in-scope QA is exactly those two tickets with evidence attached.

## US3 — #6119 backend defect (CLOSED)

Root cause: `saveComponents` delete-then-insert drops the row while the re-INSERT leg
writes 0 rows on H2 (verified via H2 TRACE_LEVEL_FILE statement trace: DELETEs execute,
no INSERT/UPDATE follows, commit, then "not visible" on reload).
Fix in `system/webservices/.../PSUiDesignWs.java`:
- skip delete-then-insert for `PSSearch` (direct UPDATE path, children merge as usual);
- post-save JDBC verify-and-repair (`ensureSearchRowCurrent`: re-INSERT missing rows,
  UPDATE stale rows), mirroring the proven create-path `ensureSearchRowPersisted` net.
Verification on H2 cell: label PUT → HTTP 200 with new label, GET shows it, offline DB
dump confirms the row updated in place with all child rows intact (2 fields + 3 properties
on the seed fixture), no orphans, no duplicates.
Unit tests: `PSUiDesignWsSaveRoutingTest` 4/4 green. Module gate:
`cd system && ../mvnw clean install` → BUILD SUCCESS (3157 tests, 0 failures).

## US4 — Merge-tail reconciliation (CLOSED)

All 102 PR references in the #4530 body table verified MERGED via `gh pr view`; zero bare
`pr_opened` rows; no new slice issues required (nothing abandoned). 992 parity matrix and
2400 Present rows unaffected (no Explorer surface changes beyond the server-side search fix;
WebUI untouched).

## Follow-ups (out of scope, tracked separately)

- Field-bearing search updates (add/remove fields via REST PUT) were silently ignored
  before this change and remain so — the repair covers row presence/staleness, not child
  merging. If product needs it, file a dedicated slice (the pipe's UPDATE leg writes
  nothing observable on H2; verified via statement trace).
- #4553 / #5799 still need human QA sign-off (evidence attached).
