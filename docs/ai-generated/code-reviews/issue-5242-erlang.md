<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 5242

Reviewed the branch diff for Developer add one repeated aging transition
(`fix/issue-5242-repeated-aging-transition` vs `origin/main`).

Command:

```
mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

CLI: mkd-code-review 0.1.18. Persona: erlang 0.1.1. Gate uses in-diff bugs, missing behavioral tests, and
non-portable paths. The in-diff cognitive-complexity row is a suggestion, not a
bug. Preexisting `collect` complexity is out of this diff. No `llm.error` in
the CLI markdown.

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Recommendation: approve

## CLI markdown

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 19 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowGraphProjector.java:157 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `collect` cognitive=28 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowTransitionWriter.java:205 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `sameTypedInterval` cognitive=16 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
