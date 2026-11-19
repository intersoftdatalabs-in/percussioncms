## Summary

Machine analysis found **4** finding(s), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 13 analyzed
- In-diff: 2 finding(s); preexisting: 2
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/pipelines/PipelinesResource.java:1178 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 1178)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/PipelinesAdaptor.java:938 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `putFilterGroup` cognitive=27 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/PipelinesAdaptor.java:1024 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `putMapper` cognitive=17 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/PipelinesAdaptor.java:1149 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `putBinaryResource` cognitive=19 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
