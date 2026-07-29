# Erlang review — fix/ismypage-produces-json

| Field | Value |
|-------|--------|
| **Date** | 2026-07-28 |
| **Branch** | `fix/ismypage-produces-json` |
| **Scope** | Internal `isMyPage` media type + SPA client cleanup |
| **Base** | `origin/development` (post #1570/#1571) |
| **Recommendation** | **approve** |
| **May commit/push** | **yes** |
| **Gate** | pass |

## Summary

Internal sitemanage `GET …/item/ismypage/{pageId}` was `@Produces(TEXT_PLAIN)` only, so SPA clients with `Accept: application/json` got HTTP 406. PR #1571 worked around that in the frontend. This change fixes the **API** (JSON + XML like sibling item endpoints) and removes the client Accept override. No public OpenAPI/`rest` surface is involved — sitemanage itemmanagement is internal.

## Issues

None (bugs). Behavioral tests cover Produces annotation contract and true/false membership; WebUI tests assert standard Accept and JSON boolean parsing.

## Cross-platform path checklist

N/A — no filesystem path work.

## Verification

- `cd projects/sitemanage && ../../mvnw test -Dtest=PSItemServiceIsMyPageTest` — 3 tests, 0 failures
- `cd WebUI && npm test -- --run src/test/ts/home/homeApi.test.ts` — 16 pass
- Module clean installs (sitemanage, WebUI) required before PR
