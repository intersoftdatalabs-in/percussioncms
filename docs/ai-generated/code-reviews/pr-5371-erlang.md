<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5371

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5371
- Base: origin/main (5e73d91c6501f839e4ed326601733d97d3af5d58)
- Head: 87e9b9176ba6219a9a71662d299c07c1cb57a4e4
- Files analyzed: 7

## Verdict

Recommendation: **approve**. Blocking bugs: 0. May merge: yes.

Independent read of `wholeNumberText`, `invalidChangedNumberFieldNames`, and the assembly save path:

- A whole number is written through the existing item field PUT with `dataType` `integer`. Other fields on the payload stay. Float and read-only numbers are not overlay fields.
- A decimal, non-numeric value, or blank change is named, the previous number is restored, and the server is not called. An unchanged blank stays out of that list. Close without save does not write.
- HTTP 400, 403, and 409 use the existing failed-save restore. Vitest and the H2 Playwright spec cover those paths. The Active Assembly section of `product-docs/8.2/admin/content-explorer.md` now states the whole-number behavior. No filesystem path joins. No agent rule files.
- In-diff complexity findings: 0. Preexisting `handleSaveFields` and `applyFieldOverlay` suggestions do not gate. `llm.error` is CUDA OOM, not a product defect.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 0 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:614 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=24 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/assembly/overlayFields.ts:590 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldOverlay` cognitive=24 (max 15), cyclomatic=8 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

