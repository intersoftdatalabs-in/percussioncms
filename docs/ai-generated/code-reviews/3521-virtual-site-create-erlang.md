# Erlang review — #3521 Virtual site create

**Branch:** `feat/issue-3521-virtual-site-create`  
**Date:** 2026-08-17  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Jackson WRAP/UNWRAP_ROOT_VALUE (`VirtualSiteProperties` envelope, not bare `sourceKind`); change-class companions (wizard + validation + API + Vitest + Playwright + product-docs); optional persist vs Developer handoff.

## Summary

Slice 3 of parent #3512. Create Site / New Site type picker enables **Virtual**. Virtual flow is type → details (name/description only) → confirm (optional Git root) → progress. Create is existing `POST /sitemanage/site/` with `managedNavigation: false` and no page template, then `PUT /services/sites/{name}/virtual` with the existing `VirtualSiteProperties` envelope when a root is supplied. Blank root hands off to Developer → Sites (server still requires `virtual.rootPath` for a Virtual PUT). Traditional and Page product rules are preserved in the same picker (slice 1/2 not merged).

## Issues

None remaining after the New Site persist fix.

**Fixed in-session:** Create Site with properties (`navigation.managed` / Virtual source) failed H2 `NULL not allowed for column SITEID` on `RXASSEMBLERPROPERTIES` (#3511). `PSSiteProperty.site` JoinColumn was `insertable=false`. Mapping now inserts SITEID; new-site save persists the site first, then property bag. Test: `PSSitePropertySiteIdMappingTest`. Live H2 QA Virtual create + PUT `/virtual` passed after hot-deploy.

## Cross-platform path checklist

- [x] No new `".../" +` filesystem joins in production Java/TS I/O
- [x] Optional Git root is operator-entered text; validation only rejects `..`
- [x] Playwright `/opt/Percussion` is the **CMS host** path inside Linux H2 QA, not a workspace path
- [x] Envelope and REST URLs use `/` (URL form)

## Tests

- Vitest: kind helpers, virtual skip-template / hide nav, optional root PUT vs handoff, `createVirtualSite` POST then PUT envelope
- Playwright: `explorer-site-create-virtual.spec.js` plus Traditional chrome updates
- WebUI `mvnw clean install`: BUILD SUCCESS, Surefire 61, Vitest 2696
