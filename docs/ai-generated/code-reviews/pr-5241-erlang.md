<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 5241

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- Base: origin/main (862ba06dbfa063670c0992537332dc10254c3939)
- Head: 4ea2137994f739ad1a06961efce7c2458b99dcab (fix/issue-5233-transition-one-role)
- LLM: ollama dev-coder failed (CUDA out of memory). Machine findings kept. Not a product defect.

## Erlang interpretation

In-diff bugs: 0. `restrictTransitionToOneRole` requires Admin, rejects packaged workflows with 403 before mutation, returns 409 for the allow-all marker and for an already-restricted transition without rewriting its role list, returns 400 for an aging match or an ambiguous label, and returns 404 for an unknown role. A successful call sets allow-all false and exactly one `PSTransitionRole`, then `setTransitions` copies that flag and the role list onto the Hibernate rows. Sibling allow-all, approvals, comment, default, label, destination, and aging are left in place. The graph emits `allowAllRoles` and one `allowedRoles` name; aging edges omit both. A one-element `roles` or `allowedRoles` JSON string is normalized to a one-name list in `parseWorkflowGraph` and `roleNameList`, so the graph does not unmount. Cancel does not PUT. HTTP 400, 403, and 409 do not replace the graph. `TestWorkflowsAdaptor` implements the new interface method. REST resource tests, adaptor tests, Vitest, Playwright, and product-docs are present. No rule-file diff. No new filesystem path joins.

The two REST test files omitted from the first path list were reviewed in a follow-up machine-only pass (2 files, 0 findings).

Non-blocking suggestions: `WorkflowGraphProjector.project` cognitive 28 and `WorkflowTransitionAllowedRoleLimiter.apply` cognitive 20 (in-diff). `collect` cognitive 25 is preexisting. If a regular transition and an aging transition share both label and destination, the call returns 400 even when `to` was supplied, because those query keys cannot tell them apart. That is not a silent write.

Gate: PASS
May commit/push: yes

## Pre-push local code review

## Summary

Machine analysis found **4** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 18 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowGraphProjector.java:47 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `project` cognitive=28 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowGraphProjector.java:157 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `collect` cognitive=25 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowTransitionAllowedRoleLimiter.java:45 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `apply` cognitive=20 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Follow-up machine pass (two REST tests)

## Summary

Machine analysis found **0** finding(s), **0** bug(s). LLM skipped (machine_only_mode)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 2 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._
