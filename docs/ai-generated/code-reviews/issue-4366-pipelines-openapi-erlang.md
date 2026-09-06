# Erlang review — issue #4366 Pipelines Slice C OpenAPI

**Scope:** uncommitted `feat/issue-4366-pipelines-openapi` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** adaptor + TestXxxAdaptor Spring stub; generic 404/400 without path echo; vertical REST+SPA+Playwright+product-docs; H2 QA C5 sequence.

## Summary

Adds `GET /services/pipelines/{idOrName}/openapi` (YAML default / JSON) generated from native or classic-imported IR resources, Developer Pipelines view/download chrome, Vitest, H2 Playwright, and product-docs.

## Gate

- Behavioral unit tests: generator, resource, adaptor, pipelinesApi, PipelineDetailPanel.
- Spring `TestPipelinesAdaptor` implements the new interface method (shared rest context).
- Unknown → 404, unsafe → 400, hidden → 400; messages do not echo raw path ids.
- Cross-platform path checklist: no filesystem path joins; download basename strips separators; IR names are catalog-trusted segments. **Clean.**

## Issues

None blocking.

## C5

Playwright `developer-pipelines-openapi.spec.js` 1/1 on H2 qa-up cell after jar + WebUI hot-deploy. Console-clean; server.log ERROR/FATAL empty for pipelines/openapi.
