<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5374

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5374
- Base: origin/main (26082e3cf9b54fc1966b90a3c16e9c32fc0f71df)
- Head: e157f09838261a953ab8c4c8145fe31f058f982b
- Files analyzed: 12

## Verdict

Recommendation: **approve**. Blocking bugs: 0. May merge: yes.

Independent read of `e157f09838`: `updateParameterType` on the existing scheme PUT replaces one stored parameter type. The server keeps that parameter's stored name, value, and sequence, ignores a value or sequence on the request, and does not replace the other parameters. A blank type is HTTP 400 and writes nothing. A name that is not stored is HTTP 409. Add, remove, or a value change combined with a type change is HTTP 400 before any field is written. A blank stored value is HTTP 400 so the update does not invent a value (`addParameter` requires a non-null value); nothing is written. The Design form omits scheme identity fields, updates the list only after success, and Cancel does not call the update. Sitemanage tests, Jackson binding, Vitest, product docs, and the H2 Playwright spec match that contract. No filesystem path joins. No agent rule files.

Suggestions, not blockers: `prepareSchemeParameterTypeChange` cognitive=20 and `applySchemeParameters` cognitive=16, both under the bug line (2× the pack max of 15). `llm.error` is Ollama CUDA out of memory, not a product defect.

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

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2120 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `prepareSchemeParameterTypeChange` cognitive=20 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2182 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applySchemeParameters` cognitive=16 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

