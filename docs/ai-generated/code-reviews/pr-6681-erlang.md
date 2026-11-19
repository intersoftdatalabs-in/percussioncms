# Erlang review — PR #6681 (fix/issue-6680-calendar-pin)

**Status:** LGTM — approve. No BLOCK findings.
**Persona:** erlang 0.1.1 (independent reviewer; not the implementer)
**Date (UTC):** 2026-11-18
**Branch:** `fix/issue-6680-calendar-pin` → `main`

## Machine result (sanitized — raw CLI piped through `scripts/sanitize-erlang-review.py`)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Independent manual pass (Erlang Shen, third eye)

Diff read live via `gh pr diff 6681` — 3 files, all in
`modules/perc-qa-automation` + this report dir:

1. `frontend/tests/helpers/published-widget-matrix.js` — removes the stale
   `percCalendar` pin; corrects the inventory comment 48 → 47.
2. `frontend/tests/unit/published-widget-matrix.test.js` — pinned-count
   assertion 45 → 44.
3. `docs/ai-generated/code-reviews/issue-6680-erlang.md` — implementer
   evidence (pre-existing pattern for this matrix; docs-only).

### Verdicts

- **Correctness (in-diff bugs: 0).** Verified against the PR head: only
  `percCalendarTwo` ships under
  `perc-widget.calendar/widgets/` (v1 `percCalendar` dir gone after #6656),
  and the on-disk `*Snippet.vm` template count is exactly **47** — the
  corrected comment is factual, not aspirational.
- **No dangling references.** `git grep '"percCalendar"'` over
  `modules/perc-qa-automation/` on the PR head: zero hits. Remaining
  `percCalendar*` hits (`percCalendarAsset`, `percCalendarControl`) are the
  content-type/editor-control surface, unrelated to the v1 widget. The
  matrix spec (`published-widget-matrix.spec.js`) is fully data-driven over
  `WIDGETS` (`WIDGETS.map` / `WIDGETS.forEach`, no hardcoded names), so no
  spec edit was needed — closure is complete, not missing.
- **Behavioral tests (gate: PASS).** The acceptance for this slice *is* the
  unit suite, and it now passes: `node --test
  tests/unit/published-widget-matrix.test.js` → **8 pass / 0 fail**,
  including `pins a closed, duplicate-free inventory` (44),
  `closes the inventory against the shipped widget templates on disk`
  (both directions: no unaccounted on-disk dir, no pin without a template),
  and `is referenced by the matrix spec for every pinned widget`.
  This matches the green `QA wiring (dry-run / unit / list)` check on the PR.
- **Cross-platform paths: clean.** No new path construction; the test's
  disk scan uses `path.join` throughout.
- **Copyright header gate: N/A.** No new source files (two JS edits + two
  markdown evidence notes under `docs/ai-generated/`).
- **Change-class closure: complete.** Test-only slice; doc comment, pin,
  and assertion updated atomically. No product-docs surface (internal QA
  matrix, not operator-facing). No fixture/spec companions outstanding.

### Residuals

None filed. Machine rows in PR-touched files: 0. Open-issue dedupe:
#6680 (this PR's `Fixes` target) and #6673 (same drift, duplicate signal)
are both *closed by* this merge, not new debt; #6238 is the parent epic.
Zero qualifying out-of-scope rows → empty residuals is per policy.

## Checks (one snapshot)

- `QA wiring (dry-run / unit / list)` — SUCCESS (the check this PR unblocks)
- `Analyze (javascript-typescript)` — in progress at review time; machine
  findings on this JS-only diff are 0 per local CLI run
- `Analyze (java-kotlin)` / `H2 qa-up + Playwright` — SKIPPED (no Java /
  live-surface touch; correct skips)

## Recommendation

**Approve + squash-merge.** This PR unblocks the QA-wiring gate on sibling
PRs #6668, #6675, #6676, #6679.
