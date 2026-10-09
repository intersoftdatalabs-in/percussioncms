<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5447

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5447
- Base: origin/main
- Head: 5ab6b91e835dc7ccbd0b1ae6abf1675ca5d88ee5
- Files analyzed: 12
- Reviewer: independent Erlang (did not author the PR)

## Independent reading

`renameContextVariable` renames one listed variable. A blank name, a blank new name, a blank context, or a name longer than 50 characters is HTTP 400 and does not save. A missing stored name, or a new name that matches a different variable on that context, is HTTP 409 and does not save. The same trimmed name does not save. `renameName` combined with `updateValue` is HTTP 400 and writes neither. A value sent with the rename is ignored. On `PSSite`, the same `PSSiteProperty` is removed, renamed, and put back so the property id and value stay. A variable with the same name on another context stays. The client omits `value`, does not PUT a blank, duplicate, or overlong name, and on 400, 403, or 409 leaves the old name. Cancel does not call the server. Vitest, the service tests (including property id and the other context), Playwright, and `product-docs/8.2/admin/publishing.md` are present. No filesystem path joins. No rule files.

`renameContextVariable` cognitive complexity is a suggestion at the ceiling. `associateContentList` is the following method; its body is not the rename. That complexity row is a suggestion, not a bug. The LLM stage failed open (Ollama CUDA out of memory). Neither blocks.

Recommendation: approve. May merge: yes.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1146 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `renameContextVariable` cognitive=15 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1263 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `associateContentList` cognitive=16 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

