<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5168

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 913a7a7ed42c57487bfa1c4288a0839733d0c968
- Branch: fix/issue-5153-remove-step-role
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 24 analyzed
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

Independent Erlang review of PR #5168 (not the author). Machine gate: 0 bugs. Recommendation: approve. May merge: yes.

DELETE /workflows/{id}/steps/{step}/roles/{role} removes one Reader or Assignee from one custom step and saves only after the assignment count drops by one. Admin and None stay (409). Packaged and system-default workflows are 403. Cancel does not call DELETE. The table drops the role only when a reload no longer lists that step and role. TestWorkflowsAdaptor implements removeStepRole. Playwright and product-docs/8.2/admin/developer-workflows.md plus developer/rest.md match that behavior. No rule-file diff.

LLM stage: ollama-dev-coder CUDA out of memory (HTTP 500). Machine findings kept. Not a blocking finding.
