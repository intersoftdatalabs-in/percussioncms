<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5149

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5149
- Branch: fix/issue-5133-explorer-multi-community
- Issue: #5133
- Base: origin/main
- Head: 53429f100e3215f4f5c1dda2a89c68f5308675f0
- Reviewer: independent Erlang pass (not the author). Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 2 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/contentExplorer/setItemCommunity.ts:197 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `planSetCommunityMulti` cognitive=29 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/contentExplorer/setItemCommunity.ts:324 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveSetCommunityOnSelection` cognitive=23 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open


## Interpreter

Independent read of the diff (not the author). Two or more checked rows save one community onto each page and asset. The name is painted from onItemSaved only after that item's change returns. Folders are named and not posted. HTTP 400/403/409 and a partial failure do not use the full-success notice. Cancel on the single-item dialog does not POST. Fewer than two checks still uses the #5077 catalog. Companions are present: Vitest for the plan, the save, the dialog, and the list; surface Playwright; product-docs content-explorer. No new filesystem path joins. No agent rule files. Cognitive complexity on planSetCommunityMulti and saveSetCommunityOnSelection is a suggestion, not a behavior bug. Ollama dev-coder failed to load (CUDA out of memory); machine findings kept. In-diff bugs: 0. Recommendation: approve. May commit/push: yes.
