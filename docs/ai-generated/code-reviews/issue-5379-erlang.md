## Summary

Machine analysis found **17** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 5 analyzed
- In-diff: 2 finding(s); preexisting: 14
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:393 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=6 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:442 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleAddChoice` cognitive=8 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:563 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleClearChoiceDescription` cognitive=8 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:633 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleRemoveChoice` cognitive=7 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:702 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceLabelEdit` cognitive=3 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:729 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceLabelSave` cognitive=10 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:821 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceValueEdit` cognitive=3 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:849 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceValueSave` cognitive=10 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:947 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceDescriptionEdit` cognitive=3 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:975 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceDescriptionSave` cognitive=9 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 11 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1062 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startChoiceSequenceEdit` cognitive=3 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 12 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1091 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceSequenceSave` cognitive=10 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 13 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1184 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startKeywordDescriptionEdit` cognitive=2 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 14 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1209 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleKeywordDescriptionSave` cognitive=6 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 15 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1282 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startKeywordLabelEdit` cognitive=2 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 16 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:1308 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleKeywordLabelSave` cognitive=6 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 17 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

