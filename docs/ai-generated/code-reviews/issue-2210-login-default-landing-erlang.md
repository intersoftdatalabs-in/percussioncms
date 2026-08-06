# Erlang review: issue #2210 login/app entry default landing

**Branch:** `feat/issue-2210-login-default-landing`  
**Date:** 2026-08-06  
**Reviewer:** Grok Build (self-review / Erlang gate)  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Wires `index.jsp` `getDefaultView` to map effective homepage types (from
`PSRoleService.getUserHomepage()`) to full `view=` keys and fail closed when the
target is role-gated and the user lacks Admin/Designer rights. Logic lives in
unit-testable `PSDefaultLandingView` (peer pattern to `PSProxyHostScheme`).

## Scope

- `WebUI` only (helper + tests + JSP dual-ship mirrors)
- Parent #959 slice 3; depends on #2209 / PR #2216 for user-override values on
  `getUserHomepage()` — mapping/regression works on main for role-only types
- Cross-platform path review: N/A (no new file I/O / path handling)

## Issues

None at **bug** severity.

### suggestion

1. **Stacked merge order** — full user-override landing requires #2216 merged
   first (or stacked). This PR remains safe alone: role-only Home/Dashboard/Editor
   mapping is unchanged; expanded types only appear once effective landing returns
   them.

### nit

1. `WebUI/war/app/index.jsp` is not the packaged WAR source (`src/main/webapp`);
   kept in sync per issue dual-ship note.

## Test evidence

- `PSDefaultLandingViewTest`: 11 tests, 0 failures (mapping + unauthorized
  fail-closed + role-only regression + admin/designer matrix)
- Module: `WebUI` `mvnw clean install` green

## Companions checked

- Change class: pure util extracted from JSP for unit tests (peer:
  `PSProxyHostScheme` / `PSProxyHostSchemeTest`)
- Dual-ship: `cm/app/index.jsp`, `cm/pages/app/index.jsp`, residual `war/app`
- Playwright not required for this server-side redirect helper (Admin UI surface
  is slice 4 / #2211)
- No Spring bean / rest adaptor surface change
