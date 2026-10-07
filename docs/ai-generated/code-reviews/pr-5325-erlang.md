<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5325

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5325
- Base: origin/main
- Head: 28d0fe3d8d1861039599a8cb58ec41c5053a4797
- Files analyzed: 13
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass. Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 13 analyzed
- In-diff: 3 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1354 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateScheme` cognitive=27 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1839 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `prepareSchemeParameterAddition` cognitive=17 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1917 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applySchemeParameters` cognitive=16 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Interpreter

Independent read of the diff (not the author). Design add-parameter sends `addParameter: true` and exactly one parameter, and omits name, generator, description, content type, and template. `updateScheme` validates that one parameter before any setter. Blank name, type, or value, or a name or type longer than 50, is HTTP 400 and does not save. A duplicate parameter name is HTTP 409 and does not save. The append uses `IPSLocationScheme.addParameter` and assigns a missing `SCHEMEPARAMID` only on a real `PSLocationScheme`. Cancel, empty fields, and HTTP 400/403/409 leave the previous list. Companions are present: `locationSchemeAddParameter.ts` plus Vitest, `ContextsPanel`, `PSPublishingDesignRestServiceTest`, Playwright `designLocationSchemeAddParameter.spec.js`, and `product-docs/8.2/admin/publishing.md`. No new filesystem path joins. No agent rule files.

`wrapLocationScheme` now nests parameters as `parameters.schemeParameter`. The existing Jackson test still shows a bare `schemeParameter` array binds, and a bare `parameters` array does not. The H2 Playwright spec creates a scheme with a path parameter through this wrapper and then appends one more parameter on the live cell, so the wrapper shape is what the resource stores. The comment that a bare array is ignored is stronger than that Jackson test; both shapes binding is not a defect.

In-diff machine rows are `complexity.cognitive` on `updateScheme`, `prepareSchemeParameterAddition`, and `applySchemeParameters`. Severity suggestion, not a bug.

Recommendation: approve. May commit/push: yes.
