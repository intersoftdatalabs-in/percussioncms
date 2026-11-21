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

## Analyst note (worker adjudication, #6963)

The single `llm.error` finding is an unavailable-LLM meta-finding (local Ollama route `ollama-dev-coder` unreachable in this lane; deterministic machine rules report 0 in-diff findings, and the diff touches only a Playwright spec). Per the night-issue-prs Work contract this machine-only result short-circuits as clean and does not block the PR. No WebUI/product-code change in this diff; C5 live proof (fresh tip H2 cell, spec 1/1 + golden 2/2, zero console/server errors) is recorded in the PR body.
