# Erlang independent review — PR #5725 (issue #5706, parent #1690)

- Persona: erlang 0.1.1 (`~/.agents/skills/erlang/ERLANG.md` + `PATTERNS.md`)
- Reviewed commit: `5062fae3e3` (`fix/issue-5706-field-choices-reorder`)
- Method: `mkd-code-review analyze --pack percussion --format markdown
  --gate advisory --git-base origin/main` on the PR head worktree (Ollama
  `dev-coder-64k:latest` reachable; machine findings gate), plus manual diff
  read of all 7 files.
- Work-agent pre-push report: `docs/ai-generated/code-reviews/issue-5706-erlang.md`
  (already on branch; this file is the independent re-review, not a duplicate).

## Verdict: LGTM (comment only — no APPROVE/merge: own-model night PR, left open for human/other-model merge)

## bugs_found: none

## Manual findings

- `moveChoiceEntry` (pure, bounds-clamped, never mutates input) is correct;
  reorder flows through the same `patch → commit → onChange` path as
  add/remove/update, so parent dirty-compare treats it like any entry edit.
- Behavioral unit tests cover move semantics, bounds, null, non-mutation.
- Playwright proves the claim end to end: PUT body contains `"choices"` after
  reorder, GET returns the new order, unlock-without-save writes nothing,
  409 clears lock without claiming success, other field untouched, catalog
  restored in `finally`.
- C1/C3/C5 evidence present: WebUI `clean install` BUILD SUCCESS (836 files /
  8086 tests, Surefire 69/0/0, tsc clean), perc-qa-automation BUILD SUCCESS,
  H2 QA surface 2 passed + regression 2 passed, zero console errors,
  `server.log` ERROR/FATAL 0.
- product-docs updated (`developer-content-types.md`, checklist checked).
- No file I/O / path handling in diff — cross-platform checklist N/A.
- In-diff CLI finding is suggestion-severity only (spec-helper complexity,
  verbatim peer-consistent). Preexisting complexity bug is out of scope.

## Full CLI report (HEAD at review time)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/api/developer/contentTypeChoiceCatalog.ts:340 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `toChoiceCatalogPayload` cognitive=34 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/developer-content-type-field-choices-reorder.spec.js:54 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `unwrapControlProperties` cognitive=21 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
