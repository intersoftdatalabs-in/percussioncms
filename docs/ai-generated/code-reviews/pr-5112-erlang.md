<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5112

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: e2fad49851 (report regenerated on this head; machine scope is this branch)
- Branch: fix/issue-5091-editor-clear-community
- Recommendation: request-changes (machine in-diff bugs: 0; Erlang blocking bug: 1)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

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

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2052 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2052)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2187 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2187)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2188 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2188)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2192 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2192)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2193 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2193)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2199 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2199)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:461 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveEditorFields` cognitive=21 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Erlang gate (reviewer)

Machine gate: 12 files, in-diff findings 0, blocking bugs 0. Preexisting path and complexity rows do not block. Ollama `dev-coder` CUDA OOM is not a defect in this diff. The machine recommendation is approve. Erlang does not agree.

Recommendation: **request-changes**. One blocking bug.

### Bug — partial community clear (blocking)

- File: `projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:504` (field save) and `:228` (`persistClearedCommunity`)
- Also: `projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemEditorFieldsMapper.java:107` (`fillCommunityWhenAbsent` keeps a present blank)
- `saveEditorFields` calls `contentItemDao.save(item)` before `persistClearedCommunity`. `PSContentItemDao` is `@Transactional` and `PSItemService` is not, so the blank `sys_communityid` commits when `save` returns. The summary write uses `IPSCmsObjectMgr.saveComponentSummaries` on another session. `PSORMException` becomes HTTP 409, but the content-item field is already blank.
- `setCommunityId(0)` runs before that save (`PSItemService.java:232`). `evictComponentSummaries` runs only after success, so a failed save leaves a cached summary at community id 0 while `CONTENTSTATUS` is unchanged.
- Reload calls `applyCommunityOnRead`. A non-zero summary id uses `fillCommunityWhenAbsent`, which returns when `sys_communityid` is already present, including blank. The editor shows the empty option after a failed clear.
- `communityClearOrmFailureIsConflict` asserts the 409 and does not assert the content-item field stayed at the previous id.

Fix: persist `CONTENTSTATUS` community id 0 first, and do not write the blank field unless that save succeeds. On failure, put the previous id back on the in-memory summary. Add a unit test that a thrown `saveComponentSummaries` leaves the item `sys_communityid` unchanged.

### Suggestion (not blocking)

- `WebUI/src/main/ts/editor/EditorHost.tsx:1639` — a generic HTTP 400/403 with no field name is attached to `sys_communityid` when it is the only community field and no earlier mapper claimed the error. Same cascade as date/keyword, but community is on almost every type, so an unrelated save failure can read as a community error.

Tests, product-docs, and the Playwright surface spec are present for the happy path. No rule-file diff. No new non-portable filesystem path joins.
