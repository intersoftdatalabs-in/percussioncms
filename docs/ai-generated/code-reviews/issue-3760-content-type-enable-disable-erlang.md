# Erlang review: issue #3760 Content Type enable/disable design REST (CD-13)

## Summary

Dedicated `PUT /services/contenttypes/{idOrName}/enabled` (CD-13) over `IPSContentDesignWs` requires a
held design-session lock (`IPSSystemDesignWs.isLocked`), Admin, and persists `PSItemDefinition.setEnabled`
with `saveContentTypes(..., release=false)`. Typed `ContentTypeDesignLockException` maps to HTTP 409
without substring inference. GET detail `enabled` is the read-back. Companions: wire DTO, adaptor
interface, sitemanage impl, Mockito resource tests, Spring `TestContentTypeAdaptor` stub,
`ContentTypesTestAdaptor`, adaptor unit tests, product-docs.

## Scope

- Branch: `feat/issue-3760-content-type-enable-disable` vs `origin/main`
- Modules: `rest`, `projects/sitemanage`, `product-docs/8.2/developer/rest.md`
- Prior report / memory: rest change-class (resource + IXxxAdaptor + Spring stub + sitemanage impl +
  adaptor tests); typed 409 vs message-substring 409 (`percBlockquote`)
- Cross-platform path review: N/A (no filesystem path I/O)

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (no `bug` findings). Behavioral tests cover 200 persist, GET reflects enabled, 400 missing flag,
403 not Admin, 404 missing type, 409 no lock / other locker / other session, 500 generic state with
`lock` in the name.

### suggestion

- Parallel PRs #3748/#3749 introduce the same `ContentTypeDesignLockException` and lock helpers.
  Expect a merge-time reconcile (keep one exception type; share `requireHeldLock`). Not a defect in
  this slice.

### nit

- Existing `PUT /contenttypes/{idOrName}` still auto lock-save-unlock and can set `enabled` as a
  field. Out of scope (#3743). Documented dedicated CD-13 action is the new contract.
