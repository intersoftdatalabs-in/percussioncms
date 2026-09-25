# Erlang review — #4861 EditorHost change item workflow (re-review)

**Date:** 2026-09-25
**Branch:** `fix/issue-4861-editor-change-workflow` vs `origin/main` (HEAD `b658968a9f`) plus working tree
**Reviewer persona:** erlang 0.1.1 (`~/.agents/skills/erlang/ERLANG.md`)
**Prior report:** this file (first pass, Gate BLOCK on missing `itemWorkflowApi.test.ts` unwrap/GET/POST)

This pass reads only `describe("itemWorkflowApi workflow change (#4861)")` in `WebUI/src/test/ts/contentExplorer/itemWorkflowApi.test.ts` (working tree) to close the previous hard-gate miss.

## Summary

The previous BLOCK was missing behavioral tests for `unwrapItemWorkflowChoices` / `getItemWorkflowChoices` / `changeItemWorkflow`. The working tree now has a dedicated `#4861` describe with Jackson unwrap, GET `allowedWorkflows`, POST `changeWorkflow`, and blank-id-without-POST cases, plus `client.post` reset in `beforeEach`. That is the peer companion the first pass required. Remaining findings from the first pass are suggestions and nits only. Recommendation: approve.

## Scope

| Path | Change |
|------|--------|
| `WebUI/src/test/ts/contentExplorer/itemWorkflowApi.test.ts` | New `describe("itemWorkflowApi workflow change (#4861)")` — unwrap, GET, POST, blank-id |

**In scope this pass:** the `#4861` describe block only (hard-gate re-check).

**Out of scope this pass:** EditorHost production, Playwright, product-docs, uncommitted `PSItemWorkflowService.java`. First-pass Issues 2–6 are carried as non-blocking.

## Recommendation

**approve**

## Gate

**PASS** — previous **bug** (missing unwrap/API behavioral tests) is closed. Open items are suggestions and nits.

**May commit/push: yes**

## Cross-platform path review

Unchanged from first pass. Tests assert REST URL construction (`PATHS.ITEM_WORKFLOW_ALLOWED`, `PATHS.itemWorkflowChange`) which correctly use `/`.

## Change-class closure

Change class: **EditorHost product-screen workflow action** (catalog unwrap + POST + chrome).

| Companion | Status |
|-----------|--------|
| Production host + panel + gate + i18n | yes (first pass) |
| REST client + `PATHS` | yes (first pass) |
| Vitest EditorHost success/403 | yes (first pass) |
| Vitest `canChangeEditorWorkflow` | yes (first pass) |
| Vitest Jackson/JAXB unwrap + GET/POST client (`itemWorkflowApi.test.ts` peer) | **yes — this pass** |
| Playwright screen spec | yes (first pass; thinner than peer — suggestion) |
| `product-docs/8.2/getting-started/index.md` | yes (first pass) |

## Hard-gate close (former Issue 1)

`describe("itemWorkflowApi workflow change (#4861)")` now covers:

| Case | Test |
|------|------|
| Jackson `{ ItemWorkflowChoices: { …, choices: { ItemWorkflowChoice: […] } } }` unwrap | `unwraps a Jackson ItemWorkflowChoices envelope` |
| GET `PATHS.ITEM_WORKFLOW_ALLOWED` + catalog unwrap | `getItemWorkflowChoices loads allowedWorkflows and unwraps the catalog` |
| POST `PATHS.itemWorkflowChange` + `ItemStateTransition` unwrap | `changeItemWorkflow posts the id and unwraps the new transitions` |
| Blank workflow id throws; `client.post` not called | `changeItemWorkflow rejects a blank id without posting` |
| `client.get` and `client.post` reset | `beforeEach` |

WRAP_ROOT miss on live H2 would fail the unwrap and GET cases. Wrong POST path or missing transition unwrap would fail the POST case. Blank-id POST would fail the reject case.

## Issues (non-blocking, carried from first pass)

### Issue 2 — Severity: suggestion

- File: `WebUI/src/main/ts/editor/EditorHost.tsx:1347`
- Description: `handleChangeWorkflow` stringifies errors with `err instanceof Error ? err.message : String(err)`. Production `post()` throws a plain `ApiError` object. Live 403 can append `[object Object]`. Vitest 403 throws `new Error("HTTP 403")`.
- Suggestion: `formatApiError` + `isSessionRedirectError`; drive 403 Vitest through an `ApiError`-shaped throw.

### Issue 3 — Severity: suggestion

- File: `WebUI/src/test/ts/editor/EditorHost.test.tsx:2299`
- Description: Host tests cover POST success and HTTP 403. They do not click **Save workflow** with a blank selection or the current id.
- Suggestion: Empty picker and current `"4"` cases asserting no POST and no **Workflow changed**.

### Issue 4 — Severity: suggestion

- File: `modules/perc-qa-automation/frontend/tests/editor-host-change-workflow.spec.js:132`
- Description: Spec is thinner than `editor-host-workflow-transitions.spec.js` (`editorSpaUrl`, leftover listener, a11y, recorded URL).
- Suggestion: Match the peer helper contract; title the first test for save/success only.

### Issue 5 — Severity: nit

- File: `WebUI/src/main/ts/editor/EditorHost.tsx:1340`
- Description: After a successful change, the host applies the POST `ItemStateTransition` and does not call `checkout` again. `handleTransition` re-checkouts after success.
- Suggestion: After a successful `changeWorkflow`, call the existing `checkout` seam the same way `handleTransition` does.

### Issue 6 — Severity: nit

- File: `modules/perc-qa-automation/frontend/tests/editor-host-change-workflow.spec.js:126`
- Description: Test title is `saves a new workflow and rejects a forbidden id`. The body only saves.
- Suggestion: Title the first test for the save/success assertions only.

## Notes (non-blocking)

- Flat DTO, single nested `ItemWorkflowChoice` object, numeric-id coercion, and blank *item* id skip-GET were in the first-pass suggestion list. The four cases above close the WRAP_ROOT / GET / POST / blank-id miss. Extra unwrap shapes remain optional coverage, not a gate.
- Uncommitted `PSItemWorkflowService.java` is still outside this EditorHost API-test re-review.

## Tests in the diff (this pass)

- `itemWorkflowApi.test.ts` — `describe("itemWorkflowApi workflow change (#4861)")`: Jackson unwrap, GET catalog, POST change + transition unwrap, blank-id without POST

---

> Co-Authored by Grok Build Grok 4.6 using grok-4.6 with agent erlang.
