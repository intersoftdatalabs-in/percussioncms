# Erlang review — issue 5459

Persona: erlang 0.1.1
Tool: mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main
Models: /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml

## Summary

Machine analysis found **1** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
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

## Night-issue gate

The complexity row is on `applyFieldOverlay`. Its body is unchanged in this diff; `repaintFieldOverlay` was inserted above it and `restoreOverlayValues` only widened its parameter to `{ name, value }`. The row is preexisting complexity, not a behavioral bug, a missing test, or non-portable path handling. It does not block this change.

Night-issue gate: PASS (no in-diff behavioral bug, missing behavioral test, or non-portable path).

> Co-Authored by Grok Build 1.0.60 using grok-4.7 with agent night-issue-prs.
