<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5446

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5446
- Base: origin/main
- Head: 12f2f1d1f1e10a05d97b41c2d5837041bb26152e
- Files analyzed: 8
- Reviewer: independent Erlang (did not author the PR)

## Independent reading

`nulLinkFieldNames` names link fields whose current value contains a NUL and does not name HTML, text, or long text. `handleSaveFields` returns before `persistOverlayEdits` when a link NUL is present, shows **That link contains a character that cannot be saved.**, and does not show **Fields saved**. An HTML NUL still uses the HTML message. Cancel restores the previous link and does not write. A content id, a hyphenated GUID, and a folder path still save with `dataType: link`. HTTP 400, 403, and 409 use the existing save failure path: the previous overlay value is restored and the notice is not **Fields saved**. Vitest covers the refusal, cancel, the three successful shapes, and the HTML gate. Playwright and `product-docs/8.2/admin/content-explorer.md` are present. No filesystem path joins. No rule files.

`handleSaveFields` cognitive complexity is preexisting and is not an in-diff bug. The in-diff row is a complexity suggestion on the Playwright route helper. The LLM stage failed open (Ollama CUDA out of memory). Neither blocks.

Recommendation: approve. May merge: yes.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:632 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=52 (max 15), cyclomatic=49 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/assembly-link-nul-field.spec.js:78 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installAssemblyRoutes` cognitive=19 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

