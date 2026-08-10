# Erlang review: issue-2146 extensions catalog live 2xx smoke

## Summary

Adds REST probe for GET /services/extensions/catalog (2xx + JSON array /
`Extension` wrapper shape) to developer-catalog-smoke after live H2 qa-up
confirmed HTTP 200 with `{"Extension":[]}`. No product/resource/adaptor code
change — `restExtensionsResource` already registered on rest-jax-rs;
unit-test slice #2129 / PR #2147 hardened null-adaptor 503 + catalog ladder.

## Scope

- modules/perc-qa-automation/frontend/tests/developer-catalog-smoke.spec.js
- Base: origin/main (includes #2147 / #2155)
- Live evidence: perc-devctl qa-up H2 →
  GET /Rhythmyx/services/extensions/catalog 200 `{"Extension":[]}`

## Path confirmation

- SPA: `WebUI/.../paths.ts` → `${SERVICES_ROOT}/extensions/catalog`
- Resource: `ExtensionsResource` `@Path("/extensions")` + `@Path("/catalog")`
- Full URL: `/Rhythmyx/services/extensions/catalog`

## Recommendation

approve

## Gate

May commit/push: **yes**

## Cross-platform path review

N/A — no file I/O or path construction; uses BASE_URL + string URL path and
adminBasicAuthHeaders peer pattern from #2121 / bug-1622.

## Issues

None (bug/suggestion/nit).

## Behavioral coverage

- New Playwright REST test asserts 2xx + array/wrapper payload for extensions.
- Empty catalog treated as valid 2xx (matches live H2 empty Extension list).
- Live run against H2 qa-up (TEST_CMS_URL freeport).

## Memory patterns

No new durable pattern; reuses RX_USEBASICAUTH + Authorization Basic from
auth.js / #2121 slots residual.
