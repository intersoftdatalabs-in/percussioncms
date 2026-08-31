# Erlang review — issue #4047 SPA CD-11 content type icon strategy

| Field | Value |
|-------|-------|
| **Date** | 2026-08-31 |
| **Branch** | `feat/issue-4047-content-type-icon-strategy` |
| **Issue** | #4047 (parent #1690, FR CD-11) |
| **Recommendation** | approve |
| **Gate** | May commit/push: yes |
| **Memory patterns hit** | Incomplete change-class closure (WebUI + Playwright + product-docs); behavioral tests for 400/409/none |

## Summary

Developer SPA Content Type Properties **Icon strategy** (`none` / `specified` / `fromFileField`) on existing REST `GET`/`PUT /services/contenttypes/{id}/icon` (#3997). After a held design lock, Save writes the dedicated PUT (does not unlock). `none` clears value; blank non-none is 400 before fetch; unlocked PUT 409 is surfaced and clears the held-lock chrome. No binary upload. REST is not re-implemented.

Companions from CD-13 enabled / CD-09 item-exits peers: API wrap/unwrap (`ContentTypeIcon` Jackson root), detail chrome after lock, Vitest (API + panel), Playwright surface spec, product-docs 8.2 admin + REST.

## Scope

- `git status` / `git diff` vs worktree `feat/issue-4047-content-type-icon-strategy` (uncommitted at review time)
- Prior REST report: `docs/ai-generated/code-reviews/issue-3997-rest-cd11-icon-strategy-erlang.md`
- Pattern memory: `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None that block.

- **suggestion** `WebUI/src/main/ts/api/developer/contentTypesApi.ts` — `contentTypeIconClientError` throws a plain `{ status, statusText, body }` object (same shape as existing `ApiError` mocks). Matches `isApiError` / `panelErrMsg`. Keep as peer to 409 panel tests; do not wrap as `Error` unless the client type changes.

## Cross-platform path review

N/A for filesystem I/O. Icon `value` is a REST string (file path/name or field name). Tests assert those strings; they do not join OS paths. Playwright URL paths correctly use `/` (URI).

## C2 / change-class

- No Java `final`/`sealed` or public REST signature changes (out of scope: REST #3997).
- Downstream: none (SPA + Playwright + product-docs only).
- WebUI change-class: API client, Properties chrome, Vitest, Playwright, product-docs 8.2.

## Tests / builds

- Focused Vitest: contentTypesApi + ContentTypeDetailPanel + DeveloperShell + ContentTypesPanel + messages.i18n — 179 passed
- Standalone `WebUI` `mvnw clean install`: BUILD SUCCESS (Tests 3263)
- Standalone `modules/perc-qa-automation` `mvnw clean install`: BUILD SUCCESS (npm ci; Playwright is C5 live)
- C5: `perc-devctl qa-up --skip-image-build --then-qa-deploy-webui` → `TEST_CMS_URL=http://127.0.0.1:9993`; `qa-health` RESULT:OK HTTP:200 HEALTH:healthy; `npm run test:surface -- --path tests/developer-content-type-icon-strategy.spec.js` **4 passed**; console-clean=yes; server.log-clean=yes (login/save INFO only; percPage editor reinit)
