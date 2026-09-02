# Erlang review — #4156 relationship-effect leftover IPS*Errors typed ErrorCodes

- Branch: `fix/issue-4156-relationship-effect-error-codes`
- Base: `origin/main`
- Date: 2026-09-02
- Recommendation: **approve**
- Gate: **May commit/push: yes**
- Memory patterns hit: behavioral tests for changed logic; incomplete change-class closure (typed exception ctors + allow-list shrink + dual-write skip); non-portable path/file I/O (checklist N/A)

## Summary

Parent #2616 leftover slice. Remaining `system/src/main/java/com/percussion/relationship/effect` production `IPS*Errors` call-sites now construct typed catalogs (`ExtensionErrorCodes`, plus `ServerErrorCodes.MISSING_INTERNAL_REQUEST_RESOURCE` / `RAW_DUMP` and `CmsErrorCodes.UNDEFINED_DEFAULT_TRANSITION` / `UNEXPECTED_ERROR` on the same files). Additive `IPSErrorCode` constructors on `PSResult.setError/setWarning`, `PSNotFoundException` (locale), `PSExtensionProcessingException` (locale+args), and `PSLogServerWarning`. Residual allow-list shrunk by those exact 7 paths. Dual-write skip is `isAuditable()==false` on leftover operational catalog codes; nearby auditable extension authz still dual-writes. No product UI/config surface.

## Change class

Typed ErrorCodes production call-site conversion (leftover relationship-effect package).

Companions present: additive IPSErrorCode constructors; allow-list shrink; freeze-gate pytest so the 7 paths stay off the list; dual-write skip tests (perc-auditlog + system leftover slice); production throw/warning tests (`PSEffectUtils`, `PSValidate`, `PSIsCloneExists`, `PSPromote`, `PSPublishMandatory` / `PSUnpublishMandatory`).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] New path logic uses `Path` / `Files` (N/A — no file I/O)
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Temp files use portable APIs (N/A)

## Issues

None.

## Verification

- `python scripts/verify-no-bare-ipserrors.py` — PASS
- `python -m pytest scripts/test_verify_no_bare_ipserrors.py -q` — 23 passed
- `cd modules/perc-auditlog && ../../mvnw.cmd clean install` — BUILD SUCCESS (`RelationshipEffectResidualErrorCodesDualWriteTest` Tests run: 3, Failures: 0)
- `cd modules/utils && ../../mvnw.cmd clean install` — BUILD SUCCESS (`PSExceptionTypedConstructorSliceTest` Tests run: 2, Failures: 0)
- `cd system && ../mvnw.cmd clean install` — BUILD SUCCESS (`PSRelationshipEffectLeftoverErrorCodesSliceTest` Tests run: 11, Failures: 0)

## Notes

- C2: additive public constructors / overloads (not `final`/`sealed`; existing int overloads unchanged). Grep `extends PSResult` found `PSEffectResult` only; no `extends PSNotFoundException` / `PSLogServerWarning` / `PSExtensionProcessingException`; no anonymous subclasses of those types. Reverse-dep module rebuilds not required beyond installing `modules/utils` then `system`.
- Product documentation: N/A (internal error-catalog retype; no operator/API surface change).
- Did not re-do #4149 DTD/workflow/date/CMS files.
