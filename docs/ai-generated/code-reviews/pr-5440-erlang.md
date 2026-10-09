<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5440

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5440
- Base: origin/main
- Head: d26475964d3fcca9fc7acd9563cef62e9e4d6aeb
- Files analyzed: 9
- Reviewer: independent Erlang (did not author the PR)

## Independent reading

`tableFieldContainsNul` rejects a raw U+0000 and a cell NUL that only appears after `JSON.parse` (`JSON.stringify` does not leave a raw NUL in the wire string). `handleSave` still runs the required-field gate first and returns before the table-NUL gate, so an empty required table stays **This field is required.** A NUL cell is not empty, so it takes the table-cell message and does not PUT. Close then Cancel does not write. HTTP 400 is not success. `aria-invalid` / `aria-required` sit on the cell inputs; `focusInvalidEditorField` focuses the first input in the row. Vitest covers the gate and the save, cancel, required, and 400 paths. Playwright and `product-docs/8.2/admin/content-explorer.md` are present. No path or file I/O. No rule files.

The `handleSave` cognitive-complexity row is preexisting and not in the diff. The LLM stage failed open (Ollama CUDA out of memory). Neither blocks.

Column titles are not edited by `TableFieldWidget` and are not part of this cell gate.

Recommendation: approve. May merge: yes.

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

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1380 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=142 (max 15), cyclomatic=176 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
