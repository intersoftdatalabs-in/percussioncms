<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5398

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5398
- Base: origin/main
- Head: 9709346c14692afbf165fdd0620180258671ddd3
- Branch: fix/issue-5384-assembly-decimal
- Files analyzed: 7
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass. Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **4** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 1 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:623 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=40 (max 15), cyclomatic=37 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/assembly/overlayFields.ts:992 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldOverlay` cognitive=40 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/assembly-decimal-field.spec.js:74 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installAssemblyRoutes` cognitive=21 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 1879048192\nllama_init_from_model: failed to initialize the context: failed to allocate buffer for kv cache","type":"api_error","param":null,"code":null}}

- Status: open


## Interpreter

Independent read of the diff (not the author). A content-type float is edited on Active Assembly and saved through the existing item field PUT with dataType float. 1.5 is accepted; an integer field still rejects that decimal and keeps the previous whole number. Cancel, a non-numeric float, a read-only float, and HTTP 400, 403, and 409 do not claim success and leave the previous value. Blank required numbers stay on the existing required-number gate. Companions present: overlay and AssemblyHost Vitest, surface Playwright, and product-docs/8.2/admin/content-explorer.md. No new filesystem path joins. No agent rule files. The in-diff Playwright helper complexity is a suggestion. Preexisting cognitive rows on handleSaveFields and applyFieldOverlay do not block. The local coder model ran out of CUDA memory; that warning is not an in-diff bug. Machine findings kept. Recommendation: approve. May commit/push: yes.
