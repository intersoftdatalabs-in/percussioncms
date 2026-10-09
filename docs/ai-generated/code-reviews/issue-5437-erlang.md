## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1146 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `renameContextVariable` cognitive=15 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1263 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `associateContentList` cognitive=16 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Gate note

Machine gate is advisory approve. Blocking bugs: 0. The Ollama layer failed with CUDA out of memory; that warning does not block. `associateContentList` complexity is a pre-existing function whose line number moved when the rename helper was inserted. `renameContextVariable` is at the complexity ceiling and is a suggestion, not a bug. No in-diff bug, missing behavioral test, or non-portable path finding.
