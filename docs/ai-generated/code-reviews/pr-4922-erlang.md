## Summary

Machine analysis found **4** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 6 analyzed
- In-diff: 0 finding(s); preexisting: 4
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-workflow-transitions.spec.js:292 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 292)
- Status: open

### Issue 2 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-workflow-transitions.spec.js:311 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 311)
- Status: open

### Issue 3 -- Severity: bug

- File: modules/perc-qa-automation/frontend/tests/explorer-workflow-transitions.spec.js:408 (preexisting)
- Rule: `patterns.hard_gate`
- Tool: `patterns`
- Pattern-id: tests.empty-catch
- Description: Empty catch block swallows failures (line 408)
- Status: open

### Issue 4 -- Severity: bug

- File: WebUI/src/main/ts/contentExplorer/actionDispatch.ts:1509 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `dispatchAction` cognitive=312 (max 15), cyclomatic=196 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

