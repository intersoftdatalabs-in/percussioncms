# Erlang review — #4337 Testing Extensions IPS*Errors typed ErrorCodes

**Branch:** `fix/issue-4337-testing-extensions-errorcodes`  
**Parent:** #2616  
**Change class:** leftover IPS*Errors → typed *ErrorCodes (legacy Testing exits + freeze-gate allow-list shrink)

## Scope
- `system/Testing/Extensions/src/com/percussion/extensions/testing/PSMakeCERequest.java`
- `system/Testing/Extensions/src/com/percussion/extensions/testing/PSSortDocData.java`
- `system/Testing/Extensions/src/com/percussion/extensions/testing/TestMakeInternalRequest.java`
- `scripts/ipserrors-residual-allowlist.txt` + `scripts/test_verify_no_bare_ipserrors.py` + `scripts/README.md`
- `system/src/test/java/com/percussion/extensions/testing/PSTestingExtensionsLeftoverErrorCodesSliceTest.java`

## Findings
- No bugs in typed ErrorCodes conversion:
  - `ServerErrorCodes.REQUEST_HANDLER_NOT_FOUND` (1308, `isAuditable==false`)
  - `ExtensionErrorCodes.EXT_PARAM_VALUE_INVALID` / `EXT_MISSING_REQUIRED_PARAMETER_ERROR` (non-auditable)
- `IPS*Errors` interfaces remain numeric bridges; constructors use exact `PSExtensionProcessingException`.
- Paths: no new filesystem path construction; portable. Gate pytest uses posix repo-relative paths.
- Companions: allow-list shrink (Testing Extensions only; HttpItemCopier / RxFix left for #4338/#4339), resurrection-guard pytest, slice unit test with numeric parity + dual-write skip + exact exception type.
- Testing Extensions sources are not on perc-system compile path (`add-source` does not include `system/Testing`). Gate verification is `python3 scripts/verify-no-bare-ipserrors.py` + pytest; Maven owns the slice test in `system`.

## Memory patterns hit
- Missing behavioral unit tests (addressed via slice test + dual-write skip)
- Incomplete change-class closure (allow-list + pytest resurrection guard + leftover slice test)

## Cross-platform path checklist
N/A for product I/O. Scripts/tests use repo-relative `/` paths (correct for git/allow-list, not OS joins).

## Verdict
PASS — `cd system && ../mvnw clean install` BUILD SUCCESS (Tests run: 2872, Failures: 0). Gate pytest 34 passed.

## Recommendation
approve

## Gate
May commit/push: yes
