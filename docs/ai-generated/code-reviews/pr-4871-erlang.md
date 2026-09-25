<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — PR 4871

Independent review of `fix/issue-4861-editor-change-workflow` at `f59affb87a` against `origin/main`. Reviewer did not author the change.

```
mkd-code-review analyze --pack percussion --format markdown --gate advisory --git-base origin/main
```

## Summary

Machine analysis found **3** finding(s), **0** bug(s).

## Scope

- Base: origin/main
- Head: HEAD
- Files: 18 analyzed
- In-diff: 2 finding(s); preexisting: 1
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemWorkflowService.java:956 (preexisting)
- Rule: `paths.hardcoded_sep`
- Tool: `paths.hardcoded_sep`
- Pattern-id: paths.hardcoded-sep
- Description: Possible non-portable path construction (line 956)
- Suggestion: Use Path/PathBuf, path.join, File.separator, or pathSeparator — not literal / or \ joins.
- Status: open

### Issue 2 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemWorkflowService.java:413 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `allowedWorkflows` cognitive=17 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/itemmanagement/service/impl/PSItemWorkflowService.java:464 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `changeWorkflow` cognitive=25 (max 15), cyclomatic=25 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Intent

In-diff findings are complexity suggestions, not behavior bugs. The line 956 path finding is preexisting and outside this slice. `ItemWorkflowAssignmentRules` rejects blank, malformed, unchanged, and unassociated ids, and `changeWorkflow` returns 400/403 before `changeWorkflowForItem`. EditorHost clears the success flag on HTTP failure. Assignment-rule tests cover the reject paths. Checkout restore after a successful workflow change is best-effort (warn, then `getTransitions`); that is not a false success for the workflow mutation itself. No blocking bug.
