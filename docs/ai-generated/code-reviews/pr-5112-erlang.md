<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5112 (re-review after community-row-first fix)

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main (`d1d9ba90bddc988897b11ab903d3ef9dfa007c6f`)
- Head: `07b21b3a77869cb3cc0f11674e24af9db1dd9fad` (`fix/issue-5091-editor-clear-community`)
- Recommendation: approve
- LLM: ollama-dev-coder CUDA OOM; machine findings kept

The prior blocking bug is fixed. `persistClearedCommunity` writes `CONTENTSTATUS` community id `0` and commits that summary before `contentItemDao.save`. `PSORMException` and `RuntimeException` restore the previous in-memory id and do not save the content item. `communityClearOrmFailureIsConflict` asserts the field stayed at the previous id and that `contentItemDao.save` is not called.

## Pre-push local code review

## Summary

Machine analysis found **9** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 0 finding(s); preexisting: 8
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemEditorFieldsMapper.java:182 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 182)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2067 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2067)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2202 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2202)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2203 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2203)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2207 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2207)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2208 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2208)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2214 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2214)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:468 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveEditorFields` cognitive=24 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Erlang gate

Machine gate: 12 files, in-diff findings 0, blocking bugs 0. Preexisting path-separator rows and `saveEditorFields` complexity do not block. Ollama `dev-coder` CUDA OOM is not a defect in this diff.

`saveEditorFields` now calls `persistClearedCommunity` before `applyUpdates` / `contentItemDao.save`. A failed summary save restores the previous community id and leaves the content item unsaved, so a reload does not show the empty option. Behavioral coverage: `blankCommunityClearsContentStatusAndReturnsEmptyOption` (order), `selectedCommunityDoesNotClearContentStatus`, `communityClearOrmFailureIsConflict`, `unassignedCommunityReloadsAsEmptyOption`.

Not blocking: `PSCmsObjectMgr.saveComponentSummaries` commits on its own transaction. If the later content-item save throws, community id `0` is already stored. That is the order this review required. A compensating restore is optional, not a merge block. `EditorHost` mapping a generic HTTP 400/403 onto the only community field is already in the diff.
