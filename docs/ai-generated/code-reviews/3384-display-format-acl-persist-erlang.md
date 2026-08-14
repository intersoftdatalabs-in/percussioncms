# Erlang review — #3384 Display Format Object ACL persist/GET

**Date:** 2026-08-14  
**Branch:** `fix/issue-3384-display-format-acl-persist`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** approve

## Summary

PUT `/services/acls/bulk` converted a new `PSAclImpl` and `session.merge` inserted a
duplicate `PK_PSX_ACLS` (or swallowed the error and GET reopened empty). This change
loads the existing ACL by objectGuid/SYSID, merges entries onto that identity
(preserving version and object type/id), and rethrows persist failures as
`ACL_SAVE_ERROR` instead of returning 200 after a silent Hibernate error.

## Scope

Uncommitted work vs `origin/main` on `fix/issue-3384-display-format-acl-persist`.

## Memory patterns hit

- Missing behavioral unit tests — **addressed** (merge-vs-insert + GET-after-save shape)
- Empty catch / swallowed exceptions — **addressed** (`internalPersist` now throws)
- Incomplete change-class closure — Playwright spec tightened; product-docs updated
- Cross-platform path checklist — N/A (no new filesystem I/O)

## Issues

None that block commit.

## Notes (not blocking)

- `PSAclImpl.merge` still copies `m_version` from the incoming object; the helper
  restores version when incoming version is null. Callers must use the helper, not
  raw `merge`, for REST save.
- Adaptor GET path still uses `ApiUtils.convertGuid(Guid)` (GuidManager). Tests
  that would hit Spring locator were kept off that path; GET shape is covered by
  `ApiUtils.convertAcl(PSAclImpl)`.
- GET JSON used JAXB/Jettison on Optional getters and omitted `aclEntries`/`name`.
  Field-access `@XmlElement`/`@JsonProperty` on `Acl`/`AclEntry` is required for
  GET-after-save. Live H2: PUT 200 then GET includes Default/AnyCommunity/Admin.
  Playwright surface spec passed.

## Tests

- `PSAclPersistMergerTest` — 4 tests
- `AclAdaptorSaveMergeTest` — merge identity + insert-when-absent
- `ApiUtilsAclConvertTest` — GET-after-save Default/AnyCommunity/Admin + identity
- `AclResourceTest` — GET entries contract
- Playwright reopen now requires those three rows (no `no-entries` accept)

## Cross-platform path checklist

N/A — no new `File`/`Path` construction.
