# Erlang review: issue 5244

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
  --git-base origin/main \
  --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Head commit under review: `dea06762483e80de0e75ef3648de2f73e5bf39df`.
Ollama `ollama-dev-coder` returned HTTP 500 (CUDA out of memory). Machine findings kept. LLM warn is not a gate failure.

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 16 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowsAdaptor.java:795 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `deleteTypedAgingTransition` cognitive=16 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 1879048192\nllama_init_from_model: failed to initialize the context: failed to allocate buffer for kv cache","type":"api_error","param":null,"code":null}}

- Status: open

## Gate interpretation

No in-diff bug, missing behavioral test, or non-portable path finding. The cognitive-complexity row is a suggestion (16 vs max 15) and does not block. Behavioral tests cover repeated and system-field delete, 409 when the matching edge is a different type, and the UI cancel / 400 / 403 / 409 paths.
