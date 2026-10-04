<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5150

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5150
- Branch: fix/issue-5136-delete-publishing-context
- Issue: #5136
- Base: origin/main
- Head: 963088804f116a68818924bc9fdda5535ba07f1c
- Reviewer: independent Erlang pass (not the author). Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
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


## Interpreter

Independent read of the diff (not the author). Confirm is required. The context leaves the list only after DELETE succeeds; a refresh failure still drops that id and does not pretend the delete failed. Dismissing confirm returns before any request. A context with location schemes is HTTP 409 and deleteContext is not called. PSRuntimeExceptionMapper keeps an explicit 4xx WebApplicationException status (409 stays 409). A wrapped server failure stays 500. requireDesignWrite runs in addition to the site-manager availability check. Companions are present: publishing-design tests, mapper status test, Vitest, surface Playwright, product-docs publishing. No new filesystem path joins. No agent rule files. The local coder model ran out of CUDA memory; that warning is not an in-diff bug. Machine findings kept. Recommendation: approve. May commit/push: yes.
