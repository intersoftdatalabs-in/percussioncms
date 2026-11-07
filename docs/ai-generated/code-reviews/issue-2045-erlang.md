## Summary

Machine analysis found **16** finding(s), **1** bug(s). LLM skipped (secret_hit)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 10 analyzed
- In-diff: 1 finding(s); preexisting: 15
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerLoginPanel.java:554 (in-diff)
- Rule: `secrets.heuristic`
- Tool: `secrets.heuristic`
- Description: Possible secret or credential material (line 554)
- Suggestion: Remove secrets from source; use env, vault, or mkd-secrets.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSNavigationTree.java:634 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 634)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:236 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 236)
- Status: open

### Issue 4 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:297 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 297)
- Status: open

### Issue 5 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:396 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 396)
- Status: open

### Issue 6 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:437 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 437)
- Status: open

### Issue 7 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:438 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 438)
- Status: open

### Issue 8 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:843 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 843)
- Status: open

### Issue 9 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:868 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 868)
- Status: open

### Issue 10 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:913 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 913)
- Status: open

### Issue 11 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:1794 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 1794)
- Status: open

### Issue 12 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:2512 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 2512)
- Status: open

### Issue 13 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerLoginPanel.java:630 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 630)
- Status: open

### Issue 14 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSMainDisplayPanel.java:379 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 379)
- Status: open

### Issue 15 -- Severity: bug

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java:1279 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `applyDisplayOptions` cognitive=34 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 16 -- Severity: suggestion

- File: modules/DesktopContentExplorer/src/main/java/com/percussion/cx/javafx/PSWindowManager.java:100 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `open` cognitive=26 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

