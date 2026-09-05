# Erlang review — #4323 SPA Pipelines test-invoke + problems

**Scope:** WebUI Developer Pipelines detail Admin **Test invoke** (POST execute) and
**Problems** soft-detect of Admin GET validation; product-docs; Vitest.
**Out of scope:** Playwright H2 (#4324), IR write, start/stop/IR chrome redo, CXF
(restPipelinesResource already registered).

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| none | Client JSON parse rejects non-objects before POST | OK |
| none | Validation 404 → soft-empty; 403/other → explicit chrome | OK |
| none | Admin-only invoke/problems; non-Admin hides sections | OK |
| none | Cross-platform: no filesystem path I/O | N/A |
| note | C5 Playwright deferred to sibling #4324 (issue out of scope) | Documented |

## Companions

- API: `executeResource`, `getApplicationValidation`, unwrap helpers + types
- UI: `PipelineDetailPanel` Test invoke + Problems
- Vitest: pipelinesApi + PipelineDetailPanel (+ shell/panel mocks)
- product-docs: `admin/developer-pipelines.md`, `developer/rest.md`
- CXF: no change (bean already present)

## Verdict

**Pass** for commit/PR of this slice (Playwright proof owned by #4324).
