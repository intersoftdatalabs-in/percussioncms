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
- Base: origin/main (3d6d2ba837a0f3031637b659db613c320a06a3e1); merge-base a6f548cd531d5ad72cfc8d8c32b22e9aa0bf593d (GitHub baseRefOid)
- Head: 4bc7e2daa4d74ad6dcd610ef706d8b7734efbc98
- Reviewed: the published PR head only (re-review after erlang-fix)

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

Machine pack found no in-diff static bug. That does not clear issue #4985. Commit `4bc7e2daa4` clears values only when `PSItemField.isMultiValue()` is true (`PSCoreItem.java` lines 1003-1005) and then calls `addValue`. Parent fields are built with that flag false (`PSItemDefExtractor.java` line 54, and lines 137-139 for multi-property simple children). The flag is true only for `TYPE_SIMPLE_CHILD` row sets (line 144). `no_externalurl` is not that. It is a parent-level local field with `OccurrenceSettings multiValuedType="delimited"` (`modules/perc-packages/src/main/resources/Packages/perc.nav/percNavon.itemDef.contentType` lines 95 and 122-132). For that shape `addValue` already replaces (`PSItemField.java` lines 141-142). The new branch does not run, so create/update still do not change what `GET` section returns.

`PSCoreItemSetTextFieldTest` constructs `new PSItemField(..., true)` (line 36). That is the simple-child flag, not the navon field the extractor builds. The test can pass while `GET /sitemanage/section/{id}` still omits `externalLinkUrl`.

The edit panel still reads only that GET property (`WebUI/src/main/ts/developer/SiteNavSections.tsx` lines 284 and 314). Vitest still stubs `loadSection`. Surface Playwright `developer-site-nav-external-link-edit.spec.js` line 122 still expects the created URL in the edit field. This commit does not make that true.

CI snapshot on this head: product-docs smoke pass, CodeQL javascript pass, QA wiring pass, H2 live Playwright skipped, Analyze (java-kotlin) still pending. Pending checks are not a pass. The writer miss blocks even if that job later goes green.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
