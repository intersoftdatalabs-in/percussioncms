# Erlang review — #3860 system/business leftover IPS*Errors typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-3860-system-business-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; additive constructors (C2); do not delete `IPS*Errors` interfaces; `(IPSErrorCode, Object...)` vs `(IPSErrorCode, Throwable, Object...)` null-arg ambiguity — explicit `(Throwable) null` at Solr no-cause sites.  
**Cross-platform path checklist:** N/A (no new filesystem path joins; test args are dummy strings, not OS paths).

## Summary

Parent #2616 leftover slice: eight `system/business` delivery production `IPSDeliveryErrors` int sites now construct typed `DeliveryErrorCodes` via additive `IPSErrorCode` constructors on `PSDeliveryException` (delegates to `PSBaseException`). `getExceptionResult` gained a typed overload. `PSRelationshipConfigModel` was already on `WebserviceErrorCodes.DELETE_FAILED`; its allow-list row is removed. Residual allow-list shrunk by those exact paths only. Dual-write skip is `PSDeliveryException.isAuditable()` because `DeliveryErrorCodes` does not flat-register (Workflow 1–10 / Assembly 11–12 collision). Decrypt-credentials remains auditable. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

### Notes (non-blocking)

- `PSDeliveryInfoLoader` still mentions `IPSDeliveryErrors` inside a block comment; the freeze gate ignores comments. Out of this issue’s listed files.
- `system/src/main` mega-tree and `system/webservices` leftovers remain on the allow-list (siblings / later slices).
