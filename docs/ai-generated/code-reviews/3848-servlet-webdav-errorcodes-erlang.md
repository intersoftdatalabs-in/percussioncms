# Erlang review — #3848 system/servlet WebDAV leftover IPS*Errors typed ErrorCodes

**Date:** 2026-08-26  
**Branch:** `fix/issue-3848-servlet-webdav-errorcodes`  
**Base:** `origin/main`  
**Reviewer:** Erlang (pre-commit, independent of implementer)

## Summary

Parent #2616 leftover slice. Convert leftover `system/servlet` production `IPS*Errors` call-sites (hooks + WebDAV methods/objectstore, plus `IPSRemoteErrors` in `PSWebdavMethod`) to typed `ServletErrorCodes` / `WebdavErrorCodes` / `RemoteErrorCodes`. Added `IPSErrorCode` constructors on `PSServletException`, `PSWebdavException`, and `PSRemoteException`. Residual allow-list shrunk by those 17 servlet paths. Dual-write skip tests cover non-auditable leftover catalogs.

## Recommendation

**approve**

## Gate

- Bugs: none found
- Behavioral tests: present (typed construction, `PSMethodFactory` unsupported-method throw, missing WebDAV config file throw, deployer config expected codes, dual-write skip)
- Cross-platform paths: pass — missing-config test uses `Path.of(System.getProperty("java.io.tmpdir"), …)` (no hardcoded `/` or `/tmp`)
- Change-class companions: exception typed ctors + production retype + allow-list shrink + dual-write tests + deployer assertion update
- May commit/push: **yes** (module `clean install` evidence recorded)

## Issues

None.

## Notes

- Servlet hooks still throw `jakarta.servlet.ServletException` with formatted bundle strings; they now format via `PSConnectionFactory.formatMessage(IPSErrorCode, Object[])`. Numeric codes and message lookup are unchanged.
- `PSWebdavException(IPSErrorCode, Object, int)` matches the legacy `(int, Object, int)` overload so `Object[]` status-code call sites keep treating the array as a single argument.
- C2: constructors are additive. Only subclass of `PSServletException` is `PSWebdavException`. No `new PSWebdavException() {` / `new PSServletException() {` sites. Reverse-dep `deployer` standalone `clean install` green (config tests compile against typed throws).
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); cross-platform temp path.
