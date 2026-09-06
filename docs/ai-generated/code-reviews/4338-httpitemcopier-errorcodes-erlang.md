# Erlang review: #4338 Testing cms HttpItemCopier IPSHttpErrors typed ErrorCodes

**Branch:** `fix/issue-4338-httpitemcopier-errorcodes`  
**Base:** `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** leftover `IPS*Errors` → `*ErrorCodes` companions (dual-write skip, allow-list shrink, resurrection-guard pytest); behavioral tests for non-trivial 302 compare.

## Summary

Parent #2616 leftover slice. Last residual production `IPS*Errors` call-site:

- `system/Testing/cms/HttpItemCopier.java` — clone-success check now compares `HttpRequest.getResponseCode()` to `HttpErrorCodes.HTTP_MOVED_TEMPORARILY.numericCode()` (HTTP 302). Other 3xx statuses remain failures.

Allow-list shrinks that exact path. Residual production list is now empty. Dual-write skip: `HTTP_MOVED_TEMPORARILY` is non-auditable protocol status. `IPSHttpErrors` remains the numeric bridge.

The tool is not on the perc-system compile path (`system/Testing`); Maven owns the slice test in `system/src/test`.

## Issues

None blocking.

## Tests

- `PSHttpItemCopierLeftoverErrorCodesSliceTest` — numeric parity with `IPSHttpErrors.HTTP_MOVED_TEMPORARILY` and literal `302`; dual-write skip; 302-only clone-success compare (200/301/303/307 fail).
- `scripts/test_verify_no_bare_ipserrors.py` — converted HttpItemCopier path not re-listed; empty allow-list passes on a clean tree.

## Cross-platform path checklist

N/A for product I/O. Scripts/tests use repo-relative `/` paths (git/allow-list, not OS joins).

## Product documentation

N/A — internal typed error-code retype of a Testing clone helper; not operator/user/API-facing.

## C2 reverse-deps

Did not apply: no `final`/`sealed` on shared types; no public/protected signature change on types other modules compile against.
