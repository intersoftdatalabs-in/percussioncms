# Erlang review — PR #6217 (night-issue-prs, REVIEW ONLY)

**Status:** LGTM-REVIEW-ONLY (own-model PR — no APPROVE, no merge, no LGTM review posted)
**PR:** intersoftdatalabs-in/percussioncms#6217 — `fix(webui): disposition 990 unverified-edges bundle (#6092)`
**Head:** `fix/issue-6092-unverified-edges-bundle` (analyzed at `7fef6b45c1`, 9 files, +249/−26)
**Author/operator:** natechadwick-intsof / opencode, model:muse-spark-1.3-contributor
**Reviewer:** Erlang Shen (independent, read-only; did not author the change)
**Date:** 2026-11-12
**Checks snapshot:** CodeQL SUCCESS, Analyze (javascript-typescript) SUCCESS,
Analyze (java-kotlin) SKIPPED (no Java in diff — correct), Detect language changes SUCCESS

---

## Full CLI report (`mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models config/models.ollama-dev-coder.toml`)

Run from a detached checkout of the PR head (`pr-6217-head`). Exit 0.
Ollama `dev-coder:latest` on `localhost:11434` present; models file present and used.

## Summary

Machine analysis found **0** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

---

## Erlang independent verification (diff read, not a rubber stamp)

### 1. Session-expiry early-return in 3 editors — CORRECT, follows documented convention

`ServerEditor.tsx`, `ContentListEditor.tsx`, `EditionEditor.tsx` each gain an
`isSessionRedirectError(e) → return` guard in both save and delete catch blocks.
Verified against `WebUI/src/main/ts/api/client.ts`:

- `SessionRedirectError` docstring: *"Callers should not surface this as a normal
  API failure (toasts / form errors)"* — the new guards implement exactly this.
- `isSessionRedirectError` (`err instanceof SessionRedirectError`) is already the
  established convention across ~60 call sites (Item* panels, wizards, dashboard
  widgets), so the "same convention as ItemPublishNowPanel" claim checks out.
- The `finally { setSaving(false) }` still runs, so the save button re-enables;
  dirty state is intentionally preserved for post-login retry. No stuck-spinner,
  no swallowed-error footgun (navigation to React Login is the surfacing).

### 2. Behavioral tests for every code path — gate satisfied

- `ServerEditor.save.test.tsx`: 409-conflict test renders, types, saves, asserts
  the "already exists" alert **and** that the typed value survives, then retries
  successfully (`onSave` called twice, `onDirtyChange(false)`). True behavioral
  coverage of the FR-020 last-write-wins surfacing — not a source-grep test.
- All three save suites gain a `SessionRedirectError` test asserting silence
  (`queryByRole("alert")` null), button re-enabled, and dirty preserved
  (`onDirtyChange`/`onSaved` not called with success). Each exercises the new guard.
- `statusPolling.test.ts` (weak `<=10s` budget assertion) deleted and replaced by
  `statusPolling.test.tsx`: locks `STATUS_POLL_INTERVAL_MS === 5_000` **plus** a
  fake-timer test proving refetch on every tick (1 → 2 → 5 calls) with no reload.
  Strictly stronger; no coverage lost. New file carries the correct
  `Copyright (c) 2026 Intersoft Data Labs, Inc.` header.

### 3. Parity claims verified against sources (not taken on trust)

- Minuet: `WebUI/war/views/PercPublishStatusMinuetView.js`
  `renderPublishStatusSection()` → `setInterval(..., 5000)` — exact 5s confirmed.
- React: `STATUS_POLL_INTERVAL_MS = 5000` in `StatusSection.tsx`, wired into
  `setInterval(load, STATUS_POLL_INTERVAL_MS)` — exact parity confirmed.
- Log-volume claim (server-bounded maxcount) is evidence-cited to pre-existing
  `logsFilter.test.ts`, which is out of this diff but the claim is about existing
  behavior, not new code — acceptable as evidence, not a new assertion.

### 4. Checklist

- [x] No in-diff bugs (machine 0 + manual read clean)
- [x] Behavioral tests for all new/changed non-trivial logic
- [x] Change-class closure: WebUI-only slice, no REST/API surface, no packaging,
      no i18n additions — no missing companions
- [x] No filesystem path handling in diff — cross-platform gate N/A (clean)
- [x] No secrets, no swallowed exceptions, no rule-file changes, no git-identity issues
- [x] `product-docs`: N/A — no operator/admin procedure or config surface changes;
      behavior refinements (silent redirect, exact poll interval) alter nothing documented
- [x] Accept-risk notes (live two-browser concurrency, unsaved-draft persistence
      across login round-trip) are explicitly recorded in the PR body — honest scoping,
      not hidden debt

## Residuals (preexisting/out-of-diff)

CLI reported **0** total findings, so there are no preexisting machine bug or
complexity rows in PR-touched files to dedupe or file. `residual_issue_urls: []`.

## Verdict

**LGTM-REVIEW-ONLY.** No blocking bugs, in-diff or otherwise. Because this is an
OWN-MODEL PR (model:muse-spark-1.3-contributor + operator:opencode), per the hard
constraint: **no APPROVE posted, no merge performed, no LGTM review posted.**
Host decides next steps (peer-PR flow or self-merge per policy).
