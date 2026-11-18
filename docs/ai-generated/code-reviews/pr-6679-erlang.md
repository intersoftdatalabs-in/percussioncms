# Pre-push local code review — PR #6679 (Explorer stage/unstage dispatch specs follow the bulk stage dialog, #4553)

Independent Erlang review (erlang 0.1.1). Author and implementer: night-issue-prs run; reviewer did not author the change.

## Reviewer verdict

**LGTM — approve.** Test-only change following the merged bulk stage dialog (#6530): the two failure-path specs now drive `explorer-bulk-stage-dialog` (confirm → per-row failure report → close) instead of the removed native confirm + error region, assert the staging/takedown endpoint actually hit (page vs resource routes), and assert no list refresh (`data-list-epoch` stays `0`) and no stray error region on a fully-failed run. The unstage spec moves from a page row to the asset row so the resource takedown route (not the page route) is covered. The `every selected item` regex update matches the current `CONFIRM_SCHEDULE_MULTI` product copy (verified in `messages.ts`); the old `every selected page and asset` pattern no longer exists in product copy, so the update is required, not cosmetic.

Manual checks beyond the machine report:

- The `confirms` collector at spec line ~642 belongs to the untouched Schedule multi-select test, which keeps its `page.on("dialog")` handler — no vacuous-assertion risk; the removed handlers were only in the two rewritten tests.
- No product code changed; no new source files (spec + proof png + review doc only). Cross-platform/copyright gates N/A.

No nits.

## Machine report (sanitized via `scripts/sanitize-erlang-review.py`)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._
