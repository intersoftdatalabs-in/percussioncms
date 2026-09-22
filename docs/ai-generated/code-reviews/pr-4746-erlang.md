<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #4746

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 317ebdbb2bc602f592bf9b8a364c3c666a3b652f
- Branch: fix/issue-4725-editor-related-remove
- Recommendation: approve (in-diff bugs: 0)

## Pre-push local code review

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/editor/editorRelatedContent.ts:108 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `insertSlotChoices` cognitive=19 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/editor/editorRelatedContent.ts:139 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `flattenRelatedContent` cognitive=26 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open


## Erlang intent

Intent: remove() calls existing requireRelationship (404) and maps adaptor failures with httpStatusForAddFailure (403/404/500), rethrowing WebApplicationException. Editor remove is hidden in read-only and when relationshipId is missing. UI does not reload the list on failure. Complexity of insertSlotChoices is a suggestion. No in-diff bug.

> Co-Authored by Grok Build 1.0.40 using grok-4.6 with agent night-issue-prs-erlang.
