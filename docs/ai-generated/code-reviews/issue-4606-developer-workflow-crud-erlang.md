# Erlang pre-commit review — fix(developer): Developer workflow create update delete (#4606)

**Branch:** `fix/issue-4606-developer-workflow-crud`
**Base:** `origin/main`
**Reviewer:** Erlang persona (opencode sub-agent)
**Scope:** Diff vs `origin/main` (15 files modified, 4 new files). Vertical increment: REST + sitemanage + WebUI + Playwright + product-docs/8.2.

## Summary

Slice 21 of #1690. Adds Admin update / delete on top of the slice 21 create surface. Vertical increment:

- `rest`: `WorkflowUpdate` DTO (Jackson `WorkflowUpdate` root wrap); `PUT /services/workflows/{idOrName}` and `DELETE /services/workflows/{idOrName}` on `WorkflowsResource` (same `mapMutationFailure` / `IllegalArgumentException → 400` / `WebApplicationException → re-throw` pattern as the existing POST and SY-06 PUT).
- `projects/sitemanage`: `WorkflowsAdaptor.updateWorkflow` (Admin check → resolve → name-mismatch check → `applyDescriptionAllowClear` follow-up save → return summary) and `WorkflowsAdaptor.deleteWorkflow` (Admin check → delegate to `IPSSteppedWorkflowService.deleteWorkflow` → map editor exceptions to 404 / 409 / 500 / 400). Renaming a separate `applyDescriptionAllowClear` keeps the create path's blank-description no-op behavior intact.
- `WebUI`: `WorkflowDetailPanel` adds an inline description editor with a Save button gated on `dirty`, and a Delete button that opens the shared `CatalogConfirmDialog`. `WorkflowsPanel.handleDeleted` reloads the catalog. `workflowsApi` gains `updateWorkflow` (PUT `WorkflowUpdate` wrap) and `deleteWorkflow` (DELETE void). `WORKFLOW_DESIGN_GAPS` shrinks — the "Workflow update / delete is not supported" gap line is removed from the catalog row, and `WorkflowDetailPanel`'s `developer-wf-gaps` fallback no longer renders `WF_GAP_WRITE`.
- `modules/perc-qa-automation`: `tests/developer-workflow-update-delete.spec.js` (SPA edit round-trip + cancel / confirm dialog + REST contract).
- `product-docs/8.2`: `admin/developer-workflows.md` (create / update / delete product paths, REST table), `developer/rest.md` (PUT / DELETE rows), `developer/index.md` (chrome summary and accessibility list).

## Recommendation

**approve**

## Gate

Open the PR.

## Issues

None blocking. Behavioral coverage on every new code path:

- `WorkflowsResourceTest`: 14 new cases (PUT/DELETE happy / 400 missing body / 400 mismatched name / 404 / 403 / 500 wrap / 503 missing adaptor).
- `WorkflowUpdateSerialDeserialTest`: 3 cases (Jackson root wrap with and without description, JAXB round-trip).
- `WorkflowsAdaptorUpdateDeleteTest`: 18 cases covering the create / update / delete delegation, blank-description vs empty-string vs null, name-mismatch, 403 non-Admin, 403 missing session, 404 missing workflow, 409 system workflow / items-still-assigned, 500 unexpected.
- `TestWorkflowsAdaptor`: stub `updateWorkflow` / `deleteWorkflow` implementations added so the Spring `ApplicationContext` test classpath continues to wire after the interface gained new methods.
- Vitest: `workflowsApi.test.ts` +11 cases (wrap, PUT URL encode, DELETE URL encode, 409 propagation). `WorkflowDetailPanel.test.tsx` +7 cases (Save disabled until dirty, PUT round-trip, 400 mismatch message, cancel confirm, delete with `onDeleted`, 409 items-in-use). `WorkflowsPanel.test.tsx` mocks updated.
- Playwright: 3 surface tests on QA H2 (`tests/developer-workflow-update-delete.spec.js`). Sibling regression: `tests/developer-workflow-create.spec.js` (3/3) and `tests/developer-workflow-content-types.spec.js` (2/2) re-run on the same cell — all green.

### Memory patterns hit

- **C2 / peer-stub update**: Whenever a public REST interface gains methods, `rest/src/test/java/com/percussion/rest/test/apibridge/TestWorkflowsAdaptor` and other `@Lazy` test stubs must implement them or the test classpath compile fails. Hit and fixed (`TestWorkflowsAdaptor` updated with `updateWorkflow` / `deleteWorkflow` no-op stubs).
- **`mapMutationFailure` vs `Exception` catch log difference**: The mutation handlers (POST / PUT / DELETE) catch `RuntimeException` via `mapMutationFailure` (no log) and fall through to `Exception` only for non-Runtime failures (which log). Mockito's checked-exception guard prevents stubbing `IOException` on the adaptor method, so the existing pattern is to assert 500 + cause without log verification for the mutation handlers. Adjusted the new 500 tests accordingly.
- **Pre-existing limitation (informational only)**: `PSSteppedWorkflowService.toPSUiWorkflow` does not copy `PSWorkflow.getDescription()` into `PSUiWorkflow`, so the `GET /services/workflowmanagement/workflows/{name}` endpoint does not surface stored descriptions. The slice 21 update surface round-trips the description via the PUT response so the SPA still reflects the saved value after a Save round-trip; the Playwright spec asserts this without depending on the workflowmanagement read carrying the description. Out of scope to fix `toPSUiWorkflow` for this slice (would change wire shape and behavior of the read catalog for sibling slices).
- **Cross-platform paths**: No new filesystem path construction. All REST / UI work uses URL paths only.

### Style

- New files (`WorkflowUpdate.java`, `WorkflowUpdateSerialDeserialTest.java`, `WorkflowsAdaptorUpdateDeleteTest.java`, `developer-workflow-update-delete.spec.js`) carry the 2026 Intersoft Apache 2.0 header.
- Pre-existing copyright lines on edited files preserved (`WorkflowDetailPanel.tsx`, `WorkflowsPanel.tsx` keep the 2026 header; the test files in `WebUI/src/test/ts/developer/` were created post-2023 and keep their existing 2026 header).
- Module `AGENTS.md` change-class completeness satisfied: REST + sitemanage + WebUI + Playwright + product-docs in the same change set.

## Notes

- 1-level Maven clean-installs verified on the touched modules before commit: `rest` (1463 tests, 0 failures), `projects/sitemanage` (2839 tests, 125 pre-existing skipped, 0 failures), `WebUI` (Vitest 4573 passed, Surefire 69 passed). `modules/perc-qa-automation` builds clean.
- C5 UI live proof: `qa-up --skip-image-build --then-qa-deploy-webui` → `qa-health` RESULT:OK (HTTP 200 / healthy on `http://127.0.0.1:9993`). Playwright surface spec 3/3 pass; create spec re-run 3/3 pass; SY-06 spec re-run 2/2 pass. `server.log`: zero ERROR/FATAL during the run.
