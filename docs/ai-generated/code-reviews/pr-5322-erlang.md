<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5322

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5322
- Base: origin/main
- Head: fa6a12cf88c1b07fa480680d43a6d4ac3328295b
- Files analyzed: 9
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass. Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
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

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:1321 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateScheme` cognitive=24 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Interpreter

Independent read of the diff (not the author). updateScheme applies a description only when the body includes one. Trimmed blank clears via setDescription(null). Longer than 255 is HTTP 400 before any setter. A null description is left stored. Unique-name 409 is checked before setters, so a conflicting name does not write the description. The Design form shows name, generator, content type, template, and parameters read-only, validates length before PUT, and replaces the list only after updateScheme succeeds. Cancel does not send PUT. Companions: locationSchemeDescription helpers and Vitest, ContextsPanel, PSPublishingDesignRestServiceTest, Playwright designLocationSchemeDescription.spec.js, and product-docs/8.2/admin/publishing.md. In-diff machine row is complexity.cognitive on updateScheme (cognitive 24), severity suggestion, not a bug. No new filesystem path joins. No agent rule files. Recommendation: approve. May commit/push: yes.
