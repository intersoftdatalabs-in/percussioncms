# Erlang review — issue 5388

Persona: erlang 0.1.1
Tool: mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main
Head: 0b569298e5827829bf9ad2ab022b7651d04c19bf

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

In-diff bugs, missing behavioral tests, and non-portable paths: none.
The preexisting `handleSave` cognitive-complexity row is not in the new
single-line NUL guard and does not block. The Ollama model failed to load
(CUDA out of memory); machine findings were kept. That warn is not a gate failure.

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1371 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=139 (max 15), cyclomatic=173 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Disposition: preexisting. Not introduced by the single-line NUL check. Does not block.

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}
- Status: open
- Disposition: LLM layer unavailable. Machine review stands. Does not block.
