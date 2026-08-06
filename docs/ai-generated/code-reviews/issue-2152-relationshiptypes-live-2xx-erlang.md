# Erlang review — #2152 relationshiptypes live 2xx

**Date:** 2026-08-06  
**Branch:** `fix/issue-2152-relationshiptypes-live-2xx`  
**Change class:** REST jaxrs serviceBeans registration + QA REST smoke (peer of #2151 / #1714)

## Scope

- `sitemanage-beans.xml` — add `restRelationshipTypeResource` to `rest-jax-rs` serviceBeans
- `RelationshipTypesRestJaxrsRegistrationTest` — static regression on rest-jax-rs block
- `developer-catalog-smoke.spec.js` — REST 2xx for `GET /services/relationshiptypes`

## Findings

| Severity | Finding | Disposition |
|----------|---------|-------------|
| none | N/A | approve |

### Checklist

- [x] No bugs in registration (ref matches `@PSSiteManageBean("restRelationshipTypeResource")`)
- [x] Behavioral unit/static test for new wiring (REQUIRED_REFS inside rest-jax-rs only)
- [x] Portable paths (`Path.of`, forward-slash resource path segments under repo; no OS-hardcoded install roots)
- [x] No secrets in smoke test (uses `adminBasicAuthHeaders()` / env)
- [x] Peer-matched companions (ServerConfigs residual #2151 pattern)
- [x] No rule-file changes

## Live evidence

H2 `perc-devctl` qa-up (`TEST_CMS_URL=http://127.0.0.1:9993`): after jaxrs reg (hot-patched container matched source), Basic + `RX_USEBASICAUTH`:

- `GET /Rhythmyx/services/relationshiptypes` → **200** `RelationshipType` array (ActiveAssembly, …)
- Peer slots still 200

Root cause was **CXF 404** (missing serviceBeans ref), not design-WS / adaptor 500. Unit slice #2132 already hardened requireAdaptor null→503.

## Verdict

**approve** — ready for commit/PR after module clean installs.
