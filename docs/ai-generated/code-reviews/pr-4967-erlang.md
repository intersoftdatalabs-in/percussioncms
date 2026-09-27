<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 4967

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Erlang (host)

Machine gate: 0 in-diff bugs. Host override: **request-changes**.

### Bug — recycle prompts after the item is already deleted

- File: `WebUI/src/main/ts/editor/EditorHost.tsx:2275`
- `handleRecycle` confirms recycle, `await recycleItem(...)`, sets recycle done, and only then calls `allowLeave()`.
- Cancel on the unsaved-edits dialog is specified to stay with the draft, but the item is already recycled. The operator cannot keep the item, and the editor remains open on a deleted id.
- Confirm-discard must run **before** `recycleItem` (and before the recycle confirm is the safer order). Do not navigate-only after a successful delete.
- `switchOpenItem` after a successful New item / Copy has the same late prompt (create/copy already committed). Fix those the same way if the draft should block the action.

Recommendation: do not merge.
