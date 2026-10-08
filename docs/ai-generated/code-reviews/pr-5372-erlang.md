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
- Base: origin/main (e11ffdff2e5552a527d83565445831bba2b57c9d)
- Head: 8142d5f027296aea0a75b2bdb49076cece21b992
- Files analyzed: 13

## Verdict

Recommendation: **approve**. Blocking bugs: 0. May merge: yes.

Re-review after `8142d5f027` extracted scheme-parameter mode checks and apply out of `updateScheme`. That was the only in-diff bug on the previous head (`updateScheme` cognitive=36, over the pack 2× line of 30). It is gone.

Independent read: the extraction preserves the value-one contract. Exactly one stored parameter is updated. A blank or null value is HTTP 400 and does not clear the stored value. A name that is not stored is HTTP 409. Request type and sequence are ignored, so the stored type, sequence, and parameter id stay, and the other parameter stays. Add or remove combined with a value change is HTTP 400 before load. Identity fields change only when present. Sitemanage tests, Jackson binding, Vitest, product docs, and the H2 Playwright spec still match that contract. No filesystem path joins. No agent rule files.

Suggestions, not blockers: `prepareSchemeParameterValueChange` cognitive=19 and `applySchemeParameters` cognitive=16, both under the bug line. `llm.error` is Ollama CUDA out of memory, not a product defect.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 13 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2041 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `prepareSchemeParameterValueChange` cognitive=19 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:2100 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applySchemeParameters` cognitive=16 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open
