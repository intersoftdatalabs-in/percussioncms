# Erlang review — #4452 system-def stylesheet write (SYS_STYLESHEET)

Independent pre-commit review of CD-16 command-handler stylesheet GET/PUT
(REST + sitemanage adaptor + SPA + Playwright + product-docs).

## Verdict

**Pass** after unit/module builds. No blocking bugs found. Path handling is
URI-shaped (`file:../sys_resources/stylesheets/*.xsl`), not OS filesystem
join.

## Change class

New nested REST write on existing `SystemDefResource` (not a new JAX-RS
resource bean). Companions: wire DTOs, `ISystemDefAdaptor` methods, Spring
test stub, sitemanage adaptor + tests, SPA panel + Vitest, Playwright
surface spec, product-docs.

## Findings

Fixed before commit:

- **Lock leak on 400** — validate handlers/hrefs before `loadContentEditorSystemDef(lock)`
  so invalid PUT does not hold the design lock. Write loads use `overrideLock=true`
  so the same Admin user can finish a request-lock write after a stale session.

Other notes:

- Href validator rejects extra `..`, backslash, absolute `file:/`, and
  `http(s)` while allowing Workbench `file:../sys_resources|rx_resources/stylesheets/*.xsl`.
- PUT full-replace omits/blank-href removes handlers; empty remaining set is
  400 (object-store requires at least one CommandHandler).
- Admin 403 before load; lock 409 via existing `SystemDefDesignLockException`.
- `SYS_STYLESHEET` dropped from `CONTROL_PROPERTY_DESIGN_GAPS`; `SYS_APP_FLOW`
  remains.
- CXF registration unchanged (`restSystemDefResource` already on rest-jax-rs).

## Tests / docs

- rest `SystemDefResourceTest` stylesheet GET/PUT 403/400/409
- sitemanage `SystemDefAdaptorTest` round-trip, add/remove, invalid href
- WebUI Vitest panel + API wrap/unwrap
- Playwright `developer-system-def-stylesheets.spec.js`
- `product-docs/8.2/admin/developer-system-def.md` and `developer/rest.md`
