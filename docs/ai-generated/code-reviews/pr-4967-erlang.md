<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 4967

Re-review of head `b08dfaf6e79f0b6d51e9af2271b57d14646f0c56` (after the recycle/copy/create prompt fix). CLI: `mkd-code-review` 0.1.18, `--pack percussion --gate advisory --git-base origin/main`.

## Summary

Machine analysis found **1** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1865
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: The `allowLeave` function is called before the `recycleItem`, `copyItem`, and `createItem` functions, which can lead to unintended behavior if these functions modify the state in a way that affects the unsaved edits check.
- Suggestion: Ensure that the `allowLeave` function is called after any state modifications made by `recycleItem`, `copyItem`, and `createItem`. This will ensure that the unsaved edits check is accurate.
- Status: open

## Erlang (re-review)

Machine issue 1 is a **false positive**. `allowLeave()` before `recycleItem` (`EditorHost.tsx:2269`), `copyItem` (`:1888`), and `createItem` (`:2355`) is the correct order: Cancel must not delete, copy, or create. Calling the prompt after those calls would reintroduce the bug fixed in `160e939fe9`.

Host gate: **request-changes**. One in-diff bug remains. Do not merge.

### Bug — confirming discard does not drop a pending file or clear

- File: `WebUI/src/main/ts/editor/EditorHost.tsx:1919` (`allowLeave`), `:1945` (`handleModeChange`), `:873` / `:944` (field-load effect)
- `editorDraftIsDirty` (`editorPreview.ts:83`) treats `pendingFiles` and `pendingClears` as unsaved edits. Confirm on Edit/View or another item only calls `allowLeave()` then `setSearchParams`.
- The load effect depends on `contentId` and `readOnly`, so a mode change or item switch refetches fields and `setDraft`s server values (`:873`). It never calls `setPendingFiles({})` or `setPendingClears({})`. Those clear only after a successful save (`:1318`) or, for files only, after restore (`:2699`).
- Confirm on View therefore keeps the picked file name. `editorDraftIsDirty` stays true, and a later Save uploads or clears that binary. The dialog says the edits will be discarded. The same stale binary can ride onto a switched item that shares the field name.
- `EditorHost.leaveDirty.test.tsx:185` covers cancel-keeps-the-file. `:125` covers confirm-switches-mode-without-PUT. Neither asserts that confirm clears the pending file or clear.
- Clear `pendingFiles` and `pendingClears` on the confirmed leave path (mode change and same-instance item switch) before navigation, and assert the file name is gone and a following save does not upload it.

Recommendation: do not merge.
