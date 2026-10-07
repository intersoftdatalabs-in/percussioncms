## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
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

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:261 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleAddChoice` cognitive=8 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:435 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceLabelSave` cognitive=10 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

