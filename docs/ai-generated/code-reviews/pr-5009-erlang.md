<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5009

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5009
- Base: origin/main
- Head: a0931068f24d7aeaa1f9c1920d4613c1b1d70082
- Reviewed: published PR head

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No issues._

### Erlang judgment (peer)

Request changes. Do not merge.

Machine pack found no in-diff static bug. Commit `a0931068f24d` always calls `clearValues()` before `addValue` (`PSCoreItem.java` lines 1004-1006). That does not change `no_externalurl`. Parent fields are extracted with `isMultiValue` false (`PSItemDefExtractor.processFieldSet` starts at line 54 with `false`; the flag is true only for `TYPE_SIMPLE_CHILD`, line 144). `PSItemConverterUtils` sets the same flag only when the root field set is `TYPE_SIMPLE_CHILD`. Navon `no_externalurl` is a parent local field (`percNavon.itemDef.contentType`, `multiValuedType="delimited"`), so the loaded field is not multi-value. `PSItemField.addValue` already clears the value list when `isMultiValue()` is false (`PSItemField.java` lines 141-142) and then stores the new value. An extra `clearValues()` before that call is a no-op on this field. Create and update still persist the same single value they persisted before this commit, so `GET /sitemanage/section/{id}` can still omit `externalLinkUrl`.

`PSCoreItemSetTextFieldTest.setTextFieldReplacesParentDelimitedField` builds `new PSItemField(..., false)` (lines 40-42) and asserts one replacement string (lines 51-55). `addValue` already does that when the flag is false, so this test passes on the pre-fix `setTextField` that never calls `clearValues()`. `setTextFieldReplacesSimpleChildMultiValue` (line 59, flag `true`) is the only test that needs the new clear, and that is not how `no_externalurl` is built.

The edit panel still reads `externalLinkUrl` from GET. Surface Playwright on the earlier head failed because that property was absent after create. CI on `a0931068` at review time: Detect language changes, QA wiring, and product-docs smoke still in progress. Pending is not a pass. The writer no-op blocks even if those jobs later go green.

> Co-Authored by Grok Build 1.0.46 using grok-4.6 with agent night-issue-prs-erlang.
