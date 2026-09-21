# Erlang review — issue #4675 PublishingShell stop in-flight job

Status: cli-unavailable, manual review (`mkd-code-review` not on PATH; persona erlang 0.1.1)

## Summary

Vertical increment on existing stop/cancel (#4615): map HTTP 403/404/409 from `stopPublishing` to operator error chrome; Status/SiteWorkspace refresh only on success; Playwright 403 path; product-docs.

## Scope

- Persona: erlang 0.1.1
- Pack: percussion (manual)
- Files: `jobStop.ts` (`mapJobStopError`), Status/SiteWorkspace, i18n keys, Vitest, Playwright, `product-docs/8.2/admin/publishing.md`
- Prior: `docs/ai-generated/code-reviews/issue-4615-publishing-cancel-job-erlang.md`
- C2: no public Java API / final type changes

## Recommendation

approve

## Gate

pass — no bugs, behavioral tests for mapper + 403/409 UI, no filesystem path I/O

## Issues

None material.
