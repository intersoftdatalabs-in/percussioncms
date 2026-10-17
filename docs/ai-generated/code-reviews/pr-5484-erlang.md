<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 5484

Independent pre-merge review. Author of the feature is not this review. Persona: erlang 0.1.1. Persona source: /home/nate/.local/share/mkd/agents/erlang.

## Pre-push local code review

## Summary

Machine analysis found **6** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- In-diff: 2 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1578 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleLabelSave` cognitive=6 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1673 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleDescriptionSave` cognitive=6 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1742 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startRequiredEdit` cognitive=2 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1783 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleRequiredSave` cognitive=13 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1865 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `startSearchableEdit` cognitive=2 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/ContentTypeDetailPanel.tsx:1906 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSearchableSave` cognitive=12 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang

Independent reading: in-diff cognitive complexity on `startSearchableEdit` and `handleSearchableSave` matches the sibling label and required handlers and is a suggestion, not a bug. `otherFieldSearchableStays` calls `fieldShellStays`, so a response that changes another field's label, required flag, occurrence, or type is not success. The searchable-only body omits label, description, enabled, workflows, templates, and type-level search indexing. `ContentTypeAdaptorUpdateTest` covers local, system, and shared fields, an unknown field (404), a missing lock, and a non-admin (403). Vitest, Playwright, and `product-docs/8.2/admin/developer-content-types.md` are present. No filesystem path joins. No rule files.

Gate: PASS. Recommendation: approve. May commit/push: yes.
