<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5113

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: a3617f2b39fa2a183d1fa140bca2298e749b128c
- Branch: fix/issue-5102-developer-update-role-description
- Recommendation: approve (in-diff bugs: 0)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 19 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/RoleAdaptor.java:279 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `browseRoles` cognitive=28 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Erlang gate (reviewer)

Machine gate: 19 files, in-diff findings 0, blocking bugs 0. `browseRoles` cognitive complexity is preexisting. Ollama `dev-coder` CUDA OOM is not a defect in this diff.

Recommendation: **approve**.

Checked by hand: `updateRole` copies stored users and homepage, does not set `oldName`, 404s a missing role, 400s a description over 255 characters, and 403s a non-admin. `PUT` with both `create=true` and `update=true` is 400. Blank description becomes null. Catalog overlay reads the backend role when the design summary has no description. `IRoleAdaptor.updateRole` signature is unchanged; `RoleTestAdaptor` still implements it. Product docs, Vitest, REST/sitemanage tests, and the Playwright surface spec are in the diff. No rule-file changes. No new filesystem path joins.

Suggestion (not blocking): `RoleAdaptor.storedRoleDescription` calls `roleService.find` for every catalog row whose summary description is blank. Design summaries do not carry descriptions, so browse is one lookup per role. A failed lookup keeps the row and drops the description (tested). Not a functional defect on the QA catalog.
