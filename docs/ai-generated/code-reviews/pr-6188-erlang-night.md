# Erlang review — PR #6188 `fix(webui): paste clipboard items as links into AA slot (#6103)`

**Status:** BLOCK — 1 in-diff bug (review-only; no approve, no merge)
**Persona:** erlang 0.1.1 (`~/.agents/skills/erlang/`)
**Scope base:** `origin/main` · **Scope head:** PR #6188 head (`8fe11eb6f5`)
**In-diff files (9):** `ContentExplorerShell.tsx`, `messages.ts`,
`pasteLinkToSlot.ts` (new), `views/RelationshipsView.tsx`,
`RelationshipsView.test.tsx`, `pasteLinkToSlot.test.ts` (new),
`docs/ai-generated/code-reviews/issue-6103-erlang.md` (new),
`explorer-paste-link-slot.spec.js` (new),
`product-docs/8.2/admin/content-explorer.md`
**Checks snapshot (one poll):** CodeQL pass, JS/TS analyze pass,
product-docs build smoke pass, QA wiring pass; H2 live + java-kotlin skipping.
**Machine findings:** 0 (see CLI report below — machine is clean; the
blocker below is from independent manual read-through).

## Machine report (verbatim `mkd-code-review analyze`, pack `percussion`, gate `advisory`)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Independent findings (manual read-through — these gate the merge)

### BUG-1 (blocking): `confirmPaste` drops already-created rows when a later POST throws

`WebUI/src/main/ts/contentExplorer/views/RelationshipsView.tsx`
(`confirmPaste`, ~line 1234–1274):

- The per-item mismatch path (bad `slotId`/`dependentId`/non-positive
  `relationshipId`) calls `applyCreated(created)` before reporting the
  error — prior successful rows are kept. Correct.
- The `catch` path (a later `pasteLink()` POST rejects: network error, HTTP
  500/400/403/409) does **not** call `applyCreated(created)`. Rows the
  server already created are silently discarded from local `edges` state.

Repro: clipboard holds 2+ ids, POST for id #1 succeeds server-side, POST
for id #2 rejects. UI shows only the error; the slot list omits the row
the server actually created. The user retries → the first item is POSTed
again → duplicate relationship (or a confusing 409). UI and server diverge
with no way to see it.

This is in-diff new code and is wrong behavior on a partial failure, so it
blocks under the strict gate. The existing tests do not cover it: every
failure test uses a single clipboard id (where `created` is empty, so the
defect is invisible).

Suggested fix (for `erlang-fix`, not applied here): mirror the mismatch
path — call `applyCreated(created)` in the `catch` before
`setPasteError(...)`, and add a component test with
`clipboard: { operation: "copy", contentIds: [88, 89] }` where the mock
resolves once then rejects, asserting the first row is listed and the
error is shown.

### Non-blocking notes (do not gate)

- Dialog summary renders raw `clipboard.contentIds` while the gate dedupes
  and filters — cosmetic only; host already strips folders/non-numeric ids.
- `NIT`: none worth filing.

## Change-class closure (checked, complete)

- Pure helper `pasteLinkToSlot.ts` + Vitest unit tests (gate/ids/apply).
- Component tests: empty/cancel/cut/multi-id success/400-403-409/a11y gate.
- Playwright surface spec (`explorer-paste-link-slot.spec.js`, QA-mode
  `test:surface` documented in header).
- `product-docs/8.2/admin/content-explorer.md` updated for the new flow.
- Copyright headers (2026 Intersoft) present on all three new files.
- No rule/instruction files in the diff; no portable-path concerns (no
  filesystem I/O — numeric ids and URL paths only); no secrets.

## Verdict

**BLOCK / request-changes** — BUG-1 must be fixed and re-reviewed.
REVIEW ONLY: no approval posted, no merge. Not LGTM.

---

# Re-review (second pass after fix) — head `aa56bf1b48`

**Status:** LGTM-REVIEW-ONLY — 0 in-diff bugs (review-only; no approve, no merge per own-model constraint)
**Persona:** erlang 0.1.1 (`~/.agents/skills/erlang/`)
**Scope base:** `origin/main` · **Scope head:** PR #6188 head (`aa56bf1b48`)
**Fix commit:** `aa56bf1b48` on `fix/issue-6103-paste-as-link` — "keep already-created paste rows when a later POST fails"
**In-diff files (9):** unchanged set from first pass
**Checks snapshot:** machine gate re-run on new head (see below); prior snapshot (CodeQL pass, JS/TS pass, docs smoke pass, QA wiring pass) stands
**Machine findings (new head):** 0 blocking, 0 total — CLI exit 0, `approve`, `May commit/push: yes`

## Machine report (verbatim `mkd-code-review analyze`, pack `percussion`, gate `advisory`, models `config/models.ollama-dev-coder.toml`)

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Fix verification (manual read-through of `aa56bf1b48`)

Prior **BUG-1** (`confirmPaste` catch path dropped already-created rows) is
**fixed and closed**:

- `WebUI/src/main/ts/contentExplorer/views/RelationshipsView.tsx` (`confirmPaste`):
  `templateName` / `applyCreated` / `created` are hoisted above the `try`,
  and the `catch` now calls `applyCreated(created)` before
  `setPasteError(...)` — exactly the suggested fix, mirroring the
  per-item mismatch path. Verified in the PR-head tree.
- The hoist is side-effect-free (pure derivations from `gate`/`pasteChoices`/
  `setEdges`); the empty-guard inside `applyCreated` keeps the single-id
  failure path behavior-identical; `finally` still resets `pasteBusy`.
- Regression test added in
  `WebUI/src/test/ts/contentExplorer/RelationshipsView.test.tsx`:
  "a later POST failure keeps already-created rows (#6103)" — clipboard
  `[88, 89]`, mock resolves once (relationship 178) then rejects (500);
  asserts the error surfaces, `done` is absent, and slot 5 lists
  `["71", "72", "178"]` with the new edge bound to dependent 88. This is
  behavioral (render → click → confirm → assert DOM + slot state), not a
  string-grep test — satisfies the strict gate's test requirement.
- Per `erlang-fix-6188.json`: new test fails pre-fix, passes post-fix;
  full Vitest (906 files / 8697 tests) green, `tsc --noEmit` clean.

## New in-diff bugs

**None.** The fix delta (21 lines production + 37 lines test) introduces no
new branches, no new I/O, no path handling, no secrets, no rule files.
Catch-path `applyCreated` intentionally keeps already-created rows visible
while still reporting the error (no false-green: error is still set and
`done` stays hidden). The dialog-slot-id-not-cleared behavior on failure
matches the pre-existing mismatch path — consistent, not a regression.

## Change-class closure (re-checked, still complete)

Fix commit adds only the missing behavioral test for the previously
uncovered path; no new companions required. Product docs, Playwright
surface spec, unit tests, copyright headers — all from the first pass,
unchanged and intact.

## Residuals (preexisting tech-debt)

Machine rows on this head: **0** — nothing out-of-diff to dedupe or file.
No new `gh` issues created. `residual_issue_urls`: [].

## Verdict

**LGTM-REVIEW-ONLY** — BUG-1 fixed, regression test behavioral and
covering, machine gate clean (0 findings), no new in-diff bugs.
REVIEW ONLY (own-model PR): no APPROVE posted, no merge performed —
handing back to host for merge decision.
