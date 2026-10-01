<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# PR 5027 Erlang review

Status: cli report captured. Independent gate: approve.
Reviewer did not author the change.

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:1701 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=364 (max 15), cyclomatic=221 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang notes

`edit_promotableversion` trims the item id, refuses folders/sites/blank ids before confirm side effects beyond the existing confirm, and maps HTTP 400/403/409 to named explorer messages without `refresh: true`. Cancel returns without refresh. Other errors still propagate. Unit tests and `explorer-promotable-version` helper/spec plus the content-explorer product-docs sentence close the change class. `dispatchAction` cognitive complexity is preexisting and not an in-diff gate. No rule files. Recommendation: approve.
