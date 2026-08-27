# Erlang review — #3884 cms.objectstore + client IPS*Errors typed ErrorCodes

**Scope:** uncommitted branch `fix/issue-3884-cms-objectstore-errorcodes` vs `origin/main`  
**Memory patterns hit:** typed `*ErrorCodes` + existing `IPSErrorCode` constructors; dual-write skip for `isAuditable()==false`; additive constructors already present (C2 not triggered); do not delete `IPS*Errors` interfaces; tests must exercise production exception types.  
**Cross-platform path checklist:** N/A (no new filesystem path joins; XML node names and dummy lookup strings only).

## Summary

Parent #2616 leftover slice. Remaining origin/main allow-list production `IPS*Errors` sites under `system/.../cms/objectstore/` (top-level, not `server/`) and `.../objectstore/client/` now throw typed `CmsErrorCodes` / `ServerErrorCodes` / `RemoteErrorCodes` via existing `IPSErrorCode` constructors on `PSCmsException`, `PSUnknownNodeTypeException`, `PSNotFoundException`, `PSException`, and `PSRemoteException`. Residual allow-list shrunk by those exact 17 paths only. Dual-write skip is `isAuditable()==false` (Cms leftover catalog is fully non-auditable; AA `MISSING_INTERNAL_REQUEST_RESOURCE` and remote SOAP codes are also non-auditable). No product UI/config surface. `cms/objectstore/server/**` remains on the allow-list for a follow-on leftover.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None.

### Notes (non-blocking)

- A few AA / processor-instantiation sites are covered by typed construction of the production exception types rather than full slot/variant graphs; XML/key/processor/client transport throw paths are exercised.
- `PSRemoteCataloger.getCEFieldXml` logs the transport failure at ERROR then rethrows `CONTENT_TYPE_CANNOT_BE_OPENED` (pre-existing wrap); tests assert that production type/code.
