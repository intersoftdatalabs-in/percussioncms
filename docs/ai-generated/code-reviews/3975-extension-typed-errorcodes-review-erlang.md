# Erlang review — PR #3975 review-thread follow-up

**Scope:** uncommitted review-thread fix on `fix/issue-3970-extension-typed-errorcodes-wt` vs `HEAD`.
**Recommendation:** approve
**Gate:** May commit/push: yes
**Memory patterns hit:** Public helper Javadoc that contradicts implementation; missing behavioral tests

## Summary

Kilo suggested the new `logMessage(IPSErrorCode, Object[])` Javadoc requires non-null `args` but only `errorCode` was validated. Enforced with `Objects.requireNonNull(args)` (matches Javadoc; int overload unchanged). Added `typedLogMessageRejectsNullArgs`.

## Issues

None. No signature change. No file I/O. Product-docs N/A (internal error-catalog helper).

## Cross-platform path checklist

N/A — no path/file I/O in this diff.

## Evidence

`cd system && ../mvnw.cmd clean install` → BUILD SUCCESS. Tests run: 2588, Failures: 0, Skipped: 246. `PSExtensionLeftoverErrorCodesSliceTest` 10/10 including `typedLogMessageRejectsNullArgs`.
