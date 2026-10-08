<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5397

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5397
- Base: origin/main
- Head: e1308a7f2ab099173286164dbdbc2d7595e235f5
- Branch: fix/issue-5380-keyword-sequence
- Files analyzed: 7
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass. Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **19** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 3 finding(s); preexisting: 15
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:424 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=6 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:473 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleAddChoice` cognitive=8 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:594 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleClearChoiceDescription` cognitive=8 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:664 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleRemoveChoice` cognitive=7 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:733 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceLabelEdit` cognitive=3 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:761 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceLabelSave` cognitive=10 (max 15), cyclomatic=29 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:853 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceValueEdit` cognitive=3 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:882 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceValueSave` cognitive=10 (max 15), cyclomatic=29 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:980 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceDescriptionEdit` cognitive=3 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1009 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceDescriptionSave` cognitive=9 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 11 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1096 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceSequenceEdit` cognitive=3 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 12 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1126 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceSequenceSave` cognitive=10 (max 15), cyclomatic=29 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 13 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1219 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startKeywordDescriptionEdit` cognitive=2 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 14 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1244 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleKeywordDescriptionSave` cognitive=6 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 15 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1344 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleKeywordLabelSave` cognitive=6 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 16 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1423 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startKeywordSequenceEdit` cognitive=2 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 17 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1450 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleKeywordSequenceSave` cognitive=9 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 18 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/keywordChoiceAdd.ts:806 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `savedKeywordSequence` cognitive=17 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 19 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 1879048192\nllama_init_from_model: failed to initialize the context: failed to allocate buffer for kv cache","type":"api_error","param":null,"code":null}}

- Status: open


## Interpreter

Independent read of the diff (not the author). Keyword sequence save uses the existing keyword PUT, copies label and description, and omits choices. The shown sequence and the form sequence change only after savedKeywordSequence accepts a response that keeps the label, description, and choice list. Blank, non-numeric, above Integer.MAX_VALUE, unchanged, and Cancel do not call updateKeyword. HTTP 400, 403, and 409 restore the previous sequence and do not apply a choice list from the error body. The main Save control stays disabled while the sequence editor is open. Companions present: Vitest for the panel and the body helper, surface Playwright, and product-docs/8.2/developer/rest.md. No new filesystem path joins. No agent rule files. In-diff complexity on the new guards is a suggestion, not a bug. Preexisting cyclomatic rows do not block. The local coder model ran out of CUDA memory; that warning is not an in-diff bug. Machine findings kept. Recommendation: approve. May commit/push: yes.
