<!--
Copyright (c) 2026 Intersoft Data Labs, Inc.
Licensed under the Apache License, Version 2.0.
-->

# Erlang review — issue 4842 (uncommitted)

## Scope

- Persona: erlang 0.1.1
- Persona source: `/home/nate/.local/share/mkd/agents/erlang`
- Status: mkd-code-review 0.1.18, pack percussion, `--gate advisory`, `--diff` of the uncommitted 4842 slice
- Issue: https://github.com/intersoftdatalabs-in/percussioncms/issues/4842
- Branch: `fix/issue-4842-developer-copy-workflow` (HEAD `68d86eeeff`, no commits ahead of `origin/main`; the slice is uncommitted)
- Files reviewed:
  - `WebUI/src/main/ts/api/developer/workflowsApi.ts`
  - `WebUI/src/main/ts/developer/WorkflowsPanel.tsx`
  - `WebUI/src/main/ts/developer/WorkflowCopyPanel.tsx` (untracked)
  - `WebUI/src/main/ts/developer/messages.ts`
  - `WebUI/src/test/ts/developer/WorkflowsPanel.test.tsx`
  - `modules/perc-qa-automation/frontend/tests/developer-workflow-copy.spec.js` (untracked)
  - `projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowsAdaptor.java`
  - `projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowCopier.java` (untracked)
  - `projects/sitemanage/src/test/java/com/percussion/apibridge/WorkflowsAdaptorCopyTest.java` (untracked)
  - `rest/src/main/java/com/percussion/rest/workflows/IWorkflowsAdaptor.java`
  - `rest/src/main/java/com/percussion/rest/workflows/WorkflowsResource.java`
  - `rest/src/test/java/com/percussion/rest/test/apibridge/TestWorkflowsAdaptor.java`
  - `rest/src/test/java/com/percussion/rest/workflows/WorkflowsResourceTest.java`
  - `product-docs/8.2/admin/developer-workflows.md`
  - `product-docs/8.2/developer/index.md`
  - `product-docs/8.2/developer/rest.md`
- Also in the worktree, not part of the feature: `rest/mvnw` mode `100644` → `100755` (0 lines)
- `--git-base origin/main` is empty here (HEAD has no commits beyond main) and would omit the four untracked files. Analysis used a unified diff with `a/` / `b/` prefixes (`c/`/`w/` and `1/`/`2/` rewritten) so the machine pass saw all 16 feature files
- Memory: `~/.agents/skills/erlang/PATTERNS.md` and repo `erlang-review/patterns.md`
- Residual LLM: Ollama was up (`localhost:11434`). The markdown report has no `llm.error` and no extra model findings

## This-diff behavior

Developer → Workflows gains **Copy** on each catalog row. The form posts `POST /services/workflows/{idOrName}/copy` with the same `WorkflowCreate` wrap as create. A unique name saves a new workflow and opens it. A duplicate catalog name stays on the form with the existing duplicate message. Cancel does not save.

`WorkflowCopier` XML-round-trips the source (`toXML` / `fromXML`), assigns a new workflow guid, and rebinds `workflowId` on states, transitions, aging transitions, notifications, transition roles, assigned roles, workflow roles, and notification definitions. State ids and transition ids stay put so `toState` pointers still match. That is the same rebind `PSSteppedWorkflowService.createWorkflow` already uses before `saveWorkflow`. The source object is not written back. `WorkflowsAdaptorCopyTest` asserts a distinct guid, copied Draft/Submit, source ids left at 7, 409 with no save, 404, and invalid name. JDK 21 `WorkflowsAdaptorCopyTest`: Tests run: 4, Failures: 0, BUILD SUCCESS. WebUI `WorkflowsPanel.test.tsx`: 9 passed, including both new copy cases.

Change-class closure for a public workflow REST mutation plus a Developer screen: adaptor interface, resource, sitemanage implementation, Spring test stub (`TestWorkflowsAdaptor.copyWorkflow`), resource Mockito tests, adaptor behavioral test, Vitest, Playwright `developer-workflow-copy.spec.js`, and `product-docs/8.2` admin / developer index / REST. New Java, TSX, and the Playwright spec use the Intersoft 2026 Apache header. No agent rule files. No new filesystem path joins (`/` in this diff is URL or JAX-RS).

`rest/mvnw` is only a file-mode flip. Drop it from the commit so the feature diff stays the workflow copy.

## CLI stdout (`mkd-code-review analyze --format markdown`)

## Summary

Machine analysis found **2** finding(s), **1** bug(s).

## Scope

- Base: (unspecified)
- Head: (unspecified)
- Files: 16 analyzed
- In-diff: 2 finding(s); preexisting: 0
- Persona: erlang 0.1.1
- Persona source: /home/nate/.local/share/mkd/agents/erlang

## Recommendation

request-changes

## Gate

- Blocking bugs: 1
- May commit/push: yes

## Issues

### Issue 1 -- Severity: bug

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowCopier.java:70 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `rebindWorkflowId` cognitive=54 (max 15), cyclomatic=19 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

### Issue 2 -- Severity: suggestion

- File: WebUI/src/main/ts/developer/WorkflowsPanel.tsx:21 (in-diff)
- Rule: `complexity.cognitive`
- Tool: `arborist-metrics`
- Description: Function `WorkflowsPanel` cognitive=17 (max 15), cyclomatic=13 (max 15)
- Suggestion: Extract helpers, reduce nesting, use guard clauses (see CODE_STANDARDS).
- Status: open

## Erlang interpretation

The pack labels `complexity.cognitive` as a bug. This slice's hit is length, not a wrong result. `rebindWorkflowId` is a straight sequence of null-safe loops, the same shape as the editor's create rebind, and the copy test shows the persisted graph keeps the new workflow id without mutating the source. `WorkflowsPanel` was already a multi-branch catalog component; the copy branch adds one `copy:` screen. Neither finding is a defect, a missing behavioral test, or a non-portable path. Downgraded to suggestions. Advisory gate already says May commit/push: yes.

### Issue 3 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowCopier.java:70
- Description: `rebindWorkflowId` is one method at cognitive 54. Roles, notifications, and aging transitions are rebound but the unit fixture only has one state and one regular transition.
- Suggestion: Split per child list. Extend the fixture with an assigned role, a transition role, a notification, and an aging transition, and assert those workflow ids move while the source ids stay.
- Status: open

### Issue 4 -- Severity: suggestion

- File: projects/sitemanage/src/main/java/com/percussion/apibridge/WorkflowsAdaptor.java:230
- Description: Uniqueness uses `getWorkflowList()`, which omits `LocalContent`, `Standard Workflow`, and `Simple Workflow`. Create still rejects `LocalContent` / `Local Content` inside `validateWorkflowName`. Copy does not, so a new name of `LocalContent` is not a 409. The insert uses a new guid, so the hidden row is not overwritten. Catalog-visible names still 409 and skip `saveWorkflow` (covered by `copy_duplicateNameDoesNotSave`).
- Suggestion: Run the same system-name guard create uses before save.
- Status: open

### Issue 5 -- Severity: nit

- File: rest/mvnw
- Description: Unrelated mode change `100644` → `100755`, no content change.
- Suggestion: Restore the mode before commit.
- Status: open

Copy does not duplicate staging-role rows or allowed content types. The issue and the admin doc limit the copy to steps and transitions, and a blank description keeps the source text. That matches the code.

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: yes

Gate: PASS
May commit: yes
