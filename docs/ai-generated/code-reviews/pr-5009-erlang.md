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
- Base: origin/main (`a6f548cd531d5ad72cfc8d8c32b22e9aa0bf593d`)
- Head: 49f113a110cd4553a62253925aec07c019f3e8f2
- Reviewed: published PR head (worktree `.kilo/worktrees/pr-5009-erlang-fix`)

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

Machine pack found no in-diff static bug. Required checks on this head are green (CodeQL, product-docs smoke, QA wiring). That is not enough.

`PSCoreItem.setTextField` (`system/src/main/java/com/percussion/cms/objectstore/PSCoreItem.java` lines 1004-1006) calls `clearValues()` then `addValue`. Parent fields are built with `isMultiValue` false (`PSItemDefExtractor` constructor line 54; `true` only for `TYPE_SIMPLE_CHILD`, lines 142-144). `PSItemField.addValue` already clears the value list when `isMultiValue()` is false (`PSItemField.java` lines 141-142) and then stores the new value. The extra `clearValues()` does not change `no_externalurl`.

`PSCoreItemSetTextFieldTest.setTextFieldReplacesParentDelimitedField` builds `new PSItemField(..., false)` (lines 40-42) and asserts a single replacement string (lines 51-55). `addValue` already does that when the flag is false, so the test passes on `setTextField` that never calls `clearValues()`. `setTextFieldReplacesSimpleChildMultiValue` (flag `true`) is the only test that needs the new clear, and that is not how parent `no_externalurl` is built.

`PSManagedNavService.applyNavonPropertyMap` (line 776) is the create and update writer. Both still persist the same single value they persisted before `a0931068`. The PR test plan records that surface Playwright failed because `GET /sitemanage/section/{id}` omits `externalLinkUrl` after a create that returned 200. This head does not re-run that spec, and the new unit test does not load a navon or assert the GET property. The edit panel still reads `externalLinkUrl` from GET, so the saved URL still cannot be shown.

> Co-Authored by Grok Build 1.0.46 using grok-4.6 with agent night-issue-prs-erlang.
