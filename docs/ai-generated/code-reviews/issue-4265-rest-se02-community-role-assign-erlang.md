# Erlang review: issue-4265 REST SE-02 community role assign/unassign

## Summary

SE-02 REST surface already existed from P0.5c (`GET /communities/roles`,
`GET /communities/{idOrName}`, `PUT /communities/{idOrName}/roles` full-set
replace). This change closes the companion gaps: sitemanage adaptor behavioral
tests, role identity synthesis/validation, product-docs for assign/unassign, and
a rest empty-list unassign resource test.

## Scope

- `projects/sitemanage/.../CommunityAdaptor.java` — `ensureRoleIdentity`
- `projects/sitemanage/.../CommunityAdaptorRolesTest.java` (new)
- `rest/.../ICommunityAdaptor.java` — javadoc
- `rest/.../CommunityResourceDetailTest.java` — empty-list case
- `product-docs/8.2/developer/rest.md`, `product-docs/8.2/admin/developer-communities.md`
- Prior: P0.5c #1616 already shipped resource + SPA dual-list save

Cross-platform path review: N/A (no file I/O or path construction in this diff).

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

(none at bug severity)

### suggestion

- `ensureRoleIdentity` casts `roleId` long → int for GUID uuid. Matches existing
  ROLE GUID construction elsewhere; role catalog uuids are int-range in practice.

### nit

- `ICommunityResource` still omits get/list/update role methods present on
  `CommunityResource`. Pre-existing; out of scope for this slice.
