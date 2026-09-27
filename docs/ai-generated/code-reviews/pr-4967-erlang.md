<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 4967

Re-review after erlang-fix. Head `160e939fe9f7311b5428e3726e0eac17b3b8a24b`.

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Erlang (re-review)

Machine gate: 0 in-diff bugs. Prior host finding (leave prompt after recycle/copy/create) is **fixed** on this head: `allowLeave()` runs before `recycleItem`, `copyItem`, and `createItem`.

Host override: **request-changes**. One in-diff bug remains.

### Bug — confirming discard does not drop a pending file or clear

- File: `WebUI/src/main/ts/editor/EditorHost.tsx` (`allowLeave` / `handleModeChange` / field-load effect)
- `editorDraftIsDirty` treats `pendingFiles` and `pendingClears` as unsaved edits, and the new Edit/View control calls `allowLeave()` before `setSearchParams`.
- The load effect depends on `readOnly`, so a mode change refetches fields and `setDraft`s server values, but it never calls `setPendingFiles({})` or `setPendingClears({})` (those clear only after a successful save, or pending files only after restore).
- Confirm on View therefore keeps the picked file name, `editorDraftIsDirty` stays true, and a later Save uploads or clears that binary. The dialog says the edits will be discarded.
- `EditorHost.leaveDirty.test.tsx` covers cancel-keeps-the-file and confirm-switches-mode-without-PUT. It does not assert that confirm clears the pending file.
- Clear `pendingFiles` and `pendingClears` on the confirmed leave path (mode change and any same-instance item switch) before navigation, and assert the file name is gone and a following save does not upload it.

Recommendation: do not merge.
