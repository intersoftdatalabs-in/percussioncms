# Erlang review: fix/issue-2027-tlsutils-javac-warnings

**Date:** 2026-08-07  
**Branch:** fix/issue-2027-tlsutils-javac-warnings  
**Issue:** #2027 (parent #2200)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  

## Summary

Real-fix cleanup of three JDK 21 deprecation diagnostics in `modules/tlsutils` (no `@SuppressWarnings`). Replaces deprecated `URL(String)` and `X509Certificate#getSubjectDN()` with `URI#toURL()` and `getSubjectX500Principal()`. Package-private helpers added with unit tests. Minor dead local removal in `WrappedTrustManager`.

## Scope

- `modules/tlsutils/src/main/java/com/percussion/tls/TLSTester.java`
- `modules/tlsutils/src/main/java/com/percussion/tls/WrappedTrustManager.java`
- `modules/tlsutils/src/test/java/com/percussion/tls/TLSTesterTest.java`

Cross-platform path review: N/A (no path/file I/O changes beyond existing `File` usage).

## Verification

- Standalone `modules/tlsutils` `mvnw clean install`: BUILD SUCCESS, 41 tests (7 skipped env-dependent), 0 failures
- Direct `javac -Xlint:all -Xlint:-path`: 0 source warnings (was 3 deprecation warnings)

## Issues

None (bug/suggestion/nit).

## Change-class companions

| Companion | Status |
|-----------|--------|
| Real generics/API fix vs suppress | done |
| Javadoc on new helpers | done |
| Behavioral unit tests | `certificateAlias`, `toUrl` covered |
| Module clean install | green |
