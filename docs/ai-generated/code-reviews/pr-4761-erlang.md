<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Pre-push local code review — PR #4761

## Summary

Machine analysis found **10** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 16 analyzed
- In-diff: 2 finding(s); preexisting: 8
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:1875 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1875)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2010 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2010)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2011 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2011)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2015 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2015)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2016 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2016)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:2022 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 2022)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: WebUI/src/main/ts/editor/controlKinds.ts:68 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `classifyEditorControl` cognitive=23 (max 15), cyclomatic=38 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: suggestion

- File: modules/perc-qa-automation/frontend/tests/editor-host-link-fields.spec.js:78 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `stubEditorApis` cognitive=25 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:410 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveEditorFields` cognitive=17 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:482 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `rejectUnresolvedLinks` cognitive=25 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang interpretation

Persona erlang 0.1.1. Gate is in-diff bugs only. Preexisting rows do not block.
No in-diff bugs, missing behavioral tests, non-portable path joins, or change-class gaps in this diff.
Recommendation: approve.
