<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5394

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 6d4d83a2d1582d119e3be70e47c2d8eea548464c
- Branch: fix/issue-5383-assembly-datetime
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **5** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 0 finding(s); preexisting: 4
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
- Description: Function `handleSaveFields` cognitive=38 (max 15), cyclomatic=35 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/assembly/overlayFields.ts:669 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `mapAssembledFieldElements` cognitive=83 (max 15), cyclomatic=38 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: bug

- File: WebUI/src/main/ts/assembly/overlayFields.ts:963 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldOverlay` cognitive=34 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/assembly/overlayFields.ts:1047 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `readNodeValue` cognitive=20 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
