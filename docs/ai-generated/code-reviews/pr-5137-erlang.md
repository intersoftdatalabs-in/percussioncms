<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5137

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5137
- Base: origin/main
- Head: 865f4c925e5d44d43c8632811fea47510e705e57
- Files analyzed: 17
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass. Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 17 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowTransitionRemover.java:44 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `removeOne` cognitive=19 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Interpreter

Independent read of the diff (not the author). `removeAbsoluteAging` deletes one absolute edge by from, to, and interval and leaves regular transitions in place. A repeated (or other non-absolute) match is 409 and is not removed. Missing workflow, step, or edge is 404. Non-positive interval and blank from/to are 400. Packaged and default workflows are 403 and are not saved. The Developer graph confirms before DELETE, keeps the row until the request resolves, and does not show success on 400, 403, or 409. Companions present: REST resource, `IWorkflowsAdaptor`, `WorkflowsAdaptor`, `TestWorkflowsAdaptor`, remover and adaptor tests, Vitest, Playwright, and product-docs. No new filesystem path joins. No agent rule files. `removeOne` complexity is preexisting and out of this diff. The local coder model ran out of CUDA memory; that warning is not an in-diff bug. Machine findings kept. Recommendation: approve. May commit/push: yes.
