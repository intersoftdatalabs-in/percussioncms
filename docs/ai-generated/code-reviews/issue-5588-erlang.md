<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue #5588

## Scope

- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main, --models models.ollama-dev-coder.toml
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/5588
- Parent: #4532
- Base: origin/main
- Head: fix/issue-5588-editor-file-field-nul
- Reviewed: staged branch diff

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **1** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 7 analyzed
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

- File: WebUI/src/main/ts/editor/EditorHost.tsx:1441 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=159 (max 15), cyclomatic=202 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Agent interpretation

- The finding is **preexisting**: `handleSave` already carried the date,
  datetime, number, decimal, yes-no, link, table, and keyword NUL gates before
  this slice. This slice adds one more gate in the same chain (file NUL) and
  does not introduce the complexity. Splitting `handleSave` is a separate slice,
  not part of a file-field NUL refusal.
- The gate reads the **raw** field value and the picked `File.name`. The
  rendered name is sanitized by `displayBinaryFileName`, so the check must not
  use the displayed text — otherwise the NUL would slip through.
- A file row is excluded from the item-field PUT by `fieldsForEditorSave`, so
  the refusal is what keeps a NUL-bearing file name from reaching the server
  through the binary upload path.
- No missing behavioral tests: `fileField.test.ts` covers the value check, the
  picked-file check, and the collector; the `EditorHost` block covers the
  refusal, the reload, the ordinary save, and HTTP 400 / 403 / 409; the H2
  Playwright spec covers the stored-name refusal and the ordinary replace.
- No non-portable path/file I/O.
- Gate: PASS for commit/push.
