<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# PR 5025 Erlang review

Status: cli report captured. Independent gate: request-changes.
Reviewer did not author the change.

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

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

Machine short-circuit does not see the failed-catalog save block below. Erlang gate overrides to request-changes.

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/editor/EditorHost.tsx:550 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `return` cognitive=62 (max 15), cyclomatic=205 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang notes

### Bug — failed keyword load blocks the whole save

- File: `WebUI/src/main/ts/editor/widgets/KeywordFieldWidget.tsx:154`
- Also: `WebUI/src/main/ts/editor/widgets/KeywordFieldWidget.tsx:170`
- Also: `WebUI/src/main/ts/editor/EditorHost.tsx` save path that calls `collectKeywordOutsideCatalogErrors` and returns before PUT.

`loadKeywords()` `.catch` sets `keywords` to `[]` and `loaded` to true. The following effect reports those empty options. `keywordValueOutsideCatalog` treats a defined empty choice list as an authoritative catalog, so every non-empty keyword draft is "outside" the catalog. EditorHost then sets field errors and returns without PUT, including when the operator only changed another field.

A rejected catalog request is not a loaded catalog. Leave choices unset on failure (same as "not loaded yet") so save is not blocked, or surface a load error that does not pretend the catalog is empty. An empty **successful** response may still reject values that are not choices. Add a behavioral test: `loadKeywords` rejects, a stored keyword remains, Save still PUTs.

Happy-path rejection of `legacy` vs catalog `events`, empty clear, product-docs sentence, and Playwright companion are otherwise in place. Preexisting EditorHost complexity is not the block.

Recommendation: request-changes. Do not merge.
