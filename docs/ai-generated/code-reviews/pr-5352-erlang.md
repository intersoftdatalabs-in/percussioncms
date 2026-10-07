<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5352

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5352
- Base: origin/main
- Head reviewed: 19a925f06f899144015757b75db590109db54c59
- Files analyzed: 9
- Reviewer disposition: **approve**. Machine gate: 0 in-diff bugs. Manual read of `rejectLocationSchemeTemplate`: a null template is omitted; a non-positive template is HTTP 400 before any setter or `saveScheme`; a taken context/template/content-type triple is HTTP 409 and excludes the scheme being updated. `applySchemeParameters` leaves stored parameters when the list is null. The client body is template id only, so omitted name, generator, description, content type, and parameters stay. Java tests cover preserve, self-assignment, 400, 403, and 409. Vitest and the Design Playwright spec are present. Product doc updated. No rule-file diffs. The `updateScheme` cognitive-complexity row is preexisting. Ollama `dev-coder` failed with CUDA out of memory; that warn is not a product defect.
- Recommendation: **approve**. May merge: yes.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1376 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateScheme` cognitive=17 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
