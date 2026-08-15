# Erlang review — #3420 Acl / AclEntry / UserAccessLevel plain wire getters

**Scope:** uncommitted branch `fix/issue-3420-acl-plain-wire-getters-rest` vs `origin/main`.
**Reviewer persona:** Erlang (independent of implementer).
**Memory patterns hit:** REST DTO wire getters must be plain nullable types + `@JsonInclude(NON_NULL)` (ContentType #1693 / TemplateSummary #2189 / parent #3388); production mapper tests must use `new JacksonContextResolver().getContext(TheDto.class)`; Object ACL bind/save envelope (#3378 / #3384 / #3391) must not regress; change-class companions (DTO + rest tests + sitemanage ApiUtils convert).

## Summary

Parent #3388 slice 8. `Acl`, `AclEntry`, and `UserAccessLevel` still exposed `Optional<T>` getters. `Acl`/`AclEntry` bandaged Optional getters with `@JsonIgnore` + field `@JsonProperty` (#3378). `UserAccessLevel` had no ignore, so `permission` could serialize as an Optional bean.

This change:

1. Converts the three wire DTOs to plain nullable getters/setters with class-level `@JsonInclude(NON_NULL)`.
2. Deletes Optional wrapper getters (no leftover `@JsonIgnore` Optional helpers).
3. Updates rest ACL bind/save/GET tests that called `.orElse()` / `.map()` / `.isPresent()`.
4. Updates sitemanage `ApiUtils.convertAcl*` / `convertAclEntries` (`entry.getType()` and `p.getPermission()` null checks).
5. Appends three `JacksonContextResolverOptionalTest` methods (production mapper, no `empty`/`present` keys).

Does **not** retake Display Format ACL persist/GET. Does **not** commit `rest/AGENTS.md`. Does **not** add `OPTIONAL_FIELDS_SUPPORTED` or `jackson-datatype-jdk8`.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs found. Existing Object ACL envelope/Guid-string/Default-AnyCommunity tests remain and are green. Behavioral tests added for the converted family. No filesystem path construction.

C2: public getter return types changed `Optional<T>` → `T`. Grep found no `extends Acl` / anonymous rest-DTO subclasses (system `IPSAcl extends java.security.acl.Acl` is unrelated). Reverse-dep `projects/sitemanage` standalone `clean install` is green.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] No path/file I/O in this diff (JSON/JAX-RS DTO + convert only)
- [x] Tests do not assert OS-specific file paths
- [x] Line-ending assertions not added

## Issues

None (hard-gate).

### Nits

- Primitive `id` / `objectId` / `objectType` still serialize as `0` when unset (`NON_NULL` does not omit primitives). Pre-existing; same as other converted families.

## Product documentation

N/A — no operator/integrator example currently shows Optional-bean JSON for this family. Wire scalars stay the same documented shape (`name`, `aclEntries`, `permission`).
