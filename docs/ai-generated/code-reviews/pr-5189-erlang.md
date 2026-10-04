<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5189

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 56b42fe9ca89943d2e549ee66b4970d4b6113ed7
- Branch: fix/issue-5182-copy-delivery-type
- Recommendation: approve (in-diff bugs: 0; preexisting suggestion does not block)
- LLM: not invoked (short-circuit; no in-diff machine findings)

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
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

- File: WebUI/src/main/ts/publishing/design/DeliveryTypesPanel.tsx:46 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `DeliveryTypesPanel` cognitive=18 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Agent note

Independent Erlang review of PR #5189 (not the author). Machine gate: 0 in-diff bugs. Recommendation: approve. May merge: yes.

Copy reuses create. The body sends a new trimmed visible name (50-character maximum), the source bean name, description, and unpublishing-requires-assembly flag, and it does not send `deliveryTypeId`. Blank and overlong names are rejected before the request and again on the server. HTTP 400, 403, and 409 do not add a row. Cancel goes through the dirty-form confirm and does not create. Create and update both have a behavioral test for the 51-character name, and create is not saved. Vitest, the H2 Playwright spec, and `product-docs/8.2/admin/publishing.md` are in the diff. No rule-file diff and no new filesystem path joins.

The cognitive-complexity row on `DeliveryTypesPanel` is preexisting and is not an in-diff bug. It does not block.

LLM stage did not run: no in-diff machine findings. Ollama was up. That skip is not a blocking finding.
