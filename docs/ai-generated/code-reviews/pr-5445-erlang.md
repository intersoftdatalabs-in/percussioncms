<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5445

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/5445
- Base: origin/main
- Head: 14395cd551f87213cc6dcea135b135846c69e9c4
- Files analyzed: 13
- Reviewer: independent Erlang (did not author the PR)

## Independent reading

`displayFormatLabelWrite` sends `{ label }` only. The same trimmed label is `"unchanged"` and does not PUT. Cancel does not write. `savedDisplayFormatLabel` rejects a response whose name, description, columns, or communities differ, and it rejects a sent body that includes those fields. A blank label is sent as `""`. `applyDisplayFormatLabel` stores the internal name when the label is blank, because `PSDisplayFormat.setDisplayName` rejects empty. The client accepts `""` or that name and does not treat any other label as success. `updateDisplayFormatLabel` refuses a blank `DISPLAYNAME`. An overlong label is HTTP 400 from `setDisplayName` and does not save. HTTP 400, 403, and 409 restore the previous label and do not show **Display format label saved**. Vitest, the adaptor write test, the JDBC persist test, Playwright, and `product-docs/8.2/admin/developer-display-formats.md` are present. No new filesystem path joins. No rule files.

In-diff cognitive-complexity rows (`handleSave`, `handleLabelSave`, `savedDisplayFormatLabel`) are suggestions. Preexisting `paths.hardcoded_sep` rows in `PSUiDesignWs` are outside the label diff. The LLM stage failed open (Ollama CUDA out of memory). Neither blocks.

A case-only label change is a no-op in `setDisplayName` (`equalsIgnoreCase` returns before assign). The client then rejects the echo and keeps the previous label. That is not a data-loss bug.

Recommendation: approve. May merge: yes.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **11** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 13 analyzed
- In-diff: 3 finding(s); preexisting: 7
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: system/webservices/src/com/percussion/webservices/ui/impl/PSUiDesignWs.java:686 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 686)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: system/webservices/src/com/percussion/webservices/ui/impl/PSUiDesignWs.java:691 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 691)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: system/webservices/src/com/percussion/webservices/ui/impl/PSUiDesignWs.java:714 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 714)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: system/webservices/src/com/percussion/webservices/ui/impl/PSUiDesignWs.java:1666 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1666)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: system/webservices/src/com/percussion/webservices/ui/impl/PSUiDesignWs.java:1667 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1667)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: system/webservices/src/com/percussion/webservices/ui/impl/PSUiDesignWs.java:1750 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1750)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/DisplayFormatDetailPanel.tsx:339 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=11 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/DisplayFormatDetailPanel.tsx:423 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleDescriptionSave` cognitive=9 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/DisplayFormatDetailPanel.tsx:519 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleLabelSave` cognitive=7 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/displayFormatLabel.ts:125 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `savedDisplayFormatLabel` cognitive=17 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 11 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

