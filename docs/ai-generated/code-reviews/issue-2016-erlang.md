## Summary

Machine analysis found **11** finding(s), **1** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 71 analyzed
- In-diff: 4 finding(s); preexisting: 7
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/utils/src/main/java/com/percussion/install/InstallUtil.java:1212 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 1212)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/utils/src/main/java/com/percussion/install/InstallUtil.java:665 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 665)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/utils/src/main/java/com/percussion/install/InstallUtil.java:688 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 688)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: modules/utils/src/main/java/com/percussion/util/PSHttpConnection.java:125 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 125)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: modules/utils/src/main/java/com/percussion/util/PSHttpConnection.java:131 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 131)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: modules/utils/src/main/java/com/percussion/util/PSHttpConnection.java:181 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 181)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: modules/utils/src/main/java/com/percussion/util/PSSqlHelper.java:1518 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1518)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: modules/utils/src/main/java/com/percussion/utils/container/adapters/JettyDatasourceConfigurationAdapter.java:171 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `loadRxDatasources` cognitive=45 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: suggestion

- File: modules/utils/src/main/java/com/percussion/util/PSCharSets.java:316 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `guessEncoding` cognitive=22 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: suggestion

- File: modules/utils/src/main/java/com/percussion/util/PSSqlHelper.java:1460 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleBackingIndex` cognitive=28 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 11 -- Severity: suggestion

- File: modules/utils/src/main/java/com/percussion/utils/io/PathUtils.java:82 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getRxDir` cognitive=26 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

