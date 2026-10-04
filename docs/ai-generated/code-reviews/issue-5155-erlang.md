# Erlang review — issue 5155

Pre-push `mkd-code-review` on the working tree versus `origin/main` (staged slice). Persona erlang 0.1.1. Author and reviewer are the same session; the CLI is the independent machine gate. Ollama `ollama-dev-coder` failed with CUDA out of memory; machine findings were kept.

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
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

- File: WebUI/src/main/ts/contentExplorer/setItemWorkflow.ts:198 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `planSetWorkflowMulti` cognitive=29 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/contentExplorer/setItemWorkflow.ts:325 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveSetWorkflowOnSelection` cognitive=23 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Interpreter

- Cognitive complexity on `planSetWorkflowMulti` and `saveSetWorkflowOnSelection` matches the #5133 community multi helpers. Suggestion only. Not a bug, missing test, or non-portable path.
- LLM layer did not return findings (CUDA OOM). Gate stays on the machine result: 0 bugs.
- Change-class closure in this diff: Explorer shell, dialog, row paint, Vitest, H2 Playwright spec, and `product-docs/8.2/admin/content-explorer.md`. Existing item-workflow REST is reused. `classifySetWorkflowSelection` still returns `multi` for a count greater than one.
- No new filesystem path joins.
- Recommendation: approve. May commit/push: yes.
