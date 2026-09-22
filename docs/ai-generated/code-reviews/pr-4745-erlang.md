<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #4745

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 80fcf6a4f5a0929b310f129704270d171dee40f3
- Branch: fix/issue-4723-explorer-workflow-transition
- Recommendation: approve (in-diff bugs: 0)

## Pre-push local code review

## Summary

Machine analysis found **6** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 17 analyzed
- In-diff: 1 finding(s); preexisting: 5
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemWorkflowService.java:788 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 788)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-workflow-transitions.spec.js:292 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 292)
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-workflow-transitions.spec.js:311 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 311)
- Status: open

### Issue 4 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-workflow-transitions.spec.js:408 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 408)
- Status: open

### Issue 5 -- Severity: bug

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:525 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=286 (max 15), cyclomatic=180 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemWorkflowService.java:442 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `rejectDisallowedTransition` cognitive=18 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open


## Erlang intent

Intent: comment-required triggers are collected only inside isAllowedTransition; rejectDisallowedTransition rethrows WebApplicationException so 403/409 are not collapsed to 500. Client cancels a blank prompt before the request and does not refresh on 403/409. In-diff complexity of rejectDisallowedTransition is a suggestion, not a bug. Preexisting path/empty-catch/complexity rows are out of diff and do not block.

> Co-Authored by Grok Build 1.0.40 using grok-4.6 with agent night-issue-prs-erlang.
