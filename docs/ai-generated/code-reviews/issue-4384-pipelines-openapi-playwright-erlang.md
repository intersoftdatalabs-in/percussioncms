# Erlang review — issue #4384 pipelines OpenAPI Playwright

## Summary

Cycle Verify residual of Slice C OpenAPI (#4366 / PR #4383). Playwright
`developer-pipelines-openapi.spec.js` failed on H2 QA because (1) skip-image-build
cells often ship a stale SPA and (2) ranking the first `sys_cmp*` app opened a
single-dataset Jackson object that made `dataSets.map` throw, so the Developer
section error boundary hid OpenAPI chrome. This change unwraps singular
`dataSets` / IR `resources` lists, ranks Playwright toward `sys_cmp*` IR apps
(not `sys_ActionPage`), and documents execute-path selection.

## Scope

Uncommitted + branch `fix/issue-4384-pipelines-openapi-playwright` vs `origin/main`.

- `WebUI/src/main/ts/api/developer/pipelinesApi.ts`
- `WebUI/src/main/ts/developer/PipelinesPanel.tsx`
- `WebUI/src/test/ts/api/developer/pipelinesApi.test.ts`
- `WebUI/src/test/ts/developer/PipelinesPanel.test.tsx`
- `modules/perc-qa-automation/frontend/tests/developer-pipelines-openapi.spec.js`
- `modules/perc-qa-automation/frontend/tests/helpers/developer-pipelines-openapi-surface.js`
- `modules/perc-qa-automation/frontend/tests/unit/developer-pipelines-openapi-surface.test.js`
- `modules/perc-qa-automation/frontend/package.json`
- `product-docs/8.2/admin/developer-pipelines.md`

Memory patterns: Jackson singular-child vs array (WRAP_ROOT); stale
`cm/modern` skip-image-build SPA; Playwright must not require gated chrome on
the first catalog row.

## Recommendation

approve

## Gate

May commit/push: yes

Cross-platform path review: Playwright REST URL helper uses `URL` origin +
services path (`/` is URL, not filesystem). Download filename assertion rejects
`[\\/]`. Unit tests reject `C:\\` and `/tmp` as OpenAPI GET URLs. No new OS
path joins.

## Issues

None (bug).

### suggestion

`retainCatalogOnListError` is a small helper covering a follow-up list GET
failure; the Playwright gate is the Jackson list unwrap + ranking. Acceptable
companion for the catalog remount we saw during diagnosis.

## Prior report / Memory patterns hit

- Jackson XML/JSON singular element as object (catalog detail `.map` throw)
- QA `qa-up --skip-image-build` without matching `qa-deploy-webui` + WAR jars
  leaves Slice C OpenAPI REST/SPA missing
