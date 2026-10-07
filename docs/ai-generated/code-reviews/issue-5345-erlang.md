# Erlang review — issue 5345

Same-session disclosure: the implementer and this review ran in one night-issue-prs session (Grok Build 1.0.46, grok-4.7). The report is the `mkd-code-review` CLI (erlang 0.1.1, pack percussion, gate advisory, base `origin/main`). This session does not self-approve the pull request.

LLM stage: `ollama-dev-coder` returned HTTP 500 (CUDA out of memory while loading the model). Machine findings are kept. That warning is not a gate failure.

Gate decision: no in-diff bugs, no missing behavioral tests, no non-portable path findings. The `handleSave` complexity row is preexisting and does not block. May commit and push.

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
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

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1370 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=138 (max 15), cyclomatic=172 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
