# Erlang review — issue #4396 Pipelines Slice D request tracing

**Date:** 2026-09-09  
**Branch:** `fix/issue-4396-pipelines-request-tracing`  
**Persona:** independent of implementer.

## Change class

Native pipeline **request tracing** vertical slice: persist tracing on/off, record last-trace (stages + timings) on execute, fail-closed sanitizer, Developer chrome, Playwright, product-docs.

## Companions checked

| Artifact | Status |
|----------|--------|
| system IR `app.tracingEnabled` + runtime last-trace store | added |
| Fail-closed sanitizer (password/token/Authorization) | added + unit tests |
| rest `IPipelinesAdaptor` + `PipelinesResource` PUT/GET tracing + GET lastTrace | added |
| Spring `TestPipelinesAdaptor` stub | updated |
| sitemanage `PipelinesAdaptor` + unit tests | updated |
| WebUI API wrap/unwrap + `PipelineDetailPanel` Trace on/off + last-trace panel | added |
| Playwright `developer-pipelines-request-tracing.spec.js` | added (no package.json lockfile churn) |
| product-docs 8.2 REST Pipelines + admin Developer Pipelines | updated |

## Findings

**Bugs:** none remaining for this slice.

**Portable paths:** no new filesystem path concatenation. IR persist uses existing `IPSPipelineIrService` store. Last-trace is in-memory.

**Secrets:** last-trace copies only sanitized `params`. Result rows are not copied. Sensitive keys and Bearer/Basic values become `[REDACTED]`.

**Tests:** system sanitizer + runtime tracing; rest resource; sitemanage adaptor; WebUI vitest; Playwright C5 on QA H2.

## Verdict

Pass for commit/PR after C1 module installs and C5 Playwright.
