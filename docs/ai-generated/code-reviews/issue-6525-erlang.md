## Summary

Machine analysis found **2** finding(s) (1 suppressed), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 1 analyzed
- In-diff: 1 finding(s); preexisting: 0
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

### Suppressed Findings (1)

<details>
<summary>Click to view suppressed findings (not blocking gate)</summary>

- `paths.hardcoded_sep` on `rest/src/main/java/com/percussion/rest/contenttypes/ContentTypesResource.java:1112` -- *Justification*: : adjudicated intentional (issue #6525) — (via in_source_mkd)

</details>
