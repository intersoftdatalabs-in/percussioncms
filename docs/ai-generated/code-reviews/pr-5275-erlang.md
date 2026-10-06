<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5275

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5275
- Issue: #5268
- Base: origin/main (45af15689b787314ae00c58ac7880047e8c729d3)
- Head: 3219cb6015ec6f72c6ebe1a038fcf3f714014b75
- Files analyzed: 9
- Recommendation: approve
- In-diff blocking bugs: 0

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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

- File: WebUI/src/main/ts/publishing/design/DeliveryTypesPanel.tsx:65 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `DeliveryTypesPanel` cognitive=44 (max 15), cyclomatic=45 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:679 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateDeliveryType` cognitive=20 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open


## Erlang interpretation

Independent read of `updateDeliveryType` and the bean form. A null `beanName` is still omitted, so a name-only or description-only update does not clear the stored bean. A blank or overlong bean is rejected before name, bean, or description is written. The panel does not replace the list when client validation fails or the PUT fails. Preexisting cognitive-complexity rows on `DeliveryTypesPanel` and `updateDeliveryType` are outside this diff and do not block. Vitest, the service test, Playwright, and `product-docs/8.2/admin/publishing.md` are present. Ollama failed to load (CUDA out of memory); machine findings were kept.

> Co-Authored by Grok Build 1.0.46 using grok-4.6 with agent night-issue-prs-erlang.
