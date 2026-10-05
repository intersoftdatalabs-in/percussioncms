# Erlang review — issue 5222

Persona: erlang 0.1.1
Tool: mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main
Head: 7676497ccf4dbb6933de796a0d704037a4ef1bf8
Ollama `ollama-dev-coder` returned HTTP 500 (CUDA out of memory). Machine findings were kept. That warn is not a gate failure.

Gate: preexisting cognitive complexity on `DeliveryTypesPanel` is not an in-diff bug. No blocking bugs. May commit/push: yes.

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/publishing/design/DeliveryTypesPanel.tsx:60 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `DeliveryTypesPanel` cognitive=36 (max 15), cyclomatic=37 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error (CUDA out of memory while allocating the kv cache). Machine findings were kept.
- Status: open
