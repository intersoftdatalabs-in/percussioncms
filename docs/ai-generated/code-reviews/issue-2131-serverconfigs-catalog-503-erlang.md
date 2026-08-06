# Erlang review: issue #2131 serverconfigs catalog 503

**Date:** 2026-08-06  
**Branch:** `fix/issue-2131-serverconfigs-catalog-503`  
**Reviewer persona:** Erlang (pre-commit gate)  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Aligns `ServerConfigsResource.requireAdaptor()` with catalog peers (`ControlsResource`,
`KeywordsResource`, `SlotsResource`): missing adaptor is **503 Service Unavailable** via
`WebApplicationException`, not `IllegalStateException` wrapped as 500. OpenAPI documents 503.
`ServerConfigsResourceTest` hardened to the peer ladder (delegate, null-safe, 500 wrap, WAE
rethrow, 503 bare resource on list + get).

## Scope

| Item | Value |
|------|--------|
| Files | `rest/src/main/java/com/percussion/rest/serverconfigs/ServerConfigsResource.java` |
| | `rest/src/test/java/com/percussion/rest/serverconfigs/ServerConfigsResourceTest.java` |
| Base | `origin/main` |
| Prior report / memory | Peer slice C5-style cecontrols (#2130) pattern; catalog requireAdaptor→503 |
| Cross-platform path review | N/A — no file I/O or path handling in diff |

## Issues

None (no `bug`, no missing behavioral tests for changed logic, no path concerns).

### Notes (not gate-blocking)

- **suggestion (out of scope):** Live QA `GET /services/serverconfigs` 2xx and any
  `@Lazy` / `IPSSystemService` wiring remain residual without stack evidence (per issue body).
- **nit:** `listConfigs` does not have a dedicated “rethrow WAE from adaptor” test; peer
  `ControlsResourceTest` also only asserts rethrow on get; list bare-resource 503 covers the
  requireAdaptor path used by list.

## Verification

- `cd rest && ../mvnw.cmd clean install` — exit 0
- `ServerConfigsResourceTest`: Tests run: 9, Failures: 0, Errors: 0, Skipped: 0

## Change-class companions

| Companion | Status |
|-----------|--------|
| Resource `requireAdaptor` → 503 | Done |
| OpenAPI 503 responses | Done |
| Mockito resource unit tests (peer ladder) | Done |
| Rest Spring `TestServerConfigAdaptor` stub | Pre-existing; unchanged |
| sitemanage `ServerConfigAdaptor` rework | Out of scope without live stack |

## Operator

Co-Authored by Grok Build using grok-4.5 with agent main.
