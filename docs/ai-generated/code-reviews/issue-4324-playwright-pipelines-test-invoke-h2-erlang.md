# Erlang review — #4324 Playwright Pipelines test-invoke H2

**Change class:** Surface-filtered H2 Playwright companion for Developer Pipelines Test invoke (+ Problems soft-assert).

**Scope reviewed:**
- `modules/perc-qa-automation/frontend/tests/developer-pipelines-test-invoke.spec.js`
- `product-docs/8.2/admin/developer-pipelines.md` (Playwright path pointer)
- `WebUI/.../pipelinesApi.ts` WRAP_ROOT execute body (C5 live finding on stacked #4323 tip)

**Stacked tip:** SPA #4323 / PR #4329 (Test invoke + Problems chrome). REST execute already on main. Validation #4322 optional (soft-unavailable accepted).

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| bug | Flat `{params}` POST fails CXF UNWRAP_ROOT_VALUE (HTTP 500 root name mismatch) | Fixed: `wrapPipelineExecuteRequestForWire` + Vitest; peer of ACL/template wrap |
| — | Spec mirrors start-stop / pipe-ir peers | console guards, exact `data-pipe-name` open, POST wait on execute, 200→result / 400|404|500→clear invoke-error, Problems soft states |
| note | Native IR success path may be rare on stock H2 | Accepted: product docs state missing IR → server error; assertion requires POST + UI reaction |
| note | Matrix image may lack `restPipelinesResource` CXF ref | C5: docker-cp `sitemanage-beans.xml` + in-cell Jetty restart (already on main) |

## Cross-platform

No new filesystem path construction; URL/`data-testid` only.

## Companions

- Peer: `developer-pipelines-start-stop.spec.js`
- C5: `qa-up` → `qa-health` → `qa-deploy-webui` (SPA tip) → `test:surface` → `qa-down`
- Module clean install: `modules/perc-qa-automation`; WebUI tip already on branch for deploy
