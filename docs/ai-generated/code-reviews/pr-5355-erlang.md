<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5355

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 5bb99f001407645bb478d8e9d213eee8f95c4362
- Branch: fix/issue-5345-editor-readonly-field
- Recommendation: approve (in-diff bugs: 0)

## Pre-push local code review

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

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1370 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=138 (max 15), cyclomatic=172 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}
- Status: open

## Erlang intent

Intent: edit mode builds the fields PUT with `fieldsForEditorSave`, which omits schema `readOnly` rows and still omits file and image widgets. `setField`, `setFile`, and `markBinaryClear` ignore schema read-only names. The control shows the stored value, Clear stays hidden, and a client edit is not an unsaved change. Cancel does not call save. HTTP 400, 403, and 409 stay on the error path and do not mark the item saved. Server `applyUpdates` writes only fields present on the PUT, so an omitted read-only field keeps the stored value. The `handleSave` cognitive-complexity row is preexisting, not in-diff, and does not block. Companions present: `editorReadOnlySave` Vitest, `EditorHost.readOnlyField` test, preview dirty-check test, Playwright `editor-host-readonly-field.spec.js`, and `product-docs/8.2/admin/content-explorer.md`. No new filesystem path joins. No agent rule files. Ollama `dev-coder` CUDA OOM is non-blocking; machine findings kept.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
