<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5127

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Tool: mkd-code-review 0.1.18 (`analyze --pack percussion --format markdown --gate advisory --git-base origin/main`)
- Models: `/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5127
- Head: 9089fa98966ed284ec0079c3580d64398fd43c0e
- Reviewer is independent of the implementer.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 21 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowGraphProjector.java:95 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `collect` cognitive=21 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Erlang gate

In-diff blocking bugs: 0. The `collect` complexity row is preexisting and is not a bug. Ollama `dev-coder` failed on CUDA OOM; machine findings kept. That warn does not block.

Manual pass: absolute aging create is minutes (`IPSAgingTransition.setInterval`), positive interval only, duplicate from/to/interval is 409, packaged and default workflows are 403, and the graph lists the edge only after save. Cancel, blank destination, and non-positive minutes do not claim success. Rest adaptor, sitemanage writer, Spring test stub, resource tests, Vitest, product-docs, and Playwright are present. No new non-portable path joins. No agent rule files in the diff.

Recommendation: approve.
