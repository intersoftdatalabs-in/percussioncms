# Erlang review — #4264 modules+misc system IPS*Errors → *ErrorCodes

**Branch:** `fix/issue-4264-modules-misc-ipserrors-errorcodes`  
**Parent:** #2616  
**Verdict:** pass (ship)

## Scope reviewed
Retype allow-listed modules + misc system production `IPS*Errors` call-sites to typed `*ErrorCodes`, shrink residual allow-list, dual-write skip tests. Deferred: `modules/segmentation-rx` (orphan module cannot standalone clean-install — missing/broken deps), `system/Testing/**`, `system/Tools/**`, and sibling server-core/webservices paths owned by #4262/#4263.

## Findings
- No bugs found in converted call-sites; exception typed constructors are additive.
- Cross-platform: no path I/O changes.
- Change-class companions present: allow-list shrink, gate pytest, per-module slice tests, typed ctors on `PSBeansException` / `PSContentConversionException` / `PSLocaleException`.
- `BeansErrorCodes.XML_PROCESSING_ERROR` (1001) collides with another registry entry; dual-write registry identity asserted in perc-i18n/utils tests instead of system registry `find()`.

## Tests / builds
- `modules/utils` clean install — BUILD SUCCESS (PSBeansExceptionTypedErrorCodeTest 3)
- `modules/perc-i18n` clean install — BUILD SUCCESS (slice test 2)
- `modules/perc-toolkit` clean install — BUILD SUCCESS (slice test 2)
- `system` clean install — BUILD SUCCESS (PSModulesMiscLeftoverErrorCodesSliceTest 3; Tests run: 2852, Failures: 0)
- `scripts/test_verify_no_bare_ipserrors.py` — 30 passed
