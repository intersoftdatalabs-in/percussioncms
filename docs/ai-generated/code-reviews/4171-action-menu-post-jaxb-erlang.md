# Erlang review: #4171 ActionMenu POST JAXB bind

**Scope:** `fix/issue-4171-action-menu-post-jaxb` vs `origin/main` (cherry-pick of closed unmerged #4189 `b2a446b89a`).
**Change class:** rest-jax-rs JSON provider + ActionMenu create POST bind (CXF skip Jettison default JSON provider).
**Memory patterns hit:** incomplete change-class closure (rest + sitemanage dual-ship); tests that only grep source strings (registration test is companion to CXF behavioral tests, not sole proof).

## Summary

Admin `POST /services/actions` with `{"ActionMenu":{…}}` was JAXB-bound by CXF’s default Jettison `JSONProvider` as `allowedWorkflowTransitionsRequest` (finder DTO). Re-land skip-default-JSON-provider on `rest-jax-rs`, pin the finder DTO `@XmlRootElement` / `@JsonRootName`, keep `ActionMenuJsonReader` ahead of Jackson, and lock wrapped create POST with `JAXBElementProvider` in the CXF unit test.

Product-docs 8.2 REST already described wrap/flat create; one sentence now states the collection POST is `ActionMenu`, not the finder DTO.

## Recommendation

approve

## Gate

- Bugs: none
- Behavioral tests: CXF `ActionMenuCreateCxfUnmarshallTest` (wrapped/flat + JAXB provider still invokes `createActionMenu`; Jackson WRAP_ROOT_VALUE binds wrap without custom reader). `CatalogRestJaxrsRegistrationTest` asserts `skip.default.json.provider.registration` and `actionMenuJsonReader` before `jacksonProvider`. Existing `ActionMenuResourceTest` keeps unique name / 400 / 409 / 403.
- Cross-platform paths: N/A (no new filesystem I/O). Catalog test already uses `Path.of` / `Files`.
- May commit/push: yes (cherry-pick already on branch; review is of that commit)

## Issues

None.

## Companions

- rest: `ActionMenuJsonReader` javadoc, `AllowedWorkflowTransitionsRequest` root name, CXF unmarshall tests
- sitemanage: `sitemanage-beans.xml` jaxrs property; registration test
- product-docs: `product-docs/8.2/developer/rest.md`
- Playwright spec not weakened (`developer-action-menu-editor.spec.js` still asserts notice + name read-only)

C2: no type made final/sealed; no public signature change. Reverse-dep `projects/sitemanage` is in the change set.
