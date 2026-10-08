# Erlang review — issue 5382

Persona: erlang 0.1.1
Tool: mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main
Models: models.ollama-dev-coder.toml (LLM layer failed: CUDA out of memory; machine findings kept)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
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

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:620 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=33 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}
- Status: open

## Gate interpretation

In-diff bugs: none. The cognitive-complexity row is preexisting on `handleSaveFields` and does not block. The LLM failure is an environment warning, not a product defect. Missing behavioral tests and non-portable paths: none in the diff. Required-date refusal, whitespace, cancel, valid save, HTTP 400/403/409, read-only omission, and optional blank (still not a clear) are covered by Vitest. Playwright covers the same assembly-host contract. No new filesystem path joins. No agent rule files.

Recommendation: approve. May commit/push: yes.
