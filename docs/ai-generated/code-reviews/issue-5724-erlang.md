## Summary

Machine analysis found **6** finding(s), **2** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 4 analyzed
- In-diff: 2 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 2
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:239 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 239)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:316 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 316)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 3 -- Severity: bug

- File: WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:318 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 318)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 4 -- Severity: bug

- File: WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:356 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 356)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 5 -- Severity: bug

- File: WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:363 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 363)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 6 -- Severity: suggestion

- File: WebUI/src/main/java/com/percussion/webui/filter/PSWebUiSpaFallbackFilter.java:104 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `doFilter` cognitive=17 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open


## Work-agent disposition (issue #5724, 2026-11-07)

The 2 in-diff `paths.hardcoded_sep` rows (lines 316/318) are **false
positives — not fixed, by design**:

- Both lines are `RETIRED_API_DIR + "/"` inside
  `buildRetirementForwardPath`, i.e. **servlet URL-path** literals
  (`/cm/api`, `/cm/api/`), matched against
  `HttpServletRequest.getRequestURI()`-derived strings.
- Root `AGENTS.md` → **Cross-Platform File I/O & Paths**, rule 1:
  "Hardcoded `/` is correct only for **URL, URI, classpath, and ZIP
  entry** paths (those always use `/`)." Request URIs use `/` on every
  OS; rewriting them with `File.separator`/`Path` would **break Windows**
  (backslash never appears in a request URI) and diverge from every
  neighboring literal in this file (`APP_PREFIX = "/cm/app"`,
  `PAGES_PREFIX`, `CANONICAL_IMAGES`, `split("/")` — the tool flags the
  same preexisting rows 239/356/363 in this report).
- Behavioral tests are present (2 new test methods,
  `PSWebUiSpaFallbackFilterTest` 20/20 green), and C5 live proof passed
  (Playwright retirement spec 2/2 + golden/login smoke 4/4, zero console
  and server.log errors), so no missing-test gate applies either.

No code change results from this review; proceeding to PR with this
rationale in the PR body for the host Erlang phase to re-judge.
