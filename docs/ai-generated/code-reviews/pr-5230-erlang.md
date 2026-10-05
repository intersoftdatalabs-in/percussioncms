<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5230

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 6ed3d066ca819a26322fc4d001c9f1d43f7bc37a
- Branch: fix/issue-5219-explorer-snippet-template
- Recommendation: approve (machine/LLM in-diff bugs: 1, dismissed as stale; reviewer bugs: 0)

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 15 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/contentExplorer/views/RelationshipsView.tsx:599
- Rule: `llm.ollama-dev-coder`
- Tool: `llm`
- Description: confirmTemplate posts changeTemplate(relationshipId, slotId, templateId) and does not pass index.
- Suggestion: Pass the sort rank as the index parameter in the call to changeTemplate.
- Status: open

## Erlang findings

The LLM row is the previous slot-index bug, restated against a line that no longer matches. It does not block.

`confirmTemplate` posts the row index, including `0`:

```601:606:WebUI/src/main/ts/contentExplorer/views/RelationshipsView.tsx
      const updated = await changeTemplate(
        gate.relationshipId,
        gate.slotId,
        gate.templateId,
        gate.index,
      );
```

`gate.index` comes from `relationshipSlotIndex`, which returns `0` for a missing or negative rank and the sort rank otherwise (`changeRelationshipTemplate.ts`). `changeSlotTemplateSlot` JSON-encodes that number, so `0` is not dropped. `SlotRelationshipAdaptor.changeTemplateSlot` uses `-1` only when `index` is null. `PSContentWs.mergeAaRelationships` appends only for `-1` or an index past the slot list. The slot list is slot-scoped, and `sortRank` is the 0-based order inside that slot.

Tests that lock this:

- `RelationshipsView.test.tsx` expects `toHaveBeenCalledWith(71, 5, 8, 0)` before the row updates, and on HTTP 400, 403, and 409.
- `changeRelationshipTemplate.test.ts` expects index `0` and index `2`.
- `explorer-change-relationship-template.spec.js` expects the posted body `index` to be `0`.

Cancel, an empty choice, a folder row, and those HTTP errors still leave the previous template. No new filesystem path joins. Product docs, Vitest, the sitemanage `templateId` mapping, and the Playwright surface spec are present. No agent rule files are in the diff.

### Suggestion

`toEdge` still leaves `templateName` empty, so a full reload shows the numeric template id until the dialog label is applied again. Not a false success. Not a merge block.

Recommendation: approve. May merge: yes, when required checks on this head are green.
