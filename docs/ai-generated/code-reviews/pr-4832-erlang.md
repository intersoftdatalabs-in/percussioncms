<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #4832

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/4832
- Base: origin/main
- Head: 3fbbbc62c3d037f4a5eb237ed77b1db9f199c032
- Files analyzed: 8
- In-diff findings: 0; preexisting: 1 (`dispatchAction` complexity)
- Reviewer note: independent of the author. In-diff bugs only. Preexisting rows do not block.

## CLI stdout (`mkd-code-review analyze --format markdown`)

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

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:895 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=304 (max 15), cyclomatic=193 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Intent

Multi-select Publish Now follows the existing Stage / Take Down batch shape: one confirm, `partitionStageSelection` skips folders, per-item failures do not mark the batch as full success, cancel publishes nothing, and a single selection still uses `publishSelectedItem`. Shell does not pass `onPublish`, so the live path is `publishSelectedItems` (HTTP and application-level preflight throws are recorded). Vitest covers the batch; Playwright covers confirm, cancel, and HTTP 403; product-docs Publish Now multi-select is updated. New strings use the same `perc.ui.explorer@` fallback as Stage multi (no separate TMX tree in this repo). `confirm` double-`message()` matches the Stage peer. Preexisting `dispatchAction` complexity is not an in-diff blocker.

## Gate (Erlang)

- Blocking bugs: 0
- May commit/push: yes
- Recommendation: approve
