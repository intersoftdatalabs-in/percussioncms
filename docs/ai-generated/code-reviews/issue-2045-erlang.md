## Summary

Machine analysis found **16** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 41 analyzed
- In-diff: 0 finding(s); preexisting: 16
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerLoginPanel.java:552 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 552)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/ServerConnection.java:96 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 96)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerMenu.java:442 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 442)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSFolderActionManager.java:899 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 899)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSFolderActionManager.java:900 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 900)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/ServerConnection.java:41 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 41)
- Status: open

### Issue 7 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerHelper.java:67 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 67)
- Status: open

### Issue 8 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerLoginPanel.java:628 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 628)
- Status: open

### Issue 9 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSFolderActionManager.java:228 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 228)
- Status: open

### Issue 10 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSItemAssemblyManager.java:394 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 394)
- Status: open

### Issue 11 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSMainDisplayPanel.java:377 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 377)
- Status: open

### Issue 12 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSSearchDialog.java:167 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 167)
- Status: open

### Issue 13 -- Severity: suggestion

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSFolderAclEditorDialog.java:262 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getCatalogEntries` cognitive=23 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 14 -- Severity: suggestion

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/javafx/PSDesktopExplorerWindow.java:421 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `setJavaBridge` cognitive=19 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 15 -- Severity: suggestion

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/javafx/PSSimpleSwingBrowser.java:405 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `createScene` cognitive=29 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 16 -- Severity: suggestion

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/javafx/PSWindowManager.java:99 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `open` cognitive=26 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

