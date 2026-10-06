<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

## Summary

Machine analysis found **2** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 20 analyzed
- In-diff: 1 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowGraphProjector.java:157 (preexisting)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `collect` cognitive=28 (max 15), cyclomatic=14 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowTransitionWriter.java:205 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `sameTypedInterval` cognitive=16 (max 15), cyclomatic=9 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

