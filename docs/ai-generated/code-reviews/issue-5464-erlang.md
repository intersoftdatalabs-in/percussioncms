# Erlang review — issue 5464

## Agent disclosure

Same-session author and reviewer (night-issue-prs, Grok Build 1.0.60, grok-4.6). The review was not delegated to a nested agent. Machine review command:

```
mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Stderr was empty (no LLM warning). Gate used for this run: in-diff bugs, missing behavioral tests, and non-portable paths. Preexisting rows do not block.

- Issue 1 is preexisting cognitive complexity on `handleSave`. This slice does not change that function. Not a block.
- Issue 2 is an in-diff suggestion (`savedContentTypeLabel` complexity). It is not a logic bug, a missing behavioral test, or a non-portable path. Not a block.
- Behavioral tests cover the label write, blank-label name echo, and rejection of description, enabled, and name changes. Paths in this diff are not filesystem joins.

Blocking bugs: 0. May commit: yes.

## CLI report

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1111 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=85 (max 15), cyclomatic=84 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/contentTypeLabel.ts:105 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `savedContentTypeLabel` cognitive=18 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
