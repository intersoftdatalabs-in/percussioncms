# Erlang pre-push review — issue #6777 (percCalendarTwo apiKey heuristic)

Adjudication slice: `secrets.heuristic` on the `apiKey` DOM reads in both
percCalendarTwo editor twins — verified FP (operator-supplied key via
editor-DOM `.val()` at runtime; no credential literal in either twin),
same-line suppressions + `suppressions.md` ledger row `mkd-6777` + guard test
`PercCalendarTwoApiKeyHeuristicGuardTest`.

## Summary

Machine analysis found **3** finding(s) (3 suppressed), **0** blocking bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 3 analyzed
- In-diff: 3 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: ~/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

_No active issues._

### Suppressed Findings (3)

- `secrets.heuristic` on `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md:117` (via in_source_mkd — self-referential ledger-row hit on the mkd-6728 row text, non-blocking)
- `secrets.heuristic` on `modules/perc-packages/.../SupportFile-rx_resources/widgets/percCalendarTwo/js/percCalendarTwo.js:273` (via in_source_mkd — adjudicated FP, issue #6777)
- `secrets.heuristic` on `modules/perc-packages/.../sys__UserDependency--rx_resources/widgets/percCalendarTwo/js/percCalendarTwo.js:277` (via in_source_mkd — adjudicated FP, issue #6777)

Notes: machine + LLM stages completed; gate is clean (0 in-diff blocking
findings). No paid/OpenRouter route used.
