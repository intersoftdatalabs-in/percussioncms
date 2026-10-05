<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 5193 — community description (#5178)

- Branch: `fix/issue-5178-community-description`
- Reviewed head: `d348ba882804f3a43e30c9fd8272c6c2023a30b8`
- Tool: mkd-code-review 0.1.18
- Pack: percussion
- Gate: advisory
- Git base: origin/main
- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 19 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/CommunityAdaptor.java:290 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateCommunityDescription` cognitive=14 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang verdict

Independent read of `CommunityResource.updateCommunityDescription`, `CommunityAdaptor.updateCommunityDescription`, `CommunitiesTestAdaptor`, `CommunityDetailPanel` save/cancel, and `assemblyApi.updateCommunityDescription`.

Admin and session checks run before a write. A description longer than 255 characters throws before `saveCommunities`. The same text after trim does not write. A design-lock failure is HTTP 409 and the stored description stays. The detail and catalog update only after the POST resolves; cancel does not POST. Empty or whitespace clears. Companions are present: REST DTO, adaptor interface, test adaptor, resource test, sitemanage behavioral test, Vitest, Playwright, and product-docs. No agent rule files are in the diff. Cyclomatic 16 on `updateCommunityDescription` is a suggestion, not a bug.

Recommendation: approve. May merge: yes.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
