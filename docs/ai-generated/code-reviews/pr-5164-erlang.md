<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5164

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 796a0dbee0094d864594b045fad18456ca08d154
- Branch: fix/issue-5152-add-step-role
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 25 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Agent note

Independent Erlang review of PR #5164 (not the author). Machine gate: 0 in-diff bugs. Recommendation: approve. May merge: yes.

`POST /services/workflows/{id}/steps/{step}/roles` adds one existing workflow role through `PSState.addAssignedRole`. Reader and Assignee only. Notify and inbox stay at `PSAssignedRole` defaults (`y` / `y`). Packaged and system-default workflows are 403 before save. Duplicate assignment is 409. Missing workflow, step, or role is 404. Blank role or any other type is 400. The Developer table shows the role only after a reload that contains that step, role, and type. Cancel does not POST. `IWorkflowsAdaptor` implementors in this tree (`WorkflowsAdaptor`, `TestWorkflowsAdaptor`) both implement `addStepRole`. Vitest, resource tests, adaptor tests, Playwright, and `product-docs/8.2` match that behavior. No rule-file diff. No new filesystem path joins.

The Add role dropdown is filled from roles already present on some step. That matches the product path (choose a role the step table already knows). It is not a defect in this slice.

LLM stage: `ollama-dev-coder` CUDA out of memory (HTTP 500). Machine findings kept. Not a blocking finding.
