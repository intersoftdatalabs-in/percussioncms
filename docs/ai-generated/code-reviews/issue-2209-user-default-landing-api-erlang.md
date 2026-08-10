# Erlang review: issue #2209 user default landing API + persist

**Branch:** `feat/issue-2209-user-default-landing-api`  
**Date:** 2026-08-06  
**Reviewer:** Grok Build (self-review / Erlang gate)  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Adds persisted user CMS landing override (`perc.user.homepage.{user}`) peer to role homepage metadata, REST GET/PUT/DELETE on CM1 user service, and effective resolve precedence in `PSRoleService.getUserHomepage()`: user override > role resolve > Home. Unit tests cover unset fallback, set override, invalid value, multi-role unchanged when unset, view-key mapping, and admin ACL on named-user endpoints.

## Scope

- `projects/sitemanage` only (no rest module public API change; CM1 internal `/user` REST)
- Parent #959 slice 2; slices 3–4 (index.jsp mapping for expanded types, Admin UI) intentionally out of scope
- Cross-platform path review: N/A (no new file I/O / path handling)

## Issues

None at **bug** severity.

### suggestion

1. **Username case in metadata key** — key uses exact `userName` string. If admin GET/PUT uses different case than login name, override may not match. Peers (`perc.user.*.dash.page`) use same pattern; acceptable for slice 2. Slice 4 UI should pass the canonical system username.

### nit

1. Fully-qualified `PSUserService.normalizeHomepageType` in `PSRoleService` could be a static import; left explicit to avoid role→impl coupling noise in imports.

## Test evidence

- Focused: `PSUserHomepageTest`, `PSRoleServiceHomepageTest`, `PSUserServiceMockTest` green
- Module: `projects/sitemanage` `mvnw clean install` green

## Companions checked

- Change class: CM1 domain service + JAX-RS methods on existing bean (not new public rest adaptor)
- REST client implementor of `IPSUserService` updated (`PSUserServiceRestClient`)
- Constructor + mock test call sites updated for `IPSMetadataService`
- No Spring test context bean gap expected (component scan / constructor autowire of existing `metadataService`)

