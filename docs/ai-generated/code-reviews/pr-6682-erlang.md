# Pre-push local code review — PR #6682 (takedown batch retry-failed with named per-item errors, #6677)

Independent Erlang review (erlang 0.1.1). Author and implementer: night-issue-prs run; reviewer did not author the change.

## Reviewer verdict

**LGTM — approve** (merge only on green checks; see base note). Retry reuses the reviewed linked-page list (`review` stashes `multiLinked[safe]` for the single-item path; `reviewBatch` for multi), flips retried rows via `mergeReport`, and clears the button when nothing is left. Worst case on a partial retry is the previous error text, never a false success.

Manual checks beyond the machine report:

- Stale-row concern cleared: `mergeReport` accumulates only within one batch lifecycle because `review`/`reviewBatch` both `setReport(null)` on a new lookup — a fresh id list cannot inherit old rows. Retried-success rows flip via `rowsFromReport`; `failedIds` is recomputed from each run's failures.
- Single-item retry resubmits through the same `runBatch` path with the stashed linked list (no silent no-linked-pages regression); the retry button hides while `submitting`.
- New `PUBLISH_TAKEDOWN_RETRY_FAILED` key follows the existing `message()`/`@` pattern; no locale/TMX companion needed beyond the default (consistent with sibling keys).
- Behavioral Vitest covers retry-only-failed-ids, row flip, button clearing, and single-item retry; Playwright spec asserts `retryCalls` equals exactly the failed id plus a live proof screenshot; product-docs updated.
- Cross-platform: no filesystem path joins in the diff. Copyright: no new source files (edited TS/tests/helper/spec + docs/proof assets only).

Base note: this branch predated the #6681 pin fix, so its earlier QA-wiring run failed on the stale `percCalendar` widget pin (`pinned widgets with no shipped template`) — unrelated to the takedown change. Rebased onto current `origin/main` (now includes #6668/#6675/#6676) before this review; re-review run is on the rebased head.

No nits.

## Machine report (sanitized via `scripts/sanitize-erlang-review.py`)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._
