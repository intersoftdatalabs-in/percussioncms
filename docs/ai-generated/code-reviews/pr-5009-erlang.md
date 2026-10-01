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
- Base: origin/main (3d6d2ba837a0f3031637b659db613c320a06a3e1)
- Head: 6715f48c9c48447e9a34584e8dc8781c1ffb44c8
- Reviewed: the published PR head only

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **0** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
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

Machine pack found no in-diff static bug. That does not clear issue #4985. The acceptance criterion is that a saved http(s) URL shows after reload. The author-recorded surface Playwright (`tests/developer-site-nav-external-link-edit.spec.js`) failed before save: create POST 200, then `GET /sitemanage/section/{id}` has no `externalLinkUrl`, so the edit field stays blank and the list cannot show the URL. Vitest stubs `loadSection` with that property and never calls the wire.

The edit panel's only URL source is that GET property (`WebUI/src/main/ts/developer/SiteNavSections.tsx` lines 284 and 314). The writer used by create and update is `PSCoreItem.setTextField` (`system/src/main/java/com/percussion/cms/objectstore/PSCoreItem.java:1000`), which calls `PSItemField.addValue`. `addValue` replaces only when the field is not multi-value (`PSItemField.java:141`). `PSManagedNavService.applyNavonPropertyMap` (`PSManagedNavService.java:776`) is the caller, and `getNavonProperties` (`PSManagedNavService.java:650`) reads `Property.getString()` (the first value). A blank starter left in front of the real URL is read back as empty, and Jackson then omits `externalLinkUrl`. This PR does not change that writer.

CI on head `6715f48c9c` was success or skipped (no fail, no pending). Checks being green does not make the failed surface path mergeable.

> Co-Authored by Grok Build 1.0.46 using grok-4.7 with agent night-issue-prs-erlang.
