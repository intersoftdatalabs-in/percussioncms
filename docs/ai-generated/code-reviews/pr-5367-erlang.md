<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5367

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5367
- Base: origin/main (f3a792660c7201d1c2a61f143643d8485f9a366f)
- Head: 7915e847f37097ee780a4408940718a6d5428d19
- Files analyzed: 8

## Verdict

Recommendation: **approve**. Blocking bugs: 0. May merge: yes.

Independent read of `keywordUpdateForRevaluedChoice` and `KeywordEditorPanel`:

- A blank draft is stored as the choice label (same rule as add). A blank draft whose effective value is already that label is `unchanged` and is not a PUT. An empty label and an empty value return `blank`.
- Duplicate detection uses `effectiveValue` (trimmed value, or the label when the value is blank) and ignores case. The other choice is not replaced. The same choice's own value, compared as an exact trimmed string, is not a write. A case-only change of that choice's own value is a write, matching the relabel peer.
- The update body copies keyword label, description, and sequence from the loaded keyword and keeps the edited choice's label, description, and sequence. Other choices are copied.
- `savedChoicesAfterAdd` refuses a 200 whose metadata or choice multiset does not match the sent body, and the panel restores the previous list. HTTP 400, 403, and 409 restore the previous value and do not apply choices from the error body.
- While the value editor is open, add, remove, label edit, keyword save, and delete stay disabled. Cancel does not call `updateKeyword`.
- Unit tests and the H2 Playwright spec cover duplicate, blank-as-label, cancel, success reload, the blank value that would collide after High is set to `low`, and HTTP 400/403/409. Add, relabel, remove, and delete remain on the same resource. Product docs in `product-docs/8.2/developer/rest.md` match. No path or file I/O. No agent rule files.

In-diff `handleChoiceValueSave` cyclomatic complexity is a suggestion (same shape as `handleChoiceLabelSave`), not a bug. Preexisting complexity on add, remove, and label save does not gate. Ollama `ollama-dev-coder` failed to load (CUDA out of memory). Machine findings stand.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **5** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 1 finding(s); preexisting: 3
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:286 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleAddChoice` cognitive=8 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:378 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleRemoveChoice` cognitive=7 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:466 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceLabelSave` cognitive=10 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/KeywordEditorPanel.tsx:578 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleChoiceValueSave` cognitive=10 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
