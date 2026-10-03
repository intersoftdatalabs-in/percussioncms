<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5115

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 1ed1cd1e3300d2012286926cfb19ad8cd028fbaf
- Branch: fix/issue-5107-associate-content-list
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
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

## Erlang gate (reviewer)

Machine gate: 12 files, 0 bug findings. The only machine row is the Ollama `dev-coder` CUDA OOM warn, which is not a defect in this diff. In-diff blocking bugs: 0.

Recommendation: **approve**.

Checked by hand: the SPA posts the `editionContentList` root so `UNWRAP_ROOT_VALUE` binds the ids. Blank content list or delivery context does not call the server. `requireDesignWrite` returns 403 before a load. A second association of the same content list is 409 and does not call `saveEditionContentList`. That matches the edition+content-list primary key (a different delivery context is not a second row). The row is added only after HTTP success, and an in-flight list reload cannot wipe it (`assocLoadGen`). Numeric `contentListId` is coerced to a string. Product docs, Jackson test, 403/409 service tests, Vitest, and the Playwright surface spec are in the diff. No rule-file changes. No new filesystem path joins.
