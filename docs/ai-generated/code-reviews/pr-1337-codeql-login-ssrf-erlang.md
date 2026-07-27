# Erlang review — PR #1337 CodeQL login redirect + SSRF residual

**Date**: 2026-07-18  
**Branch**: `989-react-cui-widget-builder`  
**Scope**: uncommitted changes for CodeQL PR check failure (2 new alerts)

## Summary

PR CodeQL check reported:

1. **Critical** `java/ssrf` on `PSDocumentUtils` (#1847) — residual thrash after development merge of PR #1364 (`Redirect.NEVER`). Runtime defense already present; not introduced by feature work.
2. **Medium** `java/unvalidated-url-redirection` on `PSLoginServlet.sendRedirect` — introduced by our Jetty path-normalization change that only called `sanitizeRedirectPath` (not an open-redirect barrier).

## Disposition

|       Finding       |                                                    Action                                                     | Status |
|---------------------|---------------------------------------------------------------------------------------------------------------|--------|
| #1847 SSRF          | Extended sink-line `// codeql[java/ssrf]`; suppressions.md row; API dismiss as false positive                 | Done   |
| Login open-redirect | Runtime fix: `resolveSafePostLoginRedirect` → `PSRedirectValidation` + URI rebuild; sink-line residual; tests | Done   |

## Gate

**Recommendation**: approve

**Gate**: pass

- Behavioral tests added and green (`PSLoginServletTest` 5/5)
- No non-portable path I/O
- No secrets
- Ladder order honored (runtime fix before suppress/dismiss)

## Issues

None blocking.

### Notes (non-blocking)

- `PSServer.getProperty(key, null)` throws (`defaultValue` must be non-null); fixed to `""`.
- `isValidRedirectUri` still accepts all URIs when behind proxy (pre-existing); post-login validation now rejects external hosts at `sendRedirect` time.
- Relative app entry points (`index.jsp`, legacy mainpage) allowed without leading `/` by explicit relative rules (not via `validateInternalRedirectUrl`).

## Tests run

```text
./mvn-env.sh -pl system -Dtest=PSLoginServletTest -Dai.integrity.skip=true test
# Tests run: 5, Failures: 0, Errors: 0
```

