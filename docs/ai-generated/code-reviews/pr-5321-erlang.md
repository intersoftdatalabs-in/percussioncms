<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5321

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5321
- Base: origin/main
- Head: 1f03673e5c03d70b78c3608a54b421e2b9c0124f
- Files analyzed: 8
- In-diff bugs: 0 (preexisting rows do not block)
- Reviewer: independent Erlang pass. Recommendation: approve.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
- In-diff: 0 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/assembly/overlayFields.ts:203 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `mapAssembledFieldElements` cognitive=60 (max 15), cyclomatic=28 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Interpreter

Independent read of the diff (not the author). Link fields classified by the existing editor control kind are overlaid. A unique assembled anchor or text node gets a text input (after an anchor, so the control is not inside the link). Read-only schema rows are omitted. Save sends dataType link through the existing item field save. HTTP failure uses restoreOverlayValues on the preview document and the field bar. The field bar renders its own link input only when the preview did not claim the field, so the two editors are not both submitted. Vitest, Playwright (save, read-only, HTTP 400/403/409), and product-docs/8.2/admin/content-explorer.md are in the diff. The cognitive-complexity row on mapAssembledFieldElements is preexisting (already over the pack threshold); the new link branch does not change the gate. No new filesystem path joins. No agent rule files. Recommendation: approve. May commit/push: yes.
