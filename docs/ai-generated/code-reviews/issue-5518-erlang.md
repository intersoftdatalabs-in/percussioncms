# Erlang pre-push review — issue #5518

Branch: `fix/issue-5518-locale-decimal-separator`
Base: `origin/main`
Command: `mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main --models /home/nate/workspaces/mkd-workspace/mkd-code-review/config/models.ollama-dev-coder.toml`

Persona: erlang 0.1.1 (`~/.agents/skills/erlang`)

Overnight gate: in-diff **bugs**, missing **behavioral** tests, and **non-portable** path/file I/O only. Preexisting rows do not block.

## Summary

Machine analysis found **10** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD (working tree at review time)
- Files: 9 analyzed
- In-diff: 3 finding(s); preexisting: 7
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes
- Overnight interpretation: **PASS**. In-diff items are complexity suggestions (`handleDecimalSepSave`, `handleSave`, `isDecimalSepOnlyWrite`), peer-matched to date/time-pattern-only handlers. Preexisting `paths.hardcoded_sep` on JAX-RS `@Path` strings and sibling save-handler complexity do not block.

## Issues

### Issue 1 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/locales/LocalesResource.java:155 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 155)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open
- Overnight: **not blocking** (preexisting JAX-RS URL path).

### Issue 2 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/locales/LocalesResource.java:224 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 224)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open
- Overnight: **not blocking** (preexisting).

### Issue 3 -- Severity: bug

- File: rest/src/main/java/com/percussion/rest/locales/LocalesResource.java:282 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 282)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open
- Overnight: **not blocking** (preexisting).

### Issue 4 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:589 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleDatePatternSave` cognitive=7 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:684 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleTimePatternSave` cognitive=7 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:779 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleDateTimePatternSave` cognitive=7 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:874 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleDecimalSepSave` cognitive=7 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Overnight: **not blocking**. Same shape as peer date/time-pattern saves.

### Issue 8 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/LocaleDetailPanel.tsx:943 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleSave` cognitive=16 (max 15), cyclomatic=24 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Overnight: **not blocking**. Extra dedicated-write inflight/reset lines; no new behavioral defect.

### Issue 9 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:630 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isDateTimePatternOnlyWrite` cognitive=4 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/LocalesAdaptor.java:657 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isDecimalSepOnlyWrite` cognitive=4 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open
- Overnight: **not blocking**. Conjunction of omitted format fields, peer of `isDateTimePatternOnlyWrite`.

## Behavioral tests (overnight)

- `localeDecimalSep.test.ts` — write body omits other fields; blank clears; unchanged skip; saved-or-null when language/label/description/status/grouping change.
- `LocaleDetailPanel.test.tsx` — dedicated editor save/cancel/400/403/409; grouping/date/time stay.
- `LocalesAdaptorDesignWsTest` — decimal-separator-only persist; blank clear; detector rejects extra fields / hasFormatProfile.
- `LocalesAdaptorTest` — `decimalSep` too long is 400-class validation.
- `LocalesResourceTest` — invalid `decimalSep` maps to HTTP 400.
- Playwright `developer-locale-decimal-sep.spec.js` — 1 passed on H2 QA.

## Cross-platform

No new filesystem path construction. JAX-RS `/` paths and REST format field names are URL/JSON, not OS paths.
