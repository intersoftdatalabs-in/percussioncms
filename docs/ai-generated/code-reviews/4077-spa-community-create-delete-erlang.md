# Erlang review — #4077 SPA SE-01 community create/delete

**Branch:** `feat/issue-4077-spa-community-create-delete`  
**Base:** `origin/main`  
**Date:** 2026-08-31  
**Reviewer:** Erlang (independent of implementer)

## Summary

Developer → Communities catalog create/delete using existing REST
`POST /services/communities/bulk` and `DELETE /services/communities/bulk`.
Role-association save on the detail panel is unchanged. Server persist is
create+save on native `PSCommunity` (Workbench Finish). SPA create is POST-only.
Nested Jackson Guid wraps are flattened so delete has a usable GUID.
Duplicate/in-use map to HTTP 409 without `log.error`.

## Scope

- `WebUI/src/main/ts/**` communities catalog/detail, assembly API, GUID helpers
- `rest` CommunityList/GuidList JSON readers (AclList peer), write-failure map
- `projects/sitemanage` CommunityAdaptor persist-on-create + 409 mapping
- `system` `PSSecurityDesignWs.loadCommunities` null-slot skip
- Playwright `developer-community-editor.spec.js`, product-docs 8.2
- Prior memory: WRAP_ROOT_VALUE unwrap (#3039), AclList String-body readers (#3391),
  nested Guid (#3200/#3380)

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No remaining bugs. Behavioral tests cover 400/403/409 create, 409 in-use
delete without `ignoredependencies`, nested Guid unwrap, persist-on-create
duplicate. Cross-platform path review: no new filesystem path construction
(REST/URL paths use `/` correctly). Playwright C5 passed on H2 QA.

## Issues

None.

## Evidence

- rest `mvnw clean install` BUILD SUCCESS, Tests run: 953
- sitemanage BUILD SUCCESS, Tests run: 2016 (Failures 0)
- perc-system BUILD SUCCESS, Tests run: 2638 (Failures 0)
- WebUI BUILD SUCCESS, Tests 3580 passed
- perc-qa-automation BUILD SUCCESS
- Playwright surface `tests/developer-community-editor.spec.js` 2 passed;
  console-clean=yes; server.log-clean=yes
- C2: `ICommunityResource` signatures unchanged; `CommunityList`/`GuidList`
  not made `final`; sitemanage reverse-dep installed
