<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 4967

Re-review of head `ac69171f108d82b93f621b9616b2ed0a84e4926a` (discarded binary picks cleared before a later save). CLI: `mkd-code-review` 0.1.18, `--pack percussion --format markdown --gate advisory --git-base origin/main --models models.ollama-dev-coder.toml`.

## Summary

Machine analysis found **1** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
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

Machine issue 1 is a **false positive**. `EditorHost.tsx:1865` is `copyErrorKeyFor`, not a leave check. `allowLeave()` before `recycleItem` (`handleRecycle`), `copyItem` (`handleCopy`), and `createItem` (`handleCreate`) is the correct order: Cancel must not delete, copy, or create. Calling the prompt after those calls would reintroduce the bug fixed in `160e939fe9`.

The prior blocking bug is fixed. `allowLeave` (`EditorHost.tsx:1938`) calls `discardUnsavedEdits` (`:1925`) only after confirm, which clears `pendingFiles` and `pendingClears`, resets `draft` from the loaded payload, and bumps `discardEpoch` so file widgets remount. The field-load effect (`:818`) also clears both maps when `contentId` or `readOnly` changes, so a confirmed mode change or item switch cannot leave a binary for a later save. `EditorHost.leaveDirty.test.tsx` covers confirm-on-mode-change for a pending upload and a pending clear; a following save does not call `uploadBinary` or `clearBinary`.

Host gate: **approve**. No in-diff bug remains. Do not merge until required checks on this head are green.

Recommendation: approve.
