<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #5112

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, gate advisory
- Base: origin/main
- Head: 7f623e451567d4c75a8550a444424fec9a4d116b
- Branch: fix/issue-5091-editor-clear-community
- Recommendation: request-changes (machine in-diff bugs: 0; Erlang blocking bug: 1)
- LLM: ollama-dev-coder OOM (cudaMalloc); machine findings kept

## Pre-push local code review

## Summary

Machine analysis found **58** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 140 analyzed
- In-diff: 0 finding(s); preexisting: 57
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/resources/Rhythmyx/AppServer/server/rx/deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/projects/sitemanage-beans.xml:1441 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1441)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:271 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 271)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:274 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 274)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:423 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 423)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:532 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 532)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:656 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 656)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:659 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 659)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:744 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 744)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 9 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:841 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 841)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 10 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:851 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 851)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 11 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:857 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 857)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 12 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:876 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 876)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 13 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:877 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 877)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 14 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:1047 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1047)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 15 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:1319 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1319)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 16 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:1418 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1418)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 17 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:1765 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1765)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 18 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:1835 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1835)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 19 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:1837 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1837)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 20 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:1871 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1871)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 21 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:551 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 551)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 22 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:758 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 758)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 23 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:779 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 779)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 24 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:806 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 806)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 25 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:808 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 808)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 26 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:1139 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1139)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 27 -- Severity: bug

- File: system/src/main/java/com/percussion/fastforward/managednav/PSManagedNavService.java:466 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 466)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 28 -- Severity: bug

- File: system/src/main/java/com/percussion/fastforward/managednav/PSManagedNavService.java:1390 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1390)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 29 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-rename-folder.spec.js:107 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 107)
- Status: open

### Issue 30 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-rename-folder.spec.js:122 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 122)
- Status: open

### Issue 31 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-rename-folder.spec.js:256 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 256)
- Status: open

### Issue 32 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-rx-folder-mutations.spec.js:85 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 85)
- Status: open

### Issue 33 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-rx-folder-mutations.spec.js:289 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 289)
- Status: open

### Issue 34 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:71 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 71)
- Status: open

### Issue 35 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:111 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 111)
- Status: open

### Issue 36 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:113 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 113)
- Status: open

### Issue 37 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:116 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 116)
- Status: open

### Issue 38 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:130 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 130)
- Status: open

### Issue 39 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:132 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 132)
- Status: open

### Issue 40 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:137 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 137)
- Status: open

### Issue 41 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:293 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 293)
- Status: open

### Issue 42 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:353 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 353)
- Status: open

### Issue 43 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:355 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 355)
- Status: open

### Issue 44 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:385 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 385)
- Status: open

### Issue 45 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:585 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 585)
- Status: open

### Issue 46 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:587 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 587)
- Status: open

### Issue 47 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-translations.spec.js:716 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 716)
- Status: open

### Issue 48 -- Severity: bug

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:1691 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=338 (max 15), cyclomatic=209 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 49 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:760 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateSectionInfo` cognitive=47 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 50 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/SlotDetailPanel.tsx:386 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=16 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 51 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSExitDefDependencyHandler.java:109 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getDependencyFiles` cognitive=19 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 52 -- Severity: suggestion

- File: deployer/src/main/java/com/percussion/deployer/server/dependencies/PSExitDefDependencyHandler.java:176 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `installDependencyFiles` cognitive=19 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 53 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/FolderAdaptor.java:479 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `extractSectionInfo` cognitive=17 (max 15), cyclomatic=7 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 54 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/SlotsAdaptor.java:200 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `updateSlot` cognitive=22 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 55 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingDesignRestService.java:258 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `copyEdition` cognitive=28 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 56 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/publishingdesign/impl/PSPublishingRuntimeSupport.java:91 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `listRuntimeEditions` cognitive=23 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 57 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/sitemanage/service/impl/PSSitePublishService.java:1009 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `unapproveQueuedIncrementalContent` cognitive=16 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 58 -- Severity: suggestion

- File: review
- Rule: `llm.error`
- Tool: `llm`
- Description: model `ollama-dev-coder` failed: http: status 500 Internal Server Error body {"error":{"message":"llama-server process has terminated: exit status 1: cudaMalloc failed: out of memory\nalloc_tensor_range: failed to allocate CUDA0 buffer of size 5064192000\nerror loading model: unable to allocate CUDA0 buffer","type":"api_error","param":null,"code":null}}

- Status: open

## Erlang gate (reviewer)

Machine gate: in-diff findings 0, blocking bugs 0, preexisting rows do not block. Ollama `dev-coder` failed with CUDA OOM; that warn is not a defect in this diff.

Recommendation: **request-changes**. One blocking bug the diff-aware tools did not raise.

### Bug — partial community clear (blocking)

- File: `projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:504` (field save) and `:228` (`persistClearedCommunity`)
- Also: `projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemEditorFieldsMapper.java:107` (`fillCommunityWhenAbsent` keeps a present blank)
- `saveEditorFields` calls `contentItemDao.save(item)` before `persistClearedCommunity`. `PSContentItemDao` is `@Transactional` and `PSItemService` is not, so the blank `sys_communityid` commits when `save` returns. The summary write uses `IPSCmsObjectMgr.saveComponentSummaries` on another session. `PSORMException` becomes HTTP 409, but the content-item field is already blank.
- `setCommunityId(0)` runs before that save (`PSItemService.java:232`). `evictComponentSummaries` runs only after success, so a failed save leaves a cached summary at community id 0 while `CONTENTSTATUS` is unchanged.
- Reload calls `applyCommunityOnRead`. A non-zero summary id uses `fillCommunityWhenAbsent`, which returns when `sys_communityid` is already present, including blank. The editor shows the empty option after a failed clear.
- `communityClearOrmFailureIsConflict` asserts the 409 and does not assert the content-item field stayed at the previous id.

Fix: persist `CONTENTSTATUS` community id 0 first, and do not write the blank field unless that save succeeds. On failure, put the previous id back on the in-memory summary. Add a unit test that a thrown `saveComponentSummaries` leaves `item` `sys_communityid` unchanged.

### Suggestion (not blocking)

- `WebUI/src/main/ts/editor/EditorHost.tsx:1639` — a generic HTTP 400/403 with no field name is attached to `sys_communityid` when it is the only community field and no earlier mapper claimed the error. Same cascade as date/keyword, but community is on almost every type, so an unrelated save failure can read as a community error.

Tests, product-docs, and the Playwright surface spec are present. No rule-file diff. No new non-portable filesystem path joins.
