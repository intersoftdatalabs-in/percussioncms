## Summary

Machine analysis found **34** finding(s), **5** bug(s). LLM skipped (hard_pattern)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 21 analyzed
- In-diff: 5 finding(s); preexisting: 29
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 5
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerLoginPanel.java:554 (preexisting)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 554)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:3226 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3226)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:5204 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 5204)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:5206 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 5206)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:5208 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 5208)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSFolderActionManager.java:899 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 899)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSFolderActionManager.java:900 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 900)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/javafx/PSWebViewUtils.java:60 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 60)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 9 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:1797 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 1797)
- Status: open

### Issue 10 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:1867 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 1867)
- Status: open

### Issue 11 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:3027 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 3027)
- Status: open

### Issue 12 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:3293 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 3293)
- Status: open

### Issue 13 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:3303 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 3303)
- Status: open

### Issue 14 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:236 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 236)
- Status: open

### Issue 15 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:297 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 297)
- Status: open

### Issue 16 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:396 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 396)
- Status: open

### Issue 17 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:437 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 437)
- Status: open

### Issue 18 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:438 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 438)
- Status: open

### Issue 19 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:843 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 843)
- Status: open

### Issue 20 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:868 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 868)
- Status: open

### Issue 21 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:913 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 913)
- Status: open

### Issue 22 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:1794 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 1794)
- Status: open

### Issue 23 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:2512 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 2512)
- Status: open

### Issue 24 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerHelper.java:67 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 67)
- Status: open

### Issue 25 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerLoginPanel.java:630 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 630)
- Status: open

### Issue 26 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerUtils.java:228 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 228)
- Status: open

### Issue 27 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSFolderActionManager.java:228 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 228)
- Status: open

### Issue 28 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSSearchViewActionManager.java:618 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 618)
- Status: open

### Issue 29 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSSearchViewActionManager.java:622 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 622)
- Status: open

### Issue 30 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/javafx/PSBrowserUtils.java:49 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 49)
- Status: open

### Issue 31 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/javafx/PSBrowserUtils.java:63 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 63)
- Status: open

### Issue 32 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSCESessionManager.java:114 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `run` cognitive=85 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 33 -- Severity: suggestion

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java:3673 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `executeServerAction` cognitive=22 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 34 -- Severity: suggestion

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerLoginPanel.java:660 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getLocaleList` cognitive=16 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

