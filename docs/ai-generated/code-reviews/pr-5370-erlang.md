<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5370

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5370
- Base: origin/main (5e73d91c6501f839e4ed326601733d97d3af5d58)
- Head: 9e5dc59c1c718b8fbdcf445e64e55e5f8c62b16c
- Files analyzed: 7

## Verdict

Recommendation: **approve**. Blocking bugs: 0. May merge: yes.

Independent read of `keywordUpdateForDescribedChoice` and `KeywordEditorPanel`:

- A blank or whitespace description is not a body, so a stored description is not cleared. The same trimmed description is not a write. A missing index is not a write. Descriptions are not required to be unique.
- The PUT copies keyword label, description, and sequence from the loaded keyword. The edited choice keeps its label, value, and sequence. Other choices are copied, including their descriptions.
- `savedChoicesAfterAdd` refuses a 200 whose keyword metadata or choice multiset does not match the sent body. HTTP 400, 403, and 409 restore the previous description and do not apply choices from the error body. Cancel does not call `updateKeyword`.
- Vitest covers the helper and the panel. The H2 Playwright spec is the screen companion. `product-docs/8.2/developer/rest.md` describes this PUT. No filesystem path joins. No agent rule files.
- In-diff complexity on `startChoiceDescriptionEdit` and `handleChoiceDescriptionSave` stays under the 2× bug line (suggestion). Preexisting add, remove, label, and value handlers do not gate. `llm.error` is CUDA OOM, not a product defect.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **9** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 2 finding(s); preexisting: 6
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:326 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleAddChoice` cognitive=8 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:420 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleRemoveChoice` cognitive=7 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:487 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceLabelEdit` cognitive=3 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:512 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceLabelSave` cognitive=10 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:602 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceValueEdit` cognitive=3 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:628 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceValueSave` cognitive=10 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:718 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceDescriptionEdit` cognitive=3 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:744 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceDescriptionSave` cognitive=9 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

