## Summary

Machine analysis found **8** finding(s), **4** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 8 analyzed
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

---

## Author adjudication (not part of CLI output — work sub-agent, 2026-11-16)

Issues 1–4 (`complexity.cognitive` on `makeMoment`, `Calendar_constructor`,
`Header`, `EventManager` in the rebased `calendarTwo/js/fullcalendar.js`)
are **adjudicated false-positive for gating**:

- The flagged functions are verbatim upstream `fullcalendar@2.9.1` npm-dist
  code, not authored logic. The rebase keeps them byte-identical to the
  stock dist (proven by the new vitest reconstruction test); only the
  banner expansion and the two documented Percussion hunks
  (`reportEvents` → `Perc.events` deferred, `computeRange` → `Perc.range`)
  differ, and neither hunk raises complexity.
- Refactoring upstream internals to satisfy the metric would break the
  stock+2-mods invariant the audit relies on (future `npm audit` /
  Dependabot bumps would no longer be verifiable by reconstruction) and
  risks breaking the published calendar widget for zero benefit.
- Inline `// mkd-code-review[…]` suppression is deliberately not used:
  any comment added to the payload would break the byte-identity proof
  against the npm dist.
- Coverage in place of the metric: exact `fullcalendar: 2.9.1` pins in
  both WebUI frontend manifests (Dependabot-visible), reconstruction
  byte-identity in `widgetPayloadNpmParity.test.js`, expanded-banner plus
  `Perc.events`/`Perc.range` pins in `WidgetPayloadVendoredJsAuditTest`
  (including the new `calendarTwoFullcalendarCarriesPercussionMods` test).

Issues 5–8 are non-blocking suggestions on the same upstream bundle; same
adjudication applies. No in-diff authored JS/TS, no missing behavioral
tests (reconstruction + marker + parse tests added), no non-portable
paths (payloads stay LF per repo `.gitattributes`; path handling in tests
uses `path.join`). No CodeQL-relevant sinks touched.

Gate stance: proceeding to PR with this adjudication recorded; host Erlang
phase holds final approve/merge authority.
