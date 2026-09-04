# Erlang review — issue #4302 REST Pipelines Slice B start/stop

**Branch:** `feat/issue-4302-pipelines-start-stop`  
**Base:** `origin/main`  
**Date:** 2026-09-04  
**Reviewer persona:** Erlang (pre-commit gate)

## Summary

Admin REST start/stop for classic XML Application / pipeline packages: extend
`IPipelinesAdaptor` + `PipelinesResource`, sitemanage `PipelinesAdaptor` via
`PSServer.startApplication` / `shutdownApplication`, Mockito + Spring stub +
adaptor tests, shrink `designGaps`, product-docs REST note.

## Scope

- `rest/.../pipelines/*` (interface, resource, DTOs, Mockito test, Spring stub)
- `projects/sitemanage/.../PipelinesAdaptor*` (impl + unit tests)
- `product-docs/8.2/developer/rest.md`
- Change class: public REST adaptor surface + Admin lifecycle action
- Companions checked: resource, interface, wire DTOs (`active`), apibridge impl,
  Mockito resource tests, Spring `TestPipelinesAdaptor`, sitemanage adaptor tests,
  product-docs
- Cross-platform path review: no new filesystem path joins; name safety rejects
  `/`, `\`, `..` (URL path params only)
- Memory patterns: path-injection sanitizer on app names; Admin gate; no raw
  path echo on 404; change-class Spring stub companion

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No `bug` findings. Behavioral unit tests cover Admin 403, hidden/disabled 400,
idempotent start/stop, unknown → null/404, designGaps no longer claims start/stop
unsupported. Standalone `rest` and `sitemanage` `mvnw clean install` green.

## Issues

_None (bug)._

### suggestion

1. **`PipelinesAdaptor.java` — `PSAuthorizationException` catch on start**  
   Default `PSServer.startApplication(String)` does not declare
   `PSAuthorizationException`; the catch is defensive for injectable ops.
   Harmless; keep or fold into generic `Exception` if desired later.

### nit

1. Catalog loader uses `getApplicationSummaryObjects(tok, false)` so hidden apps
   may already 404 before the explicit hidden 400 — dual defense is fine and
   documented.
