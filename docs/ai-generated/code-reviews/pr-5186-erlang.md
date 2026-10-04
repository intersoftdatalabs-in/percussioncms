<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5186

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5186
- Base: origin/main (`d0ee1de858c237987bce64f837bdd9b6887da713`)
- Head: 4ac25c6763da0bd9e410c58c6d255b31158c02c0
- Reviewed: published PR head (`fix/issue-5163-editor-adhoc-assignees`)

Independent read: `transitionWithComments` now forwards parsed `adhocAssignees` into `transitionItem` / approve (comma list in, semicolon list inside `PSWebserviceUtils`). `getTransitions` lists triggers whose destination state `isAdhocEnabled()` (assignee or admin, not reader-only, not disabled). The editor confirms before a required-assignee transition, refuses an empty list, sends a comment-only call immediately when assignees are not required, and updates the state label only after success. HTTP 400/403/409 stay on the open item. Behavioral tests cover the rules, the host, and the H2 Playwright spec. Product doc updated. No new non-portable path joins. The path-separator bug and `getTransitions` complexity row are preexisting and outside the diff. LLM CUDA OOM is not an in-diff bug. Empty assignee lists are still not a server error; the editor gate matches the stated contract and the existing actions panel.

Recommendation: approve. Blocking in-diff bugs: 0.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 18 analyzed
- In-diff: 0 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemWorkflowService.java:991 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 991)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemWorkflowService.java:363 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getTransitions` cognitive=17 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
