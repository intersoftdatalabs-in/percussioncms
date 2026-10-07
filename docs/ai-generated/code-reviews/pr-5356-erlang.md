<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5356

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 4dc343ff16b7a08732c6921e3375a4cb437e4c48
- Branch: fix/issue-5348-keyword-choice-label
- Recommendation: approve (in-diff bugs: 0)

## Pre-push local code review

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:261 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleAddChoice` cognitive=8 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:435 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceLabelSave` cognitive=10 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}
- Status: open

## Erlang intent

Intent: `keywordUpdateForRelabeledChoice` sends the existing keyword PUT with keyword label, description, and sequence copied from the loaded keyword. The edited choice keeps its value, description, and sequence. Other choices stay. A blank label, the same label, a missing index, and a label that matches another choice ignoring case do not call update. The list shows the new label only after `savedChoicesAfterAdd` accepts a response with the same keyword fields and the sent choices. HTTP 400, 403, and 409 restore the previous choices and do not apply the error body. Cancel does not write. `handleChoiceLabelSave` cyclomatic complexity is a suggestion, not a behavior bug. `handleAddChoice` complexity is preexisting. Companions present: helper Vitest, `KeywordEditorPanel` tests, Playwright `developer-keywords-change-choice-label.spec.js`, and `product-docs/8.2/developer/index.md` plus `rest.md`. No new filesystem path joins. No agent rule files. Ollama `dev-coder` CUDA OOM is non-blocking; machine findings kept.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
