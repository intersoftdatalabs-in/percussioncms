# Erlang review — #3839 REST object-storage virtual.sourceKind

Reviewer: independent of implementer. Date: 2026-08-26.

## Change class

REST persist of Virtual Site `virtual.sourceKind=object-storage` (allow-list + fail-closed
rootPath). Companions: helper validation, Mockito resource tests, existing Spring
`SitesTestAdaptor` stub comments, sitemanage adaptor tests, product-docs 8.2.

## Findings

### Bugs

None. PUT/GET round-trip uses existing `SitesAdaptor` → `PSVirtualSiteHelper.validate`.
Unknown kinds still 400 via `fromWireName` null. git/csv/sql/http-json paths unchanged
except allow-list text.

### Path / I/O

`object-storage` root uses NIO `Path.of(...).normalize()` and `isSafeRootPath` (no
remaining `..`). Cloud URI schemes rejected before `Path.of` so `s3://` / `https://`
are not treated as filesystem paths. Windows drive letters (`C:`) are not URI schemes
(`colon` index 1). No `"/" +` filesystem joins.

### Secrets

Credential property names fail closed. Exception text does not echo property values or
cloud URLs (may contain query credentials). `virtual.remoteUrl` already rejected for
non-git kinds.

### Tests

Helper: pass safe root, reject `..`, remoteUrl, cloud URLs, credential property (no
secret in assertion). Resource Mockito GET/PUT round-trip. Adaptor GET-after-PUT,
parent path, remoteUrl, s3 URL, leftover `aws_secret_access_key`. Spring stub already
exists (`SitesTestAdaptor`).

### Factory

Exhaustive `switch` on `VirtualSiteSourceType` is wired: `OBJECT_STORAGE` creates
`PSObjectStorageVirtualSiteSource` (SPI #3838 / PR #3844 landed on `main` before this
rebase). REST GET/PUT persist does not call the factory. REST Build/Preview/Publish for
this kind stay later-phase.

### Product docs

`product-docs/8.2` developer/admin/reference note REST GET/PUT persist and fail-closed
cloud/credentials. Build/Preview/Publish/UI for this kind remain later-phase (not
claimed).
