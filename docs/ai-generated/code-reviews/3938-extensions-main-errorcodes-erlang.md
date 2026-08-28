# Erlang review: #3938 remaining extensions-main IPS*Errors typed ErrorCodes

**Scope:** uncommitted work on `fix/issue-3938-extensions-main-errorcodes` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Date:** 2026-08-28

## Summary

Parent #2616 leftover slice. Production `modules/extensions-main` call-sites were already typed in #3756 / PR #3769 (`ExtensionErrorCodes` / `XmlErrorCodes` / `DataErrorCodes` / `ServerErrorCodes`). Cherry-pick #3793 re-listed those 20 exact paths on `scripts/ipserrors-residual-allowlist.txt`. This change shrinks the allow-list for those paths only, adds production-throw coverage (`PSGenerateAssemblerLink`, `PSTranslationConstraint`) plus dual-write skip already in perc-auditlog, and guards against re-listing via pytest. No product UI/config surface.

Memory patterns hit: change-class closure (allow-list shrink + gate pytest + behavioral throws + existing dual-write skip); portable `@TempDir Path` retained; no signature-breaking API changes.

## Gate

No bugs. Behavioral tests cover typed construction and production throws. Freeze gate `python scripts/verify-no-bare-ipserrors.py` PASS. Pytest `scripts/test_verify_no_bare_ipserrors.py` 18 passed. Standalone `cd modules/extensions-main && ../../mvnw.cmd clean install` BUILD SUCCESS, Tests run: 63, Failures: 0. Cross-platform path checklist: tests use mocks / `@TempDir Path`; no hardcoded separators. Did not class-load `PSGenericAssembly` in unit tests (legacy `org.apache.log4j.LogManager` requires log4j-core not on this module test classpath).

## Issues

None.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] New tests do not assert Unix-only absolute path shapes
- [x] N/A for product scripts / installers (allow-list paths stay posix `/` as required by the freeze gate)
