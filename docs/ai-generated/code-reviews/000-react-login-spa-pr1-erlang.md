# Erlang review — feat/000-react-login-spa-pr1

**Date:** 2026-07-27  
**Scope:** Pure React SPA PR-1 — React Login front door + minimal SPA landing vs `origin/development`.  
**Memory patterns hit:** open redirects; XSS via bootstrap; missing behavioral tests; dual-tree JSP drift.

## Summary

PR-1 ships React Login as the product front door (`rxlogin.jsp` host), preserves classic markup as `rxlogin-classic.jsp`, and lands successful auth on thin authenticated `spa.jsp` hosts with `LandingShell`. Auth remains form POST to existing `/login`. Default `PSLoginServlet` landing is `/cm/app/spa.jsp?entry=home` (query contract, no hash). Client redirect helpers allowlist SPA entries; server-side `resolveSafePostLoginRedirect` tests updated.

## Recommendation

**approve**

## Gate

|         Check         |                                      Result                                       |
|-----------------------|-----------------------------------------------------------------------------------|
| Bugs                  | None found                                                                        |
| Behavioral unit tests | Vitest 10/10 login+landing; `PSLoginServletTest` updated for SPA default          |
| Open redirect         | Client sanitize + server resolveSafePostLoginRedirect; fragments/schemes rejected |
| Bootstrap XSS         | JSP `jsonString` escapes HTML-sensitive chars into JSON script block              |
| Cross-platform paths  | N/A (URL paths use `/` correctly)                                                 |
| May commit/push       | **yes**                                                                           |

## Issues

None (hard gate).

### Suggestions (non-blocking)

1. **Anonymous `/cm/modern/*` and `/cm/themes/*`:** Required so the public login page can load the modern bundle. Accept residual that unauthenticated clients can fetch modern JS/CSS (no secret content assumed). Document in security review if scanners flag it.
2. **`spa.jsp` dual tree** (`cm/app` + `cm/pages/app`): Aligns with design dual-tree policy; keep in lockstep on future SPA PRs.
3. **LandingShell is intentionally minimal** (PR-1 demo path); full router is out of scope.

## Test evidence

- `cd WebUI && npm test -- --run src/test/ts/login src/test/ts/app` → 10 tests, 0 failures
- `cd system && ../mvnw test -Dtest=PSLoginServletTest` + clean install (see PR body)

