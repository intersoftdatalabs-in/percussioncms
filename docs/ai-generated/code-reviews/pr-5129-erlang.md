<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5129

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: b81996b614af296861753908349a63b3fe5701bf
- Branch: fix/issue-5120-aging-interval
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 19 analyzed
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

Independent Erlang review of PR #5129 (not the author). Machine gate: 0 bugs. Recommendation: approve. May merge: yes.

`PUT /workflows/{id}/aging-transitions/interval` is Admin-only. Packaged workflows are 403. A non-positive or unchanged interval is 400 and does not save. A missing absolute edge is 404. A duplicate absolute interval is 409. A generated `Aging N` label, trigger, and description are rewritten; a custom label is left alone. The UI shows the new minutes and the success notice only after the PUT returns. Cancel does not call the server. `TestWorkflowsAdaptor` implements the new method. REST, sitemanage, Vitest, Playwright, and product-docs companions are present. No rule-file diff.

Non-blocking: the Change control is offered for every aging edge that has a numeric interval, including repeated and system-field edges. `changeAbsoluteInterval` refuses those types and does not persist them.

LLM stage: `ollama-dev-coder` CUDA out of memory (HTTP 500). Machine findings kept. Not a blocking finding.
