# Erlang review — PR #5642 (fix #5632: Explorer single-item schedule dates)

Status: reviewed, machine findings clean (in-diff)
Persona: erlang 0.1.1 (`~/.agents/skills/erlang/ERLANG.md` + `PATTERNS.md`)
Reviewer: independent of the Work implementer; no fixes implemented in this turn.
Ollama: dev-coder via `/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml` on `localhost:11434`.
Gate: in-diff bugs only; preexisting rows do not block.

## Erlang verdict

**Recommendation: approve. May commit/push: yes. Blocking bugs: 0.**

Manual pass over the full in-diff (the machine found nothing in-diff, so the
third eye did the reading):

- `actionDispatch.ts` — dispatch-level `validateScheduleDateRange` guard
  reuses the existing picker validator, returns the same message keys, no
  confirm and no POST, no refresh. Correct placement: before confirm/POST.
- `actionDispatch.ts` — single-item `SCHEDULE_FAILED` names the item with
  `Home (HTTP 403)`-style detail, no list refresh, selection kept. The
  non-status fallback always has a non-empty message
  (`scheduleFailureMessage` never returns empty; `only.name` falls back to
  id), so no `"null"` literal risk. `isApiError` was already imported
  (line 43) — no missing import.
- `itemScheduleDates.ts` — `status?: number` is additive/optional; rename
  `failureMessage` → exported `scheduleFailureMessage` is behavior-neutral.
- `messages.ts` — one new `SCHEDULE_FAILED` key with `{detail}` placeholder,
  substituted via `split().join()` (safe, no regex).
- Tests (behavioral, blocking-grade): vitest invalid-range refusal (no POST,
  only the `getitemdates` read) + `it.each([400,403,404,409])` single-item
  naming with no-refresh assertions. Playwright
  `explorer-schedule.spec.js` (new, Intersoft 2026 header): save persists +
  refresh, invalid range zero-POSTs, cancel writes nothing, 403 names item
  with prior dates intact on reopen.
- Change-class closure complete for a WebUI product-screen slice: unit
  (vitest) + surface (Playwright) + `product-docs/8.2/admin/content-explorer.md`
  Schedule row. No REST change (`setitemdates` contract reused), no new
  file I/O (cross-platform gate N/A, no `/` path joins in diff), no secrets.
- Preexisting `dispatchAction` cognitive/cyclomatic complexity is out of
  scope for this slice (residual, not introduced here).

## Checks snapshot (one snapshot max)

CodeQL pass; Analyze (javascript-typescript) pass; Analyze (java-kotlin)
skipped (no Java); product-docs build smoke pass; QA wiring pass.
`mergeable: MERGEABLE`, `mergeStateStatus: BLOCKED` (review-required gate —
this approval clears it).

---

## Machine report (verbatim `mkd-code-review analyze`, full CLI markdown)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:1767 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=489 (max 15), cyclomatic=297 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
