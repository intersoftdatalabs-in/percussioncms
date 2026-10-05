<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5231

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: e59ed937cb2c32ed15a98343a8e8c5a717220cfb
- Branch: fix/issue-5221-delete-delivery-type
- Recommendation: approve (in-diff bugs: 0)

## Pre-push local code review

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

### Issue 1 -- Severity: suggestion

- File: WebUI/src/main/ts/publishing/design/DeliveryTypesPanel.tsx:55 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `DeliveryTypesPanel` cognitive=28 (max 15), cyclomatic=29 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang findings

No in-diff bug. Preexisting cognitive complexity on `DeliveryTypesPanel` does not block.

Change-class closure is present: design delete now calls `requireDesignWrite` and returns 409 when a content-list URL's `sys_deliverytype` matches the type name; the panel drops the row only after DELETE succeeds, including numeric wire ids; cancel, 400, 403, and 409 keep the row. Companions: `PSPublishingDesignRestServiceTest`, Vitest for the mapper and the panel, Playwright `designDeliveryTypeDelete.spec.js`, and `product-docs/8.2/admin/publishing.md`. `findAllContentLists("")` is the existing unfiltered list query. `WebApplicationException` from the in-use check is rethrown, not wrapped as 500. No new filesystem path joins. No agent rule files.

### Suggestion

`deliveryTypeNameFromContentListUrl` finishes with `URLDecoder.decode`, which treats `+` as a space. `PSSitePublishDaoHelper.makeContentListUrl` concatenates the type name without encoding, so a name that contains `+` would not match and could be deleted while a content list still names it. Percent-encoded values (`%20`, `%2B`) are covered. Prefer a percent-decode that does not map `+` to space, or compare both the decoded and the raw query value. Not blocking: shipped names and the encoded Playwright URL do not hit this.
