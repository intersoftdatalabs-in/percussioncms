<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5475

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5475
- Base: origin/main
- Head: 040ea4d3626c24a3204b50be30fffaf14d26d644
- Files analyzed: 7
- Reviewer: independent Erlang (did not author the PR)
- LLM: CLI markdown has no `llm.error`. Wall time about 5s with empty stderr, consistent with a machine short-circuit (preexisting complexity rows are severity bug). Residual pass is this reading of the diff. Not a block.

## Independent reading

Required HTML that trims empty is refused in `blankRequiredHtmlFieldNames` before `persistOverlayEdits`. Optional HTML is not named, so a clear still saves. A NUL stays on the markup (`htmlMarkupFromElement`) and is still the HTML NUL notice, not the required notice. `aria-invalid` is applied only to `input` and `textarea`. The assembled contenteditable gets `data-assembly-required`, not `aria-required`. Cancel restores the overlay. A failed save restores the previous markup and does not set the success notice. Vitest, the surface Playwright spec, and `product-docs/8.2/admin/content-explorer.md` are in the diff. No new filesystem path joins. No agent rule files.

Preexisting `complexity.cognitive` on `handleSaveFields` and `applyFieldOverlay` is outside this slice. Not a block.

Recommendation: **approve**. May merge: yes.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
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

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:641 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=56 (max 15), cyclomatic=50 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/assembly/overlayFields.ts:1230 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldOverlay` cognitive=43 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
