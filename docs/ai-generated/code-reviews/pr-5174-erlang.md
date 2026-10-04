<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5174

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5174
- Base: origin/main (`8ef0d21a6e47b6a6c47c14a5181ff2a7550edaec`)
- Head: 8ff2b38ccc599f22442974c7e7aeeb0bab2481c6
- Reviewed: published PR head (worktree `.kilo/worktrees/pr-5174-erlang`)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 17 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

### Erlang judgment (peer)

Approve. Independent of the author. Seventeen in-diff files, zero machine bugs. The Ollama stage is a CUDA out-of-memory suggestion and does not block.

`resolveItemFilterUpdate` runs before create and before `loadContentListModifiable`. A null `itemFilterId` does not call `setFilterId`. Blank clears. Any other value must resolve by uuid (all digits) or name, or the request is 400 and nothing is saved. The editor stores the filter name as the select value and omits `itemFilterId` until the catalog loads, so a failed lookup cannot clear the stored filter. "Saved item filter" and the design row use the last loaded summary, not the draft select. `onSaved` reloads the list. Product docs, sitemanage tests, Vitest, and Playwright are in the diff. No rule-file edits. No new filesystem path joins.

Suggestion, not a gate: an all-digit filter name is still looked up as a uuid first. The editor sends names, and catalog names in this change are not numeric.
