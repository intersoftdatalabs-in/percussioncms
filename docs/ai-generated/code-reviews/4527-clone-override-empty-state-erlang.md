# Erlang review — #4527 clone-override empty-state after clear save

## Summary

Residual of #4470 / PR #4526: after remove-all + Save of relationship-type cloning field overrides, Playwright could not see `[data-testid=developer-rt-clone-empty]`. Two defects: (1) skip-image-build H2 QA WAR `sitemanage-beans.xml` may omit `relationshipTypeJsonReader`, so Jackson drops `cloneOverrides: []` (omit, not clear) — fixed with `clearCloneOverrides: true` (item-filter `clearRules` peer); (2) GET/PUT JAXB empty collection `{}` / wrapper must not coerce to a phantom row. REST docs mention the flag. H2 Playwright 6/6 after Jetty restart.

## Scope

- Uncommitted/branch vs `origin/main` on `fix/issue-4527-clone-override-empty-state`
- Files: `RelationshipTypeJsonReader` (+ tests + serial test), `RelationshipTypeAdaptor.copyCloneOverrides`, WebUI `relationshipTypesApi` / `RelationshipTypeDetailPanel` (+ Vitest), Playwright spec
- Memory patterns hit: incomplete change-class closure (Playwright + rest/sitemanage/WebUI companions); JAXB singleton/empty-collection coerce (slotLists `asJacksonArray` peer)
- Cross-platform path review: no filesystem path I/O in this diff (JSON coerce + UI + REST). Clean.
- Prior report: none for #4527

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None at bug severity. Behavioral tests cover:

- PUT `cloneOverrides: []` remains empty list (existing)
- PUT `cloneOverrides: {}` / JAXB wrapper empty is empty list, not a phantom row
- Jackson WRAP_ROOT serializes empty list as `[]`
- SPA `coerceCloneOverrides` drops `{}`, `{empty:true}`, and JAXB wrappers
- Panel clear-save sends `cloneOverrides: []` and ignores a JAXB empty bean on the PUT response
- Playwright asserts empty-state after save **and** after catalog reload
