<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Erlang review PR 5046

Status: cli report captured. Independent re-review after erlang-fix (not the author).
Persona: erlang 0.1.1. Persona source: ~/.local/share/mkd/agents/erlang.
CLI: mkd-code-review 0.1.18, pack percussion, gate advisory, base origin/main.
Head reviewed: 3b0c7d9167fa4206cbb1bd739164f5a1bff4acfb.
Ollama dev-coder failed with CUDA OOM; machine findings kept. Not a merge block.

## Erlang interpretation

Recommendation: **approve**. In-diff blocking bugs: 0.

The earlier persist-then-reject bug is fixed on this head. `addOwned` calls `deletePersisted` when `createRelationship` returns a folder or Active Assembly row, and again when the follow-up `saveRelationships` throws. Tests `addDeletesPersistedFolderReturnedByCreate`, `addDeletesPersistedActiveAssemblyReturnedByCreate`, and `addSaveFailureIsConflict` assert the delete. The UI sets "Relationship added" only after `addEdge` resolves, and a non-positive id is thrown as a failure. Adaptor maps non-CREATED statuses to 400/403/404/409.

Non-blocking: `addOwned` is over the cognitive complexity cap (suggestion). `deletePersisted` swallows a second delete failure so the API still returns conflict rather than success; that does not claim the row was added.


## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 19 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/share/relationship/service/impl/PSExplorerRelationshipRemoveService.java:145 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `addOwned` cognitive=19 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

