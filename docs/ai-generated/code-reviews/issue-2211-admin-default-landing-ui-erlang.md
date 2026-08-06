# Erlang review: issue #2211 Admin UI default landing + tests

**Branch:** `feat/issue-2211-admin-default-landing-ui`  
**Date:** 2026-08-06  
**Reviewer persona:** Erlang (self-review pre-commit)  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Implements parent #959 slice 4: Admin Users editor control for user default CMS landing page, wired to slice 2 REST (`GET/PUT/DELETE /user/user/homepage/{userName}`), with role-gated options, i18n keys, Vitest component tests, and Playwright companion under `modules/perc-qa-automation`.

Role Homepage is **not** removed; help text documents user override vs role resolve.

## Scope

| Path | Role |
|------|------|
| `WebUI/src/main/ts/api/client.ts` | `putPlainText` for text/plain PUT |
| `WebUI/src/main/ts/api/paths.ts` | `USER_HOMEPAGE` path |
| `WebUI/src/main/ts/api/user/userHomepageApi.ts` | API client |
| `WebUI/src/main/ts/workflowAdmin/user/landingOptions.ts` | Option matrix + role filter |
| `WebUI/src/main/ts/workflowAdmin/user/UserEditor.tsx` | UI control load/save |
| `WebUI/src/main/ts/workflowAdmin/messages.ts` | i18n keys |
| `WebUI/src/test/ts/workflowAdmin/*` | Vitest |
| `modules/perc-qa-automation/frontend/tests/bugs/bug-2211-user-default-landing.spec.js` | Playwright |

**Cross-platform path review:** no filesystem I/O or path joins in this diff (URL encode only). Clean.

**Memory patterns:** WebUI product screen → Vitest + Playwright companions present.

## Issues

### suggestion — partial save if landing API fails after user update

**Where:** `WebUI/src/main/ts/workflowAdmin/user/UserEditor.tsx` (submit handler)

User create/update is committed before landing override. If homepage API 404s (slice 2 not deployed), form shows error and does not close, but user fields already saved. Acceptable for sequential slice merge; operators see error. Prefer eventual transactional UX in a follow-up if needed — not a bug for this PR size.

### nit — Dashboard not in product select list

Product UX listed Home/Editor/Designer/Navigation/Admin. Dashboard remains role Homepage option only; user override list matches #2211 product list + role-default. Intentional.

## Gates evidence

- Behavioral unit tests: `landingOptions.test.ts`, `UserEditor.landing.test.tsx` (12 tests green with UsersSection suite)
- Playwright companion listed: `bug-2211-user-default-landing.spec.js`
- `WebUI` `mvnw clean install` green
- No non-portable paths

## Recommendation

**approve** — ship for PR.
