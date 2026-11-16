## Summary

Machine analysis found **8** finding(s), **4** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 9 analyzed
- In-diff: 8 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 4
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:1118 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `makeMoment` cognitive=58 (max 15), cyclomatic=16 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:9394 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Calendar_constructor` cognitive=149 (max 15), cyclomatic=81 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:10588 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `Header` cognitive=107 (max 15), cyclomatic=32 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 4 -- Severity: bug

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:10864 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `EventManager` cognitive=336 (max 15), cyclomatic=179 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 5 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:77 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `massageOverrides` cognitive=30 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 6 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:552 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `parseFieldSpecs` cognitive=16 (max 15), cyclomatic=8 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 7 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:847 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `mergeProps` cognitive=26 (max 15), cyclomatic=10 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 8 -- Severity: suggestion

- File: modules/perc-packages/src/main/resources/Packages/perc.widget.calendar/sys__UserDependency--web_resources/widgets/calendarTwo/js/fullcalendar.js:1787 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `chunkFormatString` cognitive=19 (max 15), cyclomatic=6 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

> Tip: To suppress false positives, add `// mkd-code-review[<rule_id>] <reason>` (e.g. `// mkd-code-review[paths.hardcoded_sep] web url join`) or `// codeql[<rule_id>] <reason>` on or above the line, or add an entry in `.mkd-code-review.toml`.

## Erlang adjudication (reviewer addendum, 2026-11-16)

- The 4 `complexity.cognitive` bug rows + 4 suggestions all sit in
  `calendarTwo/js/fullcalendar.js`, a vendored **verbatim upstream
  `fullcalendar@2.9.1` rebase** (stock dist + 2 documented Percussion hunks:
  `reportEvents` → `Perc.events` deferred, `computeRange` → `Perc.range` —
  both verified still present in the rebased payload). The flagged functions
  (`makeMoment`, `Calendar_constructor`, `Header`, `EventManager`,
  `massageOverrides`, `parseFieldSpecs`, `mergeProps`, `chunkFormatString`)
  are upstream internals, not authored logic. Refactoring them would break the
  stock+2-mods byte-identity invariant proven by the new vitest
  reconstruction test (`widgetPayloadNpmParity.test.js`) and pinned by exact
  `fullcalendar: 2.9.1` manifest pins + `WidgetPayloadVendoredJsAuditTest`.
- Adjudicated **non-blocking for gating** (metric cannot be satisfied without
  destroying the audit invariant; no behavior, security, or portability defect;
  no suppression comment added deliberately to preserve byte-identity).
- No tech-debt residual filed: an issue demanding upstream-internal
  refactoring would be unactionable by construction.
- Verdict: **approve** (checks green, CodeQL clean).
