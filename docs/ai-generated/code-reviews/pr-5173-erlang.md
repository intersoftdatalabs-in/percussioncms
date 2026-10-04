<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5173

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5173
- Base: origin/main (`8ef0d21a6e47b6a6c47c14a5181ff2a7550edaec`)
- Head: 66c8a0c6a3fbf26ef707c1e7cba56fb54902e14c
- Reviewed: published PR head (worktree `.kilo/worktrees/pr-5173-erlang`)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
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

Approve. Independent of the author. Twelve in-diff files, zero machine bugs. The Ollama stage is a CUDA out-of-memory suggestion and does not block.

`planSetFolderLocaleMulti` writes folder ids once and records pages, assets, and other non-folders as skipped. `saveOneFolderLocale` calls `onFolderSaved` only after `saveSetFolderLocale` returns `saved` (post, then properties reload matches the code). `describeSetFolderLocaleMultiSave` reports success only when every target folder saved. Partial, failed, and unchanged are not full success. Callers that still use `classifySetFolderLocaleSelection` keep the single-item multi block. The shell uses the multi catalog only when two or more rows are checked. Product docs and the Playwright spec are in the diff. No rule-file edits. No new filesystem path joins.
