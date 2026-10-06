<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5294

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Tool: mkd-code-review 0.1.18 (`analyze --pack percussion --format markdown --gate advisory --git-base origin/main`)
- Models: `/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5294
- Base: origin/main (027798ad7ae0265c1a2d446488e0ae1d12abe672)
- Head: 59e3af11fb14f9e45b257d58afd363667ba81788
- Files analyzed: 18
- Reviewer is independent of the implementer.
- Ollama `dev-coder` returned HTTP 500 (CUDA out of memory). Machine findings kept. Not a merge block.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 18 analyzed
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


## Independent reading

PUT `/workflows/{idOrName}/aging-transitions/system-field` changes one SYSTEM_FIELD date column. The resource maps blank, unknown, and unchanged columns to 400 before the adaptor. `WorkflowsAdaptor` and the rest `TestWorkflowsAdaptor` both implement `changeSystemFieldAging`. `copyAging` is a shallow list copy, so `changeSystemField` mutates the live aging object, then stores that list back. Absolute and repeated rows are not rewritten. A duplicate column on the same from/to is 409 and is not written. Packaged workflows are 403. A missing workflow, step, or edge is 404. The Developer graph calls the PUT only after a different allowed column is chosen and replaces the graph only when that call returns. Adaptor tests, resource tests, Vitest, the H2 Playwright spec, and `product-docs/8.2/admin/developer-workflows.md` plus `product-docs/8.2/developer/rest.md` are in the diff. No rule-file diff and no filesystem path I/O.

Recommendation: **approve**. May commit/push: yes. In-diff blocking bugs: 0.
