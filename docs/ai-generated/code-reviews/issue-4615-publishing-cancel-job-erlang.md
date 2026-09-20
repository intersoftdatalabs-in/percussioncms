# Erlang review — issue #4615 PublishingShell cancel/stop publish job

**Scope:** uncommitted branch `fix/issue-4615-publishing-cancel-job` vs `HEAD` (vs `origin/main`).
**Recommendation:** approve
**Gate:** May commit/push: yes
**Memory patterns hit:** confirm-before-mutate; POST matches JAX-RS; UI+Playwright+product-docs companions; no GET for mutating stop.

## Summary

Vertical increment: confirm-gated Stop on running jobs only; client uses POST `publishmanagement/servers/stopPublishing/{jobId}` (was GET, which did not match `PSPubServerRestService`). Shared `isJobStoppable`. Vitest, Playwright surface, product-docs.

## Issues

None blocking.

## Cross-platform path checklist

N/A filesystem joins. URL paths use `/`. Playwright helpers join URL with `/` for HTTP only.

## Tests

Behavioral: `jobStop.test.ts`, `statusStopJob.test.tsx`, `siteWorkspaceStopJob.test.tsx`, `serversApi.stop.test.ts`, Playwright + node helper tests.
