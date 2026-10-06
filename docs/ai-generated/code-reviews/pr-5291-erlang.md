<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5291

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Tool: mkd-code-review 0.1.18 (`analyze --pack percussion --format markdown --gate advisory --git-base origin/main`)
- Models: `/home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5291
- Head: 1069fbe3d3c15731a3ee307e4604950da6ee75ef
- Reviewer is independent of the implementer.
- Ollama `dev-coder` returned HTTP 500 (CUDA out of memory). Machine findings kept. Not a merge block.

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
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 1879048192\nllama_init_from_model: failed to initialize the context: failed to allocate buffer for kv cache","type":"api_error","param":null,"code":null}}

- Status: open

## Independent reading

`type` omitted or `ABSOLUTE` still changes one absolute aging interval. `REPEATED` changes only the repeated edge: an absolute twin that shares from, to, and the current minutes stays, and an absolute edge already at the new minutes is not a 409. `SYSTEM_FIELD` and any other type are HTTP 400. The graph sends `type: REPEATED` only for a repeated row, and system-field rows have no Change interval control. Cancel, blank, unchanged, and non-positive values do not write. REST, sitemanage, Vitest, the H2 Playwright spec, and `product-docs/8.2` are in the diff. No blocking bug.
