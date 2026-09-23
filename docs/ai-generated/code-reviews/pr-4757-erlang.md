<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR #4757

## Scope

- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Status: mkd-code-review 0.1.18, pack percussion, --gate advisory, --git-base origin/main
- PR: https://github.com/intersoftdatalabs-in/percussioncms/pull/4757
- Base: origin/main
- Head: f19e91f29c8f0644c2c110add00f7371fa9abc96
- Files analyzed: 18
- Reviewer disposition: **approve**. In-diff machine finding is cognitive complexity on `numericFieldProblem` (suggestion), not a bug. Preexisting path heuristics on the NUL message and `PSItemService` are out of diff (the mapper line is a quote character, not a filesystem join). Server rejects non-numeric and out-of-range values as HTTP 400 before `prepareForEdit` when the PUT carries `dataType`. Client save blocks the same cases. Not a rule-file change.
- Recommendation: **approve**. May merge: yes, when required checks are green.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **10** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 18 analyzed
- In-diff: 1 finding(s); preexisting: 9
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemEditorFieldsMapper.java:117 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 117)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:1813 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1813)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:1948 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1948)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:1949 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1949)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:1953 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1953)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:1954 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1954)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:1960 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1960)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: WebUI/src/main/ts/editor/controlKinds.ts:67 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `classifyEditorControl` cognitive=22 (max 15), cyclomatic=37 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: suggestion

- File: WebUI/src/main/ts/editor/numericField.ts:118 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `numericFieldProblem` cognitive=23 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemService.java:407 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `saveEditorFields` cognitive=17 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
