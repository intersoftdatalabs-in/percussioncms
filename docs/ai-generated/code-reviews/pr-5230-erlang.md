<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5230

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 42026fd4efda306ded7093ef497a0b6111c67012
- Branch: fix/issue-5219-explorer-snippet-template
- Recommendation: request-changes (machine in-diff bugs: 0; reviewer bug: 1)

## Pre-push local code review

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 14 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

## Erlang findings

Machine short-circuit did not see the slot-index contract. One in-diff bug blocks merge.

### Bug — WebUI/src/main/ts/contentExplorer/views/RelationshipsView.tsx:599

`confirmTemplate` posts `changeTemplate(relationshipId, slotId, templateId)` and does not pass `index`. `changeSlotTemplateSlot` (`WebUI/src/main/ts/api/contentExplorer/slotRelationshipApi.ts:219`) then sends a body with no index. `SlotRelationshipAdaptor.changeTemplateSlot` (`projects/sitemanage/src/main/java/com/percussion/apibridge/SlotRelationshipAdaptor.java:251`) sets `add.setIndex(request.getIndex() == null ? -1 : request.getIndex())`, adds that relationship, and deletes the original id (`:256`). `PSContentWs.mergeAaRelationships` (`system/webservices/src/com/percussion/webservices/content/impl/PSContentWs.java:3257`) treats `-1` as append.

For slot order `[A, B, C]`, changing B's template therefore reloads as `[A, C, B']`. The row's `sortRank` is already the 0-based index in that slot (`PSExplorerRelationshipRemoveService` sets `sys_sortrank`). Pass that rank as `index`, including `0` (omitting it is what selects append). `RelationshipsView.test.tsx:947` currently locks the three-argument call (`toHaveBeenCalledWith(71, 5, 8)`); assert the sort rank for a non-last row.

Out of scope for this slice is a move control, not a silent reorder. The product note in this PR says the action does not move the relationship.

### Suggestion

`toEdge` sets `templateId` from `sys_variantid` and leaves `templateName` empty. After a full list reload the row shows the numeric id, not the label just chosen. Not a false success. Resolve the template label on the list if the name must survive reload.
