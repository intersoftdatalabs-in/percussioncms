<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5441

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5441
- Base: origin/main
- Head: 722b1d69ee295e9a43f02925bccda9e21d2b0e39
- Files analyzed: 9
- Reviewer: independent Erlang (did not author the PR)

## Independent reading

`slotLabelWrite` sends `{ label }` only. The same trimmed label is `"unchanged"` and does not PUT. Cancel does not write. `savedSlotLabel` rejects a response whose name, description, type, finder, relationship, or finder arguments differ from the previous slot, and it rejects a sent body that includes those fields. A blank label does not send the name. `SlotsAdaptor.applyMutableSlotUpdates` writes a label only when `body.getLabel()` is non-null and leaves omitted fields alone. A stored blank label is echoed as the slot name by `labelOrName`; the client accepts `""` or that name and does not treat any other label as success. HTTP 400, 403, and 409 restore the previous label and do not show **Slot label saved**. Vitest covers the write, the echo, and the rejection cases. Playwright and `product-docs/8.2/admin/developer-slots.md` are present. No path or file I/O. No rule files.

In-diff cognitive-complexity rows are suggestions (including `savedSlotLabel`). They are not bugs. The `handleSave` `nameInvalid` branch sits after `canSave`, which already requires `!nameInvalid`. It does not change save behavior. The LLM stage failed open (Ollama CUDA out of memory). Neither blocks.

Recommendation: approve. May merge: yes.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **5** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- In-diff: 3 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/SlotDetailPanel.tsx:479 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleDescriptionSave` cognitive=6 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/SlotDetailPanel.tsx:574 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleLabelSave` cognitive=6 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/SlotDetailPanel.tsx:691 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=14 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/slotLabel.ts:91 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `savedSlotLabel` cognitive=26 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
