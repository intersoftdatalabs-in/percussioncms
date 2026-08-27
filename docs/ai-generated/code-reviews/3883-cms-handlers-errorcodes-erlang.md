# Erlang review — #3883 system/src/main cms handlers leftover IPS*Errors

**Scope:** uncommitted branch `fix/issue-3883-cms-handlers-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; additive constructors (C2); do not delete `IPS*Errors` interfaces; PathItem-owned folder/community denials stay auditable; SQL_EXCEPTION_WRAPPER 1002 collides with `ServerErrorCodes.RAW_DUMP`.  
**Cross-platform path checklist:** N/A (no new filesystem path joins; HTTP status ints and catalog codes only).

## Summary

Parent #2616 leftover slice: sixteen `system/src/main/java/com/percussion/cms/handlers` production `IPS*Errors` call-sites now construct typed `CmsErrorCodes` / `ServerErrorCodes` / `DataErrorCodes` / `HttpErrorCodes` / `PathItemErrorCodes`. Additive `IPSErrorCode` constructors on `PSDataExtractionException` (utils), `PSServerConfigException`, `PSUnsupportedConversionException`, `PSInternalRequestCallException` (cause overload), and the AA handler local `PSRequestException`. Residual allow-list shrunk by those exact 16 paths. Dual-write skip tests cover leftover non-auditable catalog codes; folder/community denials remain auditable. HTTP status sites use `.numericCode()`. No product UI/config surface.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

### Notes (non-blocking)

- `IPSCmsErrors.SQL_EXCEPTION_WRAPPER` (1002) is mapped to `ServerErrorCodes.RAW_DUMP` per the CmsErrorCodes collision comment; numeric contract is unchanged.
- cms builders (#3882) and cms objectstore (#3884) remain on the allow-list by design.
