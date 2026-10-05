# Erlang review — issue 5178 community description

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base origin/main \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Scope: branch diff vs origin/main (18 files). Gate: advisory. In-diff bugs: 0.
The cyclomatic suggestion is not a bug, missing behavioral test, or non-portable path, so it does not block.

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 18 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/CommunityAdaptor.java:290 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateCommunityDescription` cognitive=14 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
