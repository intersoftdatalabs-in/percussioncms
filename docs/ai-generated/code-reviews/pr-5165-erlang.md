<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5165

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: e533d276e7acd10ec3c8da279467b2dae66e8b61
- Branch: fix/issue-5155-set-workflow-multi
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
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

- File: WebUI/src/main/ts/contentExplorer/setItemWorkflow.ts:198 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `planSetWorkflowMulti` cognitive=29 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/contentExplorer/setItemWorkflow.ts:325 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveSetWorkflowOnSelection` cognitive=23 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Agent note

Independent Erlang review of PR #5165 (not the author). Machine gate: 0 in-diff bugs. Recommendation: approve. May merge: yes.

Content → Set workflow with two or more checked rows writes one workflow onto each page and asset through the existing change-workflow POST. Folders are named and not posted. A row name is painted only from `onItemSaved`, which runs after that item's change returns. Cancel does not POST. HTTP 400/403/409 on any item is partial or failed, not full success. A single checked row still uses the #5076 dialog. `classifySetWorkflowSelection` still returns `multi` for callers that have not opted into the multi plan. Vitest, Playwright, and `product-docs/8.2/admin/content-explorer.md` match that behavior. No rule-file diff. No new filesystem path joins.

Cognitive complexity on `planSetWorkflowMulti` and `saveSetWorkflowOnSelection` is a suggestion, not a behavior defect. Tests cover empty, folder-only, partial HTTP failure, cancel, and single-item refuse.

LLM stage: `ollama-dev-coder` CUDA out of memory (HTTP 500). Machine findings kept. Not a blocking finding.
