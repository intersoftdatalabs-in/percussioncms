# Erlang review — PR #5538 (issue #5516 locale time pattern)

Command:

```text
mkd-code-review analyze --pack percussion --format markdown --gate advisory \
    --git-base origin/main \
    --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml
```

Cwd: PR head `afda75af713775512d1ab0819b3b28e7c560d831` (`fix/issue-5516-locale-time-pattern`).
CLI: mkd-code-review 0.1.18. Reviewer did not author this PR.

## Summary

Machine analysis found **7** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 14 analyzed
- In-diff: 3 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang
- Pack: percussion
- CLI: mkd-code-review 0.1.18
- Gate: advisory (in-diff bugs only)

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- May merge: yes

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

- File: rest/src/main/java/com/percussion/rest/locales/LocalesResource.java:277 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 277)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:681 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=16 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/localeTimePattern.ts:122 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `savedLocaleTimePattern` cognitive=19 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:542 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isDatePatternOnlyWrite` cognitive=4 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:569 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isTimePatternOnlyWrite` cognitive=4 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Gate notes

- Preexisting `paths.hardcoded_sep` rows on JAX-RS `@Path("/…")` in `LocalesResource` are URL paths, not filesystem joins. Do not BLOCK.
- In-diff complexity on `handleSave`, `savedLocaleTimePattern`, and `isTimePatternOnlyWrite` matches the date-pattern peer (`#5500` / PR `#5529`). Not bugs.
- Behavioral tests: adaptor time-pattern-only persist + blank-clear + `isTimePatternOnlyWrite` detection; rest invalid-pattern 400; Vitest body/omit/unchanged; Playwright H2 surface (1 passed per Work).
- Change-class companions: rest javadoc, sitemanage adaptor, WebUI Vitest, Playwright, `product-docs/8.2/admin/developer-locales.md` (not an index).
- Non-portable filesystem paths: none.

Gate: **PASS**. Recommendation: approve.
