## Summary

Machine analysis found **6** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 37 analyzed
- In-diff: 1 finding(s); preexisting: 5
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-toolkit/src/main/java/com/percussion/pso/restservice/impl/ItemRestServiceImpl.java:1188 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1188)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/perc-toolkit/src/main/java/com/percussion/pso/tasks/TrashTask.java:387 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 387)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-toolkit/src/main/java/com/percussion/pso/tasks/TrashTask.java:401 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 401)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: suggestion

- File: modules/perc-toolkit/src/main/java/com/percussion/pso/jexl/PSOFeedTools.java:184 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isMediaRSSContentAvailable` cognitive=18 (max 15), cyclomatic=7 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: modules/perc-toolkit/src/main/java/com/percussion/pso/jexl/PSOFolderTools.java:182 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getParentFolderPath` cognitive=19 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: modules/perc-toolkit/src/main/java/com/percussion/soln/listbuilder/FolderTools.java:195 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getParentFolderPath` cognitive=20 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

