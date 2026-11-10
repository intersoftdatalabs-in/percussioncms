# Erlang pre-push review — issue #5975 (TableFactory javadoc residual)

Adjudication (author, pre-commit): the single in-diff **bug** (Issue 11, complexity on untouched `PSJdbcReplaceExecutionPlan.execute`) is a line-shift artifact — the method body is byte-identical to base (diff above shows only an added no-arg ctor + a `@throws` label fix). Refactoring it would violate the issue's **No behavior change** acceptance. Issues 14/15 are suggestions on untouched functions for the same reason. All other bugs are tool-labeled preexisting. No in-diff behavioral, test, or portability findings. Gate: PASS (advisory).

---

## Summary

Machine analysis found **15** finding(s), **1** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 28 analyzed
- In-diff: 3 finding(s); preexisting: 12
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:3262 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3262)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:3275 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3275)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:3288 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3288)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:3301 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3301)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:3314 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3314)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:3335 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3335)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 7 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:3348 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3348)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 8 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:3360 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 3360)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 9 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableFactoryException.java:179 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 179)
- Status: open

### Issue 10 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableFactoryException.java:180 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: java.print-stack-trace
- Description: printStackTrace in production paths; use a logger (line 180)
- Status: open

### Issue 11 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcReplaceExecutionPlan.java:102 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `execute` cognitive=32 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 12 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcTableMetaData.java:56 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `PSJdbcTableMetaData` cognitive=31 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 13 -- Severity: bug

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:250 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `parseTableDefs` cognitive=112 (max 15), cyclomatic=29 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 14 -- Severity: suggestion

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcStatementFactory.java:378 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `getCopyTableDataStatement` cognitive=19 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 15 -- Severity: suggestion

- File: modules/TableFactory/src/main/java/com/percussion/tablefactory/RxJdbcTableFactory.java:83 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `populateTablesIn` cognitive=30 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

