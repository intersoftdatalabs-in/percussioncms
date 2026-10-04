# Erlang review — issue 5182

Calling-agent gate: **not blocking**. Machine analysis reported 0 bugs (11 files). The cognitive-complexity row is preexisting on `DeliveryTypesPanel` (cognitive=18, cyclomatic=18) and is not an in-diff bug. In-diff bugs, missing behavioral tests, and non-portable paths: none. May commit/push: yes.

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/publishing/design/DeliveryTypesPanel.tsx:46 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `DeliveryTypesPanel` cognitive=18 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
