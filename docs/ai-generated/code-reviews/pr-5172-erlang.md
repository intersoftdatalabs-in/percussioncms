<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5172

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5172
- Base: origin/main (`8ef0d21a6e47b6a6c47c14a5181ff2a7550edaec`)
- Head: 9d7c8082d28f509a76d678643081c5da419d3db2
- Reviewed: published PR head (worktree `.kilo/worktrees/pr-5172-erlang`)

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 24 analyzed
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

Approve. Independent of the author. Twenty-four in-diff files, zero machine bugs. The Ollama stage is a CUDA out-of-memory suggestion and does not block.

`WorkflowStepRoleAssignmentWriter.setNotify` changes only `PSAssignedRole.setDoNotify` after rejecting a null or unchanged flag. Step rename and role-list size are checked before `saveWorkflow`. Packaged names are 403 and do not save. Missing workflow, step, or role is 404. `WorkflowsResource.mapMutationFailure` maps `IllegalArgumentException` to 400. `implements IWorkflowsAdaptor` is only `WorkflowsAdaptor` and `TestWorkflowsAdaptor`.

The detail table reads stored rows. `setRows` runs only when `applyNotifyAfterReload` sees the requested flag. Cancel does not call `setStepRoleNotify`. Packaged workflows omit the confirm (`canOffer` is false). Product docs, REST/adaptor/Vitest tests, and the Playwright spec are in the diff. No rule-file edits. No new filesystem path joins.
