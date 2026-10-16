<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5460

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 317f93fa820703b71fcd140f5522343f416ebca7
- Branch: fix/issue-5459-blank-required-datetime
- Recommendation: approve (in-diff behavioral bugs: 0)

## Pre-push local code review

## Summary

Machine analysis found **1** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
- In-diff: 1 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/assembly/overlayFields.ts:1198 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldOverlay` cognitive=40 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang intent

`applyFieldOverlay` cognitive=40 is marked in-diff, but its body is not in the diff. `repaintFieldOverlay` was inserted above it, and `restoreOverlayValues` only widened its parameter to `{ name, value }`. The complexity is preexisting. Advisory gate counts it as a bug; this review does not. It is not a behavioral defect, a missing test, or a non-portable path.

Repaint reads the overlay edits, remounts, writes those edits back, and skips the baseline snapshot when prior nodes existed. A blank required datetime stays blank (`assemblyDatetimeValue` keeps an empty picker) and is not adopted as the saved value. Vitest covers the late iframe load for the refused blank and for a later successful save. The Playwright spec asserts the picker stays empty. `product-docs/8.2/admin/content-explorer.md` matches that. No filesystem path joins. No agent-rule files.

Recommendation: approve.

> Co-Authored by Grok Build 1.0.60 using grok-4.7 with agent night-issue-prs-erlang.
