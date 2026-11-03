## Summary

Machine analysis found **6** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 11 analyzed
- In-diff: 3 finding(s); preexisting: 3
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

- File: rest/src/main/java/com/percussion/rest/locales/LocalesResource.java:274 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 274)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:544 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=16 (max 15), cyclomatic=21 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/localeDatePattern.ts:122 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `savedLocaleDatePattern` cognitive=19 (max 15), cyclomatic=20 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:525 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isDatePatternOnlyWrite` cognitive=4 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Independent reading

Calling agent: night-issue-prs (Grok Build 1.0.70, grok-4.6). Persona erlang 0.1.1. CLI mkd-code-review 0.1.18, pack percussion, gate advisory, `--git-base origin/main`, models `models.ollama-dev-coder.toml`. No `llm.error`.

Date-pattern-only save on the existing locale PUT. `localeDatePatternWrite` sends `{ format: { datePattern } }` only (trimmed; blank clears) and returns `"unchanged"` when the stored pattern matches. `savedLocaleDatePattern` refuses a success paint when the sent body includes language/label/description/status/hasFormatProfile or other format fields, when the saved date pattern is not the one sent, or when language, label, description, status, baseLocale, or other format fields changed. Cancel does not call `updateLocale`. HTTP 400 / 403 / 409 restore the previous pattern. Create locale still hides the dedicated control. Adaptor `isDatePatternOnlyWrite` merges `datePattern` onto the existing RXLOCALEFORMAT row so time pattern, separators, and currency stay. Preexisting `paths.hardcoded_sep` hits on `LocalesResource` are JAX-RS `@Path` URL templates, not filesystem joins. Complexity findings are arborist suggestions — not behavioral bugs.

Companions: helper + Vitest, panel tests, sitemanage adaptor tests, Playwright `developer-locale-date-pattern.spec.js`, `product-docs/8.2/admin/developer-locales.md`. No filesystem path joins. No rule files.

Gate: PASS. Recommendation: approve. May merge: yes.

> Co-Authored by Grok Build 1.0.70 using grok-4.6 with agent night-issue-prs.
