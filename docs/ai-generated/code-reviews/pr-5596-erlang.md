<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue #5576 (Assembly host save one yes-no field)

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review, pack percussion, --gate advisory, --git-base origin/main
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/5576
- Parent: https://github.com/intersoftdatalabs-in/percussioncms/issues/4530
- Base: origin/main
- Head: HEAD
- Files analyzed: 6

## Verdict

Recommendation: **approve**. Blocking bugs: 0. May commit/push: yes.

Independent read of the yes-no overlay path:

- A yes-no field (`sys_SingleCheckBox` / `dataType` boolean) is mounted as a Yes/No `<select>` (same overlay shape as keyword). **Save fields** writes that one value through the existing item field PUT with `dataType` `boolean`. Other fields on the payload stay.
- Unsaved edits, **Cancel**, **Close**, and a **read-only** yes-no field do not write. HTTP 400, 403, and 409 restore the previous yes or no via `restoreOverlayValues` after remount.
- Vitest covers overlay mapping, save, reload, cancel, close, read-only, and HTTP restore (live select after remount). Playwright `assembly-yesno-field.spec.js` covers the H2 product path. Active Assembly in `product-docs/8.2/admin/content-explorer.md` states the yes-no save. No filesystem path joins. No agent rule files.
- In-diff bugs: 0. Preexisting `handleSaveFields` / `applyFieldOverlay` / `readNodeValue` complexity does not gate this slice. Yes-no NUL stays on a later slice.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- In-diff: 0 finding(s); preexisting: 3
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/ts/assembly/AssemblyHost.tsx:707 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSaveFields` cognitive=64 (max 15), cyclomatic=59 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/ts/assembly/overlayFields.ts:1492 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyFieldOverlay` cognitive=47 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: WebUI/src/main/ts/assembly/overlayFields.ts:1608 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `readNodeValue` cognitive=24 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
