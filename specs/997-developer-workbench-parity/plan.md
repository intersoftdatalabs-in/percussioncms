# Plan: Developer Workbench Parity (997)

**Spec**: `spec.md` · **Parent**: #1690 (execution vehicle — one PR per slice, table updated on merge)
**Branch**: `spec-997-developer-workbench-parity` (acceptance layer only; no product code here)

## Goal

Give #1690's slice execution a parity gate: full inventory coverage mapping, Present-only-on-human-QA evidence rule, and a closeout that proves 100% rx732 parity. This package amends process, not slices.

## Phases

| Phase | Work | Maps to |
|---|---|---|
| WP0 | Reconcile every inventory §4–14 capability to a gap-matrix row (slice link or explicit Missing); classify "Other" bucket; disposition thin areas (UI Elements, Navigators) as slice-growth or scoped-out with rationale | SC-001, FR-001–008 |
| WP1 | Attach QA evidence expectations to in-flight slices (61 PR-open): what human-QA proof flips each Partial row to Present | SC-002 |
| WP2 | Drive the two decision points: thin-area scope notes; any slice blocked >30 days gets implement-or-defer decision in #1690 | SC-002, SC-003 |
| WP3 | Closeout: matrix 100% Present + slice table 100% done/verified + QA results linked back to #1690 | SC-004 |

## Execution rules (inherited)

- One PR per slice; update #1690's table on merge (unchanged).
- Review deferrals still go in `review-followups-tech-debt.md` if not fixed in-PR (unchanged).
- Playwright hard gate for user-visible screen changes (unchanged — record specs + CMS install + pass/fail in PR bodies).
- New rule from this spec: **no row flips to Present without closed human QA** (evidence link required in the matrix row).

## Gates

Erlang pre-commit review for this package (docs-only, expect clean). No product builds (no code changes).
