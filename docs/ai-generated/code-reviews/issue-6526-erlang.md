# Erlang pre-push review — issue #6526 (branch `fix/issue-6526-contenttypedetail-handlesave`)

- Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory
  --require-llm --git-base origin/main` with the local dev-coder model pool
  (OpenRouter route unavailable in this environment — no provider key; local pool
  used instead, no paid fallback).
- First run flagged an in-diff `complexity.cognitive` suggestion on the new
  `runSavePrechecks` (cognitive 9 / cyclomatic 21, max 15): fixed by extracting
  `isBulkSaveNeeded` + `hasPendingSaveWork` before re-running.
- Final run below: **0 in-diff machine findings**. The remaining `llm.error` is an
  LLM response-contract failure (small local model, reproduced twice), not an
  in-diff defect; the tool itself reports `May commit/push: yes`.
- Compensating evidence: `tsc --noEmit` clean, eslint clean on the touched file,
  focused suite 122/122, full WebUI `mvnw clean install` BUILD SUCCESS
  (vitest 946 files / 9165 tests, Surefire 82 tests, 0 failures).

## Sanitized CLI report (final run)

## Summary

Machine analysis found **1** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: response contract: finding location is outside the provided diff
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
