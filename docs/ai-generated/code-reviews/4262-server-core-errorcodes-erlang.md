# Erlang review — #4262 system server core IPS*Errors → *ErrorCodes

**Date:** 2026-09-03  
**Branch:** `fix/issue-4262-server-core-errorcodes`  
**Scope:** uncommitted changes vs `origin/main` for issue #4262 (parent #2616 leftover)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Cross-platform path checklist:** N/A (no new filesystem path construction; HTTP/status/error-catalog retype only)

## Summary

Retypes allow-listed production `IPS*Errors` call-sites in system server core
(`PSServer`, `PSRequest`/`PSResponse`/`PSBaseResponse`, `PSApplicationHandler`,
`PSInternalRequest`, `PSFileRequestHandler`, `PSServerLogHandler`, `PSUserSession`,
`job/PSJobHandler`, `PSLoginServlet`) to typed `*ErrorCodes`. Already-typed
`PSJobHandlerConfiguration` / `PSJobRunnerFactory` are removed from the residual
allow-list. Dual-write skip asserted for non-auditable catalogs; auditable
security auth codes remain dual-write eligible. Slice unit test + allow-list
pytest gate updated. Standalone `cd system && ../mvnw.cmd clean install` green.

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Notes

- HTTP status sites use `HttpErrorCodes.*.numericCode()` matching prior #4153 peers.
- Exception sites use typed `IPSErrorCode` constructors where available.
- Product docs N/A (internal error-catalog retype).
- C2 reverse-deps N/A (no `final`/`sealed` or signature changes).

Memory patterns hit: leftover IPS*Errors retype + dual-write skip tests; shrink exact allow-list paths only.
