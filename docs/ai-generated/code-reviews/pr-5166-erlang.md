<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5166

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 22a450338455d15219ef0f08f4c60904080e6f71
- Branch: fix/issue-5158-rename-content-list
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
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

Independent Erlang review of PR #5166 (not the author). Machine gate: 0 in-diff bugs. Recommendation: approve. May merge: yes.

Design content-list edit leaves Name editable and Type disabled. Save trims the name and sends the stored list type, not a mutated select value. A blank name is rejected in the client and does not PUT. Back does not PUT. HTTP 409 does not call `onSaved`, and the Design list keeps the previous name until a successful reload. Description-only save still updates the description and does not rename. `PSPublishingDesignRestServiceTest` locks `setName` plus `saveContentList` and that list type is not rewritten. Vitest, Playwright, and `product-docs/8.2/admin/publishing.md` match that behavior. No production Java signature change. No rule-file diff. No new filesystem path joins.

LLM stage: `ollama-dev-coder` CUDA out of memory (HTTP 500). Machine findings kept. Not a blocking finding.
