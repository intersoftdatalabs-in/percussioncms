<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5372

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5372
- Base: origin/main (5e73d91c6501f839e4ed326601733d97d3af5d58)
- Head: e98eb187a25201a8f7011d839bc2e4cd83f1b4f5
- Files analyzed: 11

## Verdict

Recommendation: **request-changes**. Blocking bugs: 1. May merge: no.

Independent read of `prepareSchemeParameterValueChange` and `updateScheme`:

- The value path is behaviorally right. Exactly one stored parameter is updated. A blank or null value is HTTP 400 and does not clear the stored value. A name that is not stored is HTTP 409. A request type and sequence are ignored: the stored type, sequence, and parameter id stay, and the other parameter stays. Add or remove combined with a value change is HTTP 400 before load. Identity fields change only when present.
- Vitest, Jackson binding, sitemanage tests, product docs, and the H2 Playwright spec match that contract. No filesystem path joins. No agent rule files.
- **Block:** `updateScheme` is an in-diff `complexity.cognitive` bug. On this head it is cognitive=36, cyclomatic=26. On `origin/main` the same method is cognitive=25, cyclomatic=21 (suggestion; under the pack 2× bug line of 30). This slice crosses that line. `prepareSchemeParameterValueChange` (cognitive=19) and `applySchemeParameters` (cognitive=16) are suggestions. `llm.error` is CUDA OOM, not a product defect.
- Do not merge until `updateScheme` is back at or under the bug line. Extract the new value-change branch (or the surrounding field updates) without changing the value-update behavior.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **4** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 3 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1404 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateScheme` cognitive=36 (max 15), cyclomatic=26 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1982 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `prepareSchemeParameterValueChange` cognitive=19 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2041 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applySchemeParameters` cognitive=16 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

