# Pre-push local code review — issue #6696 (takedown review-path retry-failed)

Slice: PublishingShell site-workspace **takedown batch review retry**, parity
with #6677 (takedown run retry, merged #6682). `Related to #6696`, parent #4531.

Review-path parity: `reviewTakedownBatch` names linked-page lookup failures per
item but offered no re-review. The panel now tracks failed review ids behind
the same retry button (new `failedOp` run/review split), re-runs
`reviewTakedownBatch` for failed ids only, and merges the results into the
reviewed linked-page map on the same confirm. Also fixes the loaded count
(`reviewed.length - failures.length`; `reviewed[]` keeps failed ids).

Manual checks beyond the machine report:

- Retry routes on `failedOp`: review failures re-review only the failed ids
  (`retryReview` merges into `multiLinked`, keeps full `multiIds` confirm,
  clears the button on full success); run failures still route to `runBatch`.
- Single-item review path unchanged (no retry button on the single confirm);
  single-item run retry still resubmits through `runBatch` with the stashed
  linked list.
- Behavioral Vitest covers partial-review retry (failed-id-only re-read,
  row flip, button clearing, loaded-count fix), full-review no-retry, and
  single-item unchanged. Playwright spec adds a review-retry test with a
  healed-lookup second route plus a live-proof screenshot.
- Reuses existing `PUBLISH_TAKEDOWN_BATCH_REVIEW_PARTIAL` /
  `PUBLISH_TAKEDOWN_RETRY_FAILED` keys (no new locale/TMX entries, same as #6682).
- Cross-platform: no filesystem path joins in the diff (CMS logical-path
  `/`, URL routes, and test ids only). Copyright: no new source files
  (edited TS/tests/spec/docs only).

## Machine report (sanitized via `scripts/sanitize-erlang-review.py`)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._
