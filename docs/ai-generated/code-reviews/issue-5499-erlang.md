# Erlang review — issue #5499 (locale label)

Persona: erlang 0.1.1
Pack: percussion
Gate: advisory (strict: in-diff bugs, missing behavioral tests, non-portable paths)
Base: origin/main
Head: staged working tree (committed with this report)

## Summary

Machine analysis found **7** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 12 analyzed
- In-diff: 3 finding(s); preexisting: 4
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

- File: rest/src/main/java/com/percussion/rest/locales/LocalesResource.java:271 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 271)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:383 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleDescriptionSave` cognitive=6 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:444 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=16 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/localeLabel.ts:75 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `formatFingerprint` cognitive=16 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/localeLabel.ts:105 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `savedLocaleLabel` cognitive=16 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Intent / companions

Slice 106 is a label-only save on the existing locale PUT. Language string,
description, enabled status, and format stay. Blank or whitespace label does
not write. Cancel and HTTP 400/403/409 are not success.

Companions in this diff:

- Helper + Vitest (`localeLabel.ts` / `localeLabel.test.ts`)
- Messages kept out of `messages.ts` (`localeLabelMessages.ts`)
- Panel chrome + panel tests (`LocaleDetailPanel`)
- Catalog label attribute (`LocalesPanel`)
- Adaptor tests for label-only PUT and blank/omitted label
- REST OpenAPI / adaptor javadoc (no method signature change)
- H2 Playwright `developer-locale-label.spec.js` (peer of `developer-locale-editor.spec.js`)
- Product-docs note on `product-docs/8.2/admin/developer-locales.md` (not an index)

No Java API shape change. No new path/file I/O. No banned thrash paths.
Preexisting `paths.hardcoded_sep` hits are JAX-RS `@Path` URL segments, not
filesystem joins. `handleSave` complexity is the existing locale save plus
labelShown reset. `formatFingerprint` / `savedLocaleLabel` refuse a PUT
response that changed language, description, status, or format. None of those
are in-diff behavioral bugs.

## Verdict

Gate: PASS. Recommendation: approve. May commit/push: yes.
