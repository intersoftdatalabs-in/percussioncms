<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5429

Independent review of `fix/issue-5412-assembly-html-nul` (assembly HTML NUL). The author report is not this review. Recommendation: **approve**. In-diff bugs: 0. Preexisting `handleSaveFields` complexity does not block. `readNodeValue` complexity is a suggestion. The Ollama pass failed open (CUDA out of memory) and is not a product defect.

Manual reading: `nulHtmlFieldNames` refuses an HTML NUL before the item field PUT, including a content-editable node whose `innerHTML` drops a NUL text node. Cancel restores the previous markup. Ordinary HTML still saves. The overlay-strip textarea path keeps the raw NUL and does not call save. Single-line and long-text gates are unchanged. Vitest covers those paths. Playwright, product-docs, and no rule-file or path I/O changes. Change-class companions are present.

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

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:629 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=49 (max 15), cyclomatic=46 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/assembly/overlayFields.ts:1206 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `readNodeValue` cognitive=20 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
