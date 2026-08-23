# Erlang review: #3756 extensions-main IPS*Errors typed ErrorCodes

**Scope:** uncommitted work on `fix/issue-3756-extensions-main-errorcodes` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Date:** 2026-08-23

## Summary

Leftover `modules/extensions-main` production `IPS*Errors` sites now construct typed `*ErrorCodes` via additive `IPSErrorCode` constructors on `PSExtensionException`, `PSExtensionProcessingException`, `PSConversionException`, `PSRequestValidationException`, and `PSInternalRequestCallException`. Allow-list shrunk by those exact 20 paths. Dual-write skip tests cover leftover non-auditable catalog codes. No product UI/config surface.

Memory patterns hit: change-class closure (typed ctors + production retype + allow-list + dual-write skip + producer module install); portable `@TempDir Path` in new tests; additive constructors (not `final` / signature-breaking).

## Gate

No bugs. Behavioral tests cover typed construction, production throws (`PSFormEncode`, `PSPrepareInClause`, `PSSetArrayHtmlParameter`, `PSConcatAssemblyLocation`), and dual-write skip. Cross-platform path checklist: new tests use `Path` / `@TempDir`; no hardcoded separators. Existing assembly exits that swallow `PSExtensionException` were not changed.

## Issues

None.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] New path logic uses `Path` / `Files` (`@TempDir Path` + `toFile()` for legacy `init`)
- [x] Tests do not assert Unix-only absolute path shapes
- [x] N/A for product scripts / installers
