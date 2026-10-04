<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5147

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5147
- Base: origin/main
- Head: 2c0c160869f9dde803823286d2a0a3d3525f478b
- Reviewer: independent Erlang (did not author the PR)
- Title: feat(publishing): delete a Design content list

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1461 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `rejectContentListInUse` cognitive=22 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Erlang disposition

Independent review (did not author the PR). `mkd-code-review` 0.1.18, pack percussion, `--gate advisory`, `--git-base origin/main`. Ollama `dev-coder` failed with CUDA OOM; machine findings were kept.

No in-diff bugs. The only machine row is cognitive complexity of `rejectContentListInUse` (cognitive 22 / cyclomatic 16), a **suggestion**. `findAllEditions("")` returns every edition (blank filter is not a site filter). Delete runs only after `requireDesignWrite`, a loaded list, and no edition link; HTTP 409 does not call `deleteContentLists`. The editor calls `onSaved` only after DELETE succeeds, which closes the editor and reloads the list. Cancel does not call the server. Tests cover 400, 403, 404, unassociated delete, and in-use 409. No rule-file diff.

**Recommendation: approve.**
