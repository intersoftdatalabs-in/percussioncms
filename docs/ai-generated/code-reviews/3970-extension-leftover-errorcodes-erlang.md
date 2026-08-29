# Erlang review — #3970 system extension leftover IPS*Errors typed ErrorCodes

- Branch: `fix/issue-3970-extension-typed-errorcodes`
- Base: `origin/main`
- Date: 2026-08-28
- Recommendation: **approve**
- Gate: **May commit/push: yes**
- Memory patterns hit: behavioral tests for changed logic; incomplete change-class closure (typed exception ctors + allow-list shrink + dual-write skip); non-portable path/file I/O (checklist N/A)

## Summary

Parent #2616 leftover slice. Nine `system/src/main/java/com/percussion/extension` production `IPS*Errors` call-sites now construct typed catalogs (`ExtensionErrorCodes`, plus `ServerErrorCodes.ARGUMENT_ERROR` / `DataErrorCodes.UNSUPPORTED_CONVERSION` on `PSJavaScriptUdfExtension`). Additive `logMessage(IPSErrorCode, Object[])` on `PSExtensionHandler` keeps the int overload for `logMessage(0, …)`. Residual allow-list shrunk by those exact 9 paths. Dual-write skip is `isAuditable()==false` on leftover operational catalog codes. No product UI/config surface.

## Change class

Typed ErrorCodes production call-site conversion (leftover extension package).

Companions present: existing IPSErrorCode constructors (no new public exception signatures); allow-list shrink; freeze-gate pytest so the 9 paths stay off the list; dual-write skip tests; production throw tests (`PSExtensionParams`, handler init, handler config missing file, JS UDF param/type/conversion).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] New path logic uses `Path` / `Files` (`@TempDir Path` + `Files.writeString`)
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Temp files use `@TempDir` (portable)

## Issues

None.

## Verification

- `python scripts/verify-no-bare-ipserrors.py` — PASS
- `python -m pytest scripts/test_verify_no_bare_ipserrors.py -q` — 20 passed
- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 2587, Failures: 0, Skipped: 246 (`PSExtensionLeftoverErrorCodesSliceTest` 9/9)

## Notes

- C2: `logMessage(IPSErrorCode, Object[])` is additive protected. Grep `extends PSExtensionHandler` found `PSJavaExtensionHandler`, `PSJavaScriptExtensionHandler`, and the package test stub. No `new PSExtensionHandler() {`. No reverse-dep module install required (existing int overload unchanged; exception int ctors remain).
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- UI/Playwright C5: N/A.
- Dual-write skip tests go through the enum (`isAuditable()`), not `LegacyErrorCodeRegistry.find(int)`.
- Did not delete `IPSExtensionErrors` (int bridge remains).

Memory patterns hit: missing behavioral tests; incomplete change-class closure (allow-list companion); dual-write skip for `isAuditable()==false`.
