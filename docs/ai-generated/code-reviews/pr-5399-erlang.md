<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5399

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5399
- Base: origin/main
- Head: e0c27c82c57efcc99f6f6f76e8d55314fa21565d
- Branch: fix/issue-5386-delivery-type-unpublish-assembly
- Files analyzed: 8
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass. Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/publishing/design/DeliveryTypesPanel.tsx:70 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `DeliveryTypesPanel` cognitive=51 (max 15), cyclomatic=52 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 1879048192\nllama_init_from_model: failed to initialize the context: failed to allocate buffer for kv cache","type":"api_error","param":null,"code":null}}

- Status: open


## Interpreter

Independent read of the diff (not the author). Unpublish assembly sends a flag-only updateDeliveryType body. Explicit false is included so Jackson marks the flag specified and the server can turn a stored true off; omitted name, description, and bean are not set. The list Yes or No changes only after the PUT succeeds. Cancel does not PUT. HTTP 400, 403, and 409 stay on the form and leave the previous flag, name, description, and bean. A failed reload after success patches only the flag on the previous rows. Java tests cover the flag-only false body and that updateDeliveryType does not call setName, setBeanName, or setDescription. Companions present: Vitest, sitemanage tests, surface Playwright, and product-docs/8.2/admin/publishing.md. No new filesystem path joins. No production Java change and no agent rule files. The preexisting DeliveryTypesPanel complexity row does not block. The local coder model ran out of CUDA memory; that warning is not an in-diff bug. Machine findings kept. Recommendation: approve. May commit/push: yes.
