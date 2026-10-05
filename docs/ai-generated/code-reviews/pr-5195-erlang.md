<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 5195 — edition content-list order (#5184)

- Branch: `fix/issue-5184-edition-content-list-order`
- Reviewed head: `3dc4785362a581f6113479b919b1a80af2e3cc7a`
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
- Files: 11 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:817 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `associateContentList` cognitive=16 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang verdict

Independent read of `reorderEditionContentList`, `EditionEditor.handleReorder`, and `reorderEditionContentList` in `designApi`.

Non-adjacent positions, a missing sequence, and a blank id are HTTP 400 before any save. Design-write denial is 403. A running publish job is 409. An adjacent move rewrites stored sequences to 1..n. The editor swaps that pair only after the PUT returns; cancel does not write. Edition priority is not part of this request. The `associateContentList` complexity row is preexisting and out of diff, so it does not block. The two `saveEditionContentList` calls are separate transactions; a failure on the second row could leave a partial order while the UI shows the error. That is not the specified happy path or the 400/403/409 paths, and it is not a missing behavioral test. No agent rule files are in the diff.

Recommendation: approve. May merge: yes.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
