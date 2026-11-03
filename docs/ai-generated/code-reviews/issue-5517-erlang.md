# Erlang review — issue #5517 (locale date-time pattern)

Machine analysis found **6** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- In-diff: 2 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/locales/LocalesResource.java:155 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 155)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/locales/LocalesResource.java:224 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 224)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/locales/LocalesResource.java:280 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 280)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:810 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=16 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:586 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isTimePatternOnlyWrite` cognitive=4 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:613 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isDateTimePatternOnlyWrite` cognitive=4 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Gate decision

No in-diff blocking bugs, missing behavioral tests, or non-portable path construction in this slice. Preexisting `paths.hardcoded_sep` rows on JAX-RS `@Path` URLs are not filesystem joins. Complexity suggestions match the date/time pattern peer and do not block.

Gate: PASS (advisory; 0 blocking bugs)
