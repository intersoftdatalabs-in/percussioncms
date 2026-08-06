# Erlang review: issue-2142 searches live 2xx (jaxrs registration)

## Summary
Live H2 qa-up probe of `GET /Rhythmyx/services/searches` returned **404 Not Found**
(not 500). Root cause: `restSearchResource` was `@PSSiteManageBean`-scanned but never
listed on `rest-jax-rs` `jaxrs:serviceBeans` in `sitemanage-beans.xml` — same class as
#1714 (slots/keywords/locales). After registering the five missing catalog beans and
Jetty-only restart (docker restart re-extracts install XML), all returned **2xx**:

| Endpoint | Status | Notes |
|----------|--------|-------|
| /services/searches | 200 | `{"SearchDef":[]}` empty OK on stock H2 |
| /services/views | 200 | ViewDef catalog |
| /services/cecontrols | 200 | ControlDef catalog |
| /services/serverconfigs | 200 | ServerConfig catalog |
| /services/relationshiptypes | 200 | RelationshipType catalog |

## Scope
- `projects/sitemanage/.../sitemanage-beans.xml` — add five `ref bean` entries
- `CatalogRestJaxrsRegistrationTest` — static regression on rest-jax-rs block
- `developer-catalog-smoke.spec.js` — REST 2xx matrix including searches (#2142)
- Base: origin/main (includes #2143 SearchResource unit harden)

## Recommendation
approve

## Gate
May commit/push: **yes**

## Cross-platform path review
- Unit test resolves monorepo root via `Path` (cwd / `../..`) — portable Windows/Unix.
- No new File I/O in production code; smoke uses `BASE_URL` + string URL path.

## Issues
None (bug/suggestion/nit).

## Behavioral coverage
- Static: REQUIRED_REFS must appear inside rest-jax-rs server block.
- Live: Playwright REST smokes for slots + searches + C peers 2xx on H2 qa-up.

## Memory patterns
Reaffirms #1714: new `@PSSiteManageBean` REST resources require explicit
`jaxrs:serviceBeans` ref or CXF 404.
