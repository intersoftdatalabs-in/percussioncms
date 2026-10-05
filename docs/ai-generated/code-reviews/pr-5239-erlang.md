<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 5239

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- Base: origin/main (862ba06dbfa063670c0992537332dc10254c3939)
- Head: 5efacf89aebfb3f3b6b19f6a8a12c429f66c1d2f (fix/issue-5223-edition-priority)
- LLM: ollama dev-coder failed (CUDA out of memory). Machine findings kept. Not a product defect.

## Erlang interpretation

In-diff bugs: 0. A priority-only `PUT` sends `{ priority }` and omits name, comment, and site id. `updateEdition` leaves those stored when they are blank or null, and it does not call the content-list association APIs. Null priority still skips `setPriority`, so name-only updates are unchanged. Values `findByValue` rejects (0, 6, 9) return HTTP 400 before load or save; 403 returns before load. The form accepts only a trimmed whole number 1–5, Cancel does not call `updateEdition`, and HTTP 400/403/409 stay on the form and do not replace the list priority. Vitest, the publishing-design service tests, Playwright, and `product-docs/8.2/admin/publishing.md` are present. No rule-file diff. No new filesystem path joins.

Non-blocking: a priority-only body does not take the name-uniqueness path, so HTTP 409 is unlikely. The form still keeps the previous priority if a 409 is returned. Not a merge block.

Gate: PASS
May commit/push: yes

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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
