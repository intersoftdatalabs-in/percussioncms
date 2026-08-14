# Erlang review — #3378 Display Format Object ACL Save HTTP 400

**Scope:** uncommitted branch `fix/issue-3378-display-format-acl-save` vs `origin/main`.
**Reviewer persona:** Erlang (independent of implementer).
**Memory patterns hit:** Jackson WRAP/UNWRAP_ROOT_VALUE (UserPreference / VirtualSiteProperties peers); CXF ArrayList→typed list ClassCast; change-class companions (reader + beans.xml provider + SPA wrap + tests + product-docs + Playwright); behavioral tests for new parse/normalize; residual persist not claimed as done.

## Summary

Human FAIL on QA #2640: Developer Display Format **Save** Object ACL returned HTTP 400; Default / AnyCommunity / USER disappeared after reopen.

Root cause of the 400 is CXF Jackson `UNWRAP_ROOT_VALUE` plus `AclList extends ArrayList`: the bulk PUT body binds as `java.util.ArrayList` and the resource parameter type is `AclList` → `ClassCastException` → 400.

This change:

1. Adds `AclListJsonReader` (registered ahead of `jacksonProvider` on `rest-jax-rs`) that binds `{"AclList":[…]}`, a bare array, or JAXB `{"AclList":{"Acl":[…]}}`.
2. SPA wraps PUT/POST with Jackson roots; strips `principal.type`; derives `objectType`/`objectId` from `objectGuid.stringValue`; defaults `name` to `"ACL"`.
3. `ApiUtils` skips `setGUID(null)` and copies object identity from `objectGuid`.
4. `@JsonIgnore` on `TypedPrincipal` `is*` getters (those names collided with unknown JSON properties).
5. Unit tests (reader, resource, convert, Vitest wrap) + Playwright surface spec + product-docs.

**Not fixed here:** Hibernate persist / GET-after-save still can 500 (duplicate PK insert) or return empty entries. Playwright asserts **not 400**; reopen may still be `no-entries`. Residual is PR-sized.

## Recommendation

**approve** for the 400 slice (Partial #3378). Do not treat persist/GET as closed.

## Gate

**May commit/push: yes** (partial; residual required)

No new bugs in the 400 path. Reader is package-local parse with 400 on malformed JSON. No filesystem path construction. C2: no `final`/`sealed` or signature breaks; no `extends AclList` / anonymous `AclList` subclasses in-repo.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] New code is JSON/JAX-RS/SPA only (URL paths use `/` correctly)
- [x] Tests do not assert OS-specific file paths
- [x] Playwright uses `TEST_CMS_URL` / `BASE_URL` (no hardcoded `:9993`)

## Issues

None on the 400 slice (hard-gate).

### Residual (blocking full acceptance, not this commit)

`PUT /services/acls/bulk` can still 500 (`PK_PSX_ACLS` insert vs merge) and `GET /services/acls/object/{guid}` often returns no `aclEntries`. File a p1 child of #2640.

## Product documentation

Updated `product-docs/8.2/admin/object-acl.md`, `users-roles.md`, and `developer/rest.md` (PUT envelope, principal/type shape).
