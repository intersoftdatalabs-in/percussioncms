# Erlang review: #3770 nav/sfp/workflow IPS*Errors typed ErrorCodes

**Scope:** uncommitted work on `fix/issue-3770-nav-sfp-workflow-errorcodes` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Date:** 2026-08-26

## Summary

Leftover `modules/extensions-nav`, `modules/extensions-sfp`, and `modules/extensions-workflow` production `IPS*Errors` sites now construct typed `ExtensionErrorCodes` / `CmsErrorCodes` / `ServerErrorCodes` / `PathItemErrorCodes`. Additive `IPSErrorCode` constructors on workflow/system/utils exception types. Allow-list `scripts/ipserrors-residual-allowlist.txt` shrunk by those exact paths only. Dual-write skip tests cover leftover non-auditable catalog codes. Language+typed constructors live on subclasses (not `PSException(String, IPSErrorCode)`) to avoid `(String, Throwable)` null-arg ambiguity. No product UI/config surface.

Memory patterns hit: change-class closure (typed ctors + production retype + allow-list + dual-write skip + producer module install); additive constructors (not `final` / signature-breaking); behavioral production throws for sfp calendar + workflow mail.

## Gate

No bugs. Behavioral tests cover typed construction, production throws (`PSExpandRecurringEvents`, `PSMakeCalendar`, `PSJavaxMailProgram`, `PSSecureMailProgram`), and dual-write skip. Cross-platform path checklist: new tests use mocks / typed constructors; no hardcoded separators. Did not add `PSException(String, IPSErrorCode)` on the base class after it made `new PSException("msg", null)` ambiguous with `(String, Throwable)`.

## Issues

None.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] New tests do not assert Unix-only absolute path shapes
- [x] N/A for product scripts / installers
