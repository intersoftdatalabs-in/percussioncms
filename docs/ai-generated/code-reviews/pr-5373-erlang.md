<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5373

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5373
- Base: origin/main (26082e3cf9b54fc1966b90a3c16e9c32fc0f71df)
- Head: a6509060131cc862833b3c3e3fce4d7e678ee582
- Files analyzed: 8

## Verdict

Recommendation: **approve**. Blocking bugs: 0. May merge: yes.

Independent read of `a650906013`: Active Assembly saves one calendar date (`yyyy-MM-dd`, `dataType` date) through the existing item field PUT. `calendarDateText` accepts only a widget date, so a blank value, a datetime, and an invalid day are not written. A blank change restores the previous date and does not call the server. Cancel restores the painted values and does not save. Read-only rows are omitted before the overlay is built. HTTP failures restore the pre-save overlay and do not show **Fields saved**. Datetime still classifies before date, so it stays off this overlay. Other changed fields are the only edits sent. Vitest covers the refusal and error paths. Playwright `assembly-date-field.spec.js` and `product-docs/8.2/admin/content-explorer.md` match that contract. No new filesystem path joins. No agent rule files.

Suggestions, not blockers: preexisting cognitive complexity on `handleSaveFields`, `applyFieldOverlay`, and `readNodeValue`. `llm.error` is Ollama CUDA out of memory, not a product defect.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **4** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 0 finding(s); preexisting: 3
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:616 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=28 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/assembly/overlayFields.ts:679 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldOverlay` cognitive=26 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/assembly/overlayFields.ts:754 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `readNodeValue` cognitive=17 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

