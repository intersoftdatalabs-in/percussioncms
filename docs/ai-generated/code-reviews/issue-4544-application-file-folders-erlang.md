# Erlang review — #4544 application-file folder create/delete/rename

Date: 2026-09-18
Reviewer persona: Erlang (independent of implementer)
Change class: public REST adaptor surface + WebUI product screen + Playwright + product-docs

## Verdict

**Pass** for commit/PR. Hard gates addressed.

## Bugs

None remaining. Traversal is rejected before object-store I/O (`requireSafeRelativePath` → 400). Nested folder-into-self is 400. Destination exists is 409. Unknown app/path is 404. Admin is required before writes.

## Tests

- REST resource Mockito tests for POST folders / DELETE content / POST move (400/403/404/204).
- Sitemanage adaptor tests for Admin 403, traversal 400, mkdir/delete/move, nested move, recursive NIO delete on temp dirs.
- WebUI API + panel tests for create/rename/delete refresh.
- Playwright surface spec on QA H2: Admin create/rename/delete; non-Admin write denied.

## Cross-platform paths

- API keys use `/` (URL/query).
- Filesystem joins use `Path.resolve` per segment and `File.separatorChar` for object-store `File`.
- `PSPathInjectionGuard.requireUnderBase` before mkdir/delete/move/list walk.

## Companions

REST resource + DTO + adaptor interface + MainTest stub; sitemanage impl; SPA chrome; product-docs 8.2; Playwright + smoke-set entry.

## Residual (out of scope)

Design locking (#4562), binary round-trip (#4563), workflow create (#4564).
