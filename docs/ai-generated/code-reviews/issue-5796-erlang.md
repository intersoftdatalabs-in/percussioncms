## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- In-diff: 2 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: modules/DesktopContentExplorer/src/test/java/com/percussion/cx/PSContentExplorerLoginPanelPasswordTest.java:52 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 52)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: suggestion

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerLoginPanel.java:705 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getLocaleList` cognitive=16 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

