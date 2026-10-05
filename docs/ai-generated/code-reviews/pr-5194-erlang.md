<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 5194 — multi-folder allowed publish sites (#5181)

- Branch: `fix/issue-5181-multi-folder-allowed-sites`
- Reviewed head: `3a001484125807246edec43db4618cf704f6e6d1`
- Tool: mkd-code-review 0.1.18
- Pack: percussion
- Gate: advisory
- Git base: origin/main
- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`

## Pre-push local code review

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 2 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/contentExplorer/setFolderAllowedSites.ts:684 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `describeSetFolderAllowedSitesMultiSave` cognitive=16 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/explorer-set-folder-allowed-sites-multi.spec.js:142 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installAllowedSitesRoutes` cognitive=17 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang verdict

Independent read of `saveSetFolderAllowedSitesOnSelection`, `ContentExplorerShell` confirm/cancel, and `DetailList` badges.

Each checked folder is read, posted, and read again. `onFolderSaved` runs only when the reloaded canonical site list matches, so a badge is not applied on HTTP 400, 403, 409, or a mismatch. Pages and assets are named and skipped and are not posted. A per-folder failure is `partial` or `failed`, and the status kind is error rather than full success. Cancel does not save. Complexity on the status formatter and the Playwright route helper are suggestions, not bugs. No agent rule files are in the diff.

Recommendation: approve. May merge: yes.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
