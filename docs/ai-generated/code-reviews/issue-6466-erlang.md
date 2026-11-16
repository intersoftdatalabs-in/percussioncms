## Summary

Machine analysis found **1** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
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
- Description: model `<llm-model>` failed: config: missing env <llm-provider-key-env> (for OpenRouter set <llm-provider-key-env> from https://openrouter.ai/keys)
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
