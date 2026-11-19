# Erlang re-review (r2) — PR #6710

- Status: **BLOCKED (human_blocked)** — do NOT merge.
- CLI: `mkd-code-review 0.5.0`, pack `percussion`, gate `advisory`,
  base `origin/main`, local Ollama route (no `--require-llm`).
- Scope: PR head `b8a3f2139d` (`fix/issue-6482-forms-retrieval-green`).
- Machine: **0 in-diff findings**. The single `llm.error` row below is an
  unavailable-LLM-stage notice (local route would not serve the contract
  this run) — recorded and disclosed here; per contract it is not a merge
  block when in-diff machine findings are 0.
- Manual: `DTS_URL` alias + unit test verified in-diff (r1); nothing new
  to gate on in code.
- Remaining human gate: live `--with-dts` 3/3 green run still PENDING —
  no QA cell with the provisioned `deliverymanager` credential (DTS secret)
  was available in this session. Body honestly records
  `LIVE --with-dts green run: PENDING`.
- Checks green (CodeQL pass, Analyze js-ts pass, QA wiring pass, docs
  smoke pass; H2 live skipping). All review threads resolved — but the
  live-run gate is human-owned, so the PR stays open.

## Machine report (sanitized)

## Summary

Machine analysis found **1** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
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
