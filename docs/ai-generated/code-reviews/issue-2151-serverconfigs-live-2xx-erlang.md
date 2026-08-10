# Erlang review: issue-2151 serverconfigs live 2xx

## Summary

Live H2 qa-up probe of `GET /Rhythmyx/services/serverconfigs` returned **200** with
Jackson `ServerConfig` array after `restServerConfigsResource` is listed on
`rest-jax-rs` `jaxrs:serviceBeans` (container was hot-patched during #2142 work;
source on `main` still lacked the ref). Root cause class matches #1714:
`@PSSiteManageBean` scan alone does not register CXF routes.

This residual is **focused on C6 serverconfigs only** (unit ladder already shipped
in #2131 / PR #2150). No ServerConfigAdaptor / `@Lazy` rework — live 2xx proves
adaptor + IPSSystemService path is healthy once the route exists.

## Scope

- `projects/sitemanage/.../sitemanage-beans.xml` — add `restServerConfigsResource`
- `ServerConfigsRestJaxrsRegistrationTest` — static regression on rest-jax-rs block
- `developer-catalog-smoke.spec.js` — REST 2xx for `/services/serverconfigs` (#2151)
- Base: origin/main (includes #2150 unit harden + #2155 slots smoke)

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

- Static: `restServerConfigsResource` (and locked peers) must appear inside rest-jax-rs.
- Live: Playwright REST smoke GET /services/serverconfigs 2xx on H2 qa-up.

## Memory patterns

Reaffirms #1714: new `@PSSiteManageBean` REST resources require explicit
`jaxrs:serviceBeans` ref or CXF 404. Unit `requireAdaptor` 503 (#2131) does not
replace live registration verification.

## Overlap note

Open PR #2162 registers five C-slice catalog beans including serverconfigs. This
PR intentionally scopes only #2151 so residual acceptance (“no mega-PR of
unrelated catalog resources”) stays focused; either PR can land first (bean ref
is idempotent once present).
