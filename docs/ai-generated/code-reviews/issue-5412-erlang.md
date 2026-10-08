# Erlang review — issue 5412

Persona: erlang 0.1.1
Tool: mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main
Recommendation: approve. In-diff bugs: 0. Preexisting complexity on `handleSaveFields` does not block. The in-diff `readNodeValue` row is a suggestion, not a bug, missing test, or non-portable path.

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

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:629 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=49 (max 15), cyclomatic=46 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/assembly/overlayFields.ts:1206 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `readNodeValue` cognitive=20 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
