<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5354

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 97afeabb6dd5aab13340071dd2a351c5893786df
- Branch: fix/issue-5333-location-scheme-remove-parameter
- Recommendation: approve (in-diff bugs: 0)

## Pre-push local code review

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1391 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateScheme` cognitive=25 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1973 (in-diff)
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

## Erlang intent

Intent: `updateScheme` prepares a single `removeParameter` name before any field write. A blank name, a name longer than the column, or a count other than one is HTTP 400 and does not save. A name that is not stored, or a duplicate scheme name sent with the update, is HTTP 409 and does not call `removeParameter`. Add and remove together are HTTP 400 before load. The last parameter leaves an empty list and does not delete the scheme. The UI confirms through the existing scheme update, keeps the other parameters and scheme identity, and leaves the previous list in place on cancel and on HTTP 400, 403, and 409. `updateScheme` and `applySchemeParameters` cognitive complexity are suggestions; the remove path is a guarded branch, and `applySchemeParameters` is not the remove path. No new filesystem path joins. Companions present: `locationSchemeRemoveParameter.ts` plus Vitest, `ContextsPanel` tests, `PSPublishingDesignRestServiceTest`, Jackson bind test, Playwright `designLocationSchemeRemoveParameter.spec.js`, and `product-docs/8.2/admin/publishing.md`. No agent rule files. Ollama `dev-coder` CUDA OOM is non-blocking; machine findings kept.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
