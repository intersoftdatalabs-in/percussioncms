# Erlang review: issue-2205 empty-recycle API

## Summary

Backend-only empty Recycling bin for parent #944 slice 1. Dedicated service + CM1 REST under pathmanagement; reuses path deleteFolder(shouldPurge) and folderHelper leaf purge. Admin-only. No public rest-module adaptor (WebUI peers are sitemanage internal REST).

## Scope

- projects/sitemanage recycle service/data + REST + unit tests
- sitemanage-beans.xml jaxrs registration
- No filesystem I/O (CMS finder paths only, product `/` convention)

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None at bug severity.

### suggestion

- Consider future metrics for partial undeleted (in-use) items if WebUI needs per-path detail; current summary counts + errors list is adequate for slice 1.

### nit

- REST class lives under recycle.service.impl (peer to page/asset rest services that also live under service.impl packages).

## Cross-platform path review

No OS filesystem paths; only CMS logical paths with `/`. N/A for Path/Files gates.

## Tests

- PSEmptyRecycleServiceTest: 8 tests
- PSRecycleRestServiceTest: 4 tests
- Module `mvnw clean install` BUILD SUCCESS

