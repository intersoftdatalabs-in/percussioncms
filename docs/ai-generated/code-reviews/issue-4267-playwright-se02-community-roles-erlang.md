# Erlang review — #4267 Playwright SE-02 community roles H2

**Verdict:** approve (with companion REST/SPA wire fixes required for green C5)

## Scope

- `modules/perc-qa-automation/frontend/tests/developer-community-roles.spec.js`
- Live-proof companions found by C5 against tips #4272/#4273:
  - SPA: wrap PUT body as `CommunityRoleList`; coerce one-item `roleList` object
  - REST: `CommunityRoleListJsonReader` + String-body PUT (peer CommunityList bulk)
  - sitemanage: `convertCommunity` full-set replace via `setRoleAssociations`

## Checklist

- [x] Behavioral tests: Playwright surface + Vitest/Surefire for wrap/asRoles/JsonReader
- [x] Cross-platform: no path I/O in Playwright; portable helpers
- [x] Product-docs updated for preferred wire envelope
- [x] C5: qa-up → qa-health → deploy → `test:surface` path → qa-down (1 passed)
- [x] No weakened assertions

## Notes

Bare JSON array PUT fails CXF UNWRAP_ROOT; ArrayList subclass needs JsonReader.
Jackson one-item list emits a bare role object — SPA `asRoles` must coerce.
