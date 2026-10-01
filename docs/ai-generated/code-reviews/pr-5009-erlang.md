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
- Head: b93495993dd03afed9505a64ff5257c40fcdffd5
- Reviewed: published PR head (code still `4bc7e2daa4`; `b934959` only records the prior block)

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

Machine pack found no in-diff static bug. `PSCoreItem.setTextField` (`system/src/main/java/com/percussion/cms/objectstore/PSCoreItem.java` lines 1003-1005) calls `clearValues()` only when `PSItemField.isMultiValue()` is true, then `addValue`. Parent fields are extracted with that flag false (`PSItemDefExtractor.processFieldSet` starts at line 54 with `false`). The flag is true only for `TYPE_SIMPLE_CHILD` row sets (line 144). `no_externalurl` is a parent local field with `OccurrenceSettings multiValuedType="delimited"` (`percNavon.itemDef.contentType`). For a non-multi `PSItemField`, `addValue` already replaces (`PSItemField.java` lines 141-142), so the new branch does not run on the navon `loadItems` path (`PSItemConverterUtils` also sets multi only when the root field set is `TYPE_SIMPLE_CHILD`).

`PSCoreItemSetTextFieldTest.setTextFieldReplacesExistingMultiValue` constructs `new PSItemField(..., true)` (line 36). That is not the flag the extractor uses for this field. The test can pass while `GET /sitemanage/section/{id}` still omits `externalLinkUrl`.

The edit panel still loads that GET property. Surface Playwright on the prior head failed because the created URL was absent from GET. CI on `b934959` is green (CodeQL, product-docs, QA wiring; live H2 Playwright skipped). Green checks do not clear this writer miss.

> Co-Authored by Grok Build 1.0.46 using grok-4.6 with agent night-issue-prs-erlang.
