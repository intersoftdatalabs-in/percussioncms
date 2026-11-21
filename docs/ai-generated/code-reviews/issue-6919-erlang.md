# Pre-push local code review — issue #6919 slice 1 (worker adjudication)

> Tool: `mkd-code-review analyze --pack percussion --format markdown
> --gate advisory --git-base origin/main` (local Ollama pool; the LLM
> stage was unavailable — `llm.error`/skipped — so this is a machine-only
> result, disclosed per the work contract). Raw CLI output piped through
> `scripts/sanitize-erlang-review.py`.
>
> **Worker verdict: PASS (advisory).** Machine analysis reported 44
> findings (14 bug-severity), and **every one** targets the stock vendored
> FullCalendar 6.1.15 dist bytes — **zero findings on authored code**
> (snippet, surface/parity/Playwright tests, audit test, manifests,
> product-docs):
>
> - Issue 1 (`paths.hardcoded_sep`, `gcal.js:72`): **verified false
>   positive**. The flagged line builds the Google Calendar v3 API request
>   URL (`apiBase + '/' + encodeURIComponent(...)`). Institutional memory
>   (`PATTERNS.md` secret-scanning FP guards) and the root `AGENTS.md`
>   cross-platform rules both state URL paths correctly use `/`. An inline
>   suppression inside the payload is impossible — it would break the
>   byte-identity acceptance — so the adjudication is recorded here.
> - Issues 2–44 (complexity metrics + one empty-catch, all inside the stock
>   `fullcalendar.js` Standard Bundle): **non-actionable vendored
>   upstream**. Byte-identity with the npm 6.1.15 dists is the slice
>   acceptance (`Fixes #6944` requires it); refactoring upstream internals
>   is forbidden by the issue. Same precedent as every prior
>   npm-migration slice (lightbox/slick/superfish/MiniColors ship stock
>   dists). Behavioral coverage stands in for structural review:
>   8-test v6 surface lock, byte-identity parity test, and the hermetic
>   Playwright browser proof with zero console errors.
>
> No BLOCK: nothing to fix before the PR.

---

## Summary

Machine analysis found **44** finding(s), **14** blocking bug(s). LLM skipped (hard_pattern)

## Scope

- Base: origin/main
- Head: HEAD
- Files: 14 analyzed
- In-diff: 44 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 14
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/gcal.js:72 (in-diff)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 72)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:9 (in-diff)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 9)
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:9 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `g$2` cognitive=54 (max 15), cyclomatic=36 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:9 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `H$1` cognitive=45 (max 15), cyclomatic=42 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:9 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `M` cognitive=62 (max 15), cyclomatic=55 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:9 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `O` cognitive=53 (max 15), cyclomatic=48 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:1646 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `mergeProps` cognitive=34 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:4261 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `sliceEventStore` cognitive=38 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 9 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:6486 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isInteractionPropsValid` cognitive=34 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 10 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:7801 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `createPlugin` cognitive=31 (max 15), cyclomatic=32 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 11 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:8434 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `parseSection` cognitive=47 (max 15), cyclomatic=29 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 12 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:10528 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `constructor` cognitive=38 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 13 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:11063 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `constructor` cognitive=75 (max 15), cyclomatic=30 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 14 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:11404 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `constructor` cognitive=31 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 15 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:9 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `z$1` cognitive=21 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 16 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:374 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `parseFieldSpecs` cognitive=16 (max 15), cyclomatic=8 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 17 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:504 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `parseObject` cognitive=9 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 18 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:914 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `memoizeArraylike` cognitive=27 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 19 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:946 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `memoizeHashlike` cognitive=18 (max 15), cyclomatic=7 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 20 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:1123 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `postProcess` cognitive=18 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 21 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:2417 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `render` cognitive=27 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 22 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:3244 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `parseSingle` cognitive=29 (max 15), cyclomatic=22 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 23 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:3535 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `reduceEventStore` cognitive=4 (max 15), cyclomatic=17 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 24 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:3903 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `setProp` cognitive=27 (max 15), cyclomatic=7 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 25 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:4173 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `toPlainObject` cognitive=21 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 26 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:4402 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `buildSegTimeText` cognitive=15 (max 15), cyclomatic=18 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 27 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:4998 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `addEvent` cognitive=20 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 28 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:5848 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `findInsertion` cognitive=19 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 29 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:6555 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `isDateSelectionPropsValid` cognitive=16 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 30 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:6646 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `render` cognitive=24 (max 15), cyclomatic=23 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 31 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:6916 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `render` cognitive=15 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 32 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:8068 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `buildViewSpec` cognitive=28 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 33 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:8197 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `reduceEventSources` cognitive=6 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 34 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:8662 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `parse` cognitive=13 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 35 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:9194 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `processRawViewOptions` cognitive=22 (max 15), cyclomatic=11 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 36 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:10396 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `handleMove` cognitive=17 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 37 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:10459 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `computeBestEdge` cognitive=25 (max 15), cyclomatic=27 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 38 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:10957 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `constructor` cognitive=26 (max 15), cyclomatic=15 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 39 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:11569 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `constructor` cognitive=18 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 40 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:11645 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `constructor` cognitive=25 (max 15), cyclomatic=12 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 41 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:12224 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `computeFgSegPlacement` cognitive=22 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 42 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:12323 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `placeRects` cognitive=17 (max 15), cyclomatic=7 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 43 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:12535 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `renderFgSegs` cognitive=19 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 44 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:14204 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `buildTimeContent` cognitive=21 (max 15), cyclomatic=7 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.
