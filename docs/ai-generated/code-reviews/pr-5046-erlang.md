<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
-->

# Erlang review PR 5046

Independent review (not the author). Persona: erlang 0.1.1.
CLI: mkd-code-review 0.1.18, pack percussion, gate advisory, base origin/main.
Machine gate is clean. Erlang blocks on a persist-then-reject bug the metrics pass did not see.

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 18 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/share/relationship/service/impl/PSExplorerRelationshipRemoveService.java:145 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `addOwned` cognitive=17 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang

Recommendation: **request-changes**. In-diff blocking bugs: 1.

### Bug — file:line

`projects/sitemanage/src/main/java/com/percussion/share/relationship/service/impl/PSExplorerRelationshipRemoveService.java:173`

`IPSSystemWs.createRelationship` (`PSSystemWs.java` around the `saveRelationship` call) already persists the new relationship before it returns. `addOwned` then rejects folder category and Active Assembly rows (lines 175-184) and returns CONFLICT without deleting that row. A later `saveRelationships` failure (lines 185-194) also returns CONFLICT after the first save has already committed when `createRelationship` runs in its own transaction.

Effect: the panel says the relationship was not added, but the row exists. Active Assembly type names are not refused before create. The name denylist does not cover every config whose category is `rs_folder`.

The unit test `addSaveFailureIsConflict` mocks `createRelationship` as non-persisting, so it does not catch this.

Fix: refuse folder category and Active Assembly from the relationship config **before** any save, and do not call a second save that can disagree with a row `createRelationship` already stored. Add a behavioral test that a refused type is not left saved.
