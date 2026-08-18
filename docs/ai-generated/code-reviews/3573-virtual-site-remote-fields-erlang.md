# Erlang review — #3573 Developer Sites remote URL and branch fields

## Summary

Optional **Remote URL** and **Branch** fields on Developer → Sites Virtual Site
source, with GET/PUT round-trip so Save does not drop a stored remote and a
blank remote still saves local `rootPath`. No client checkout/URL validation
(server owns that on #3568 / PR #3572).

## Scope

- Branch: `feat/issue-3573-virtual-site-remote-fields` vs `origin/main`
- Files: WebUI form + `sitesApi` envelope, Vitest, Playwright
  `developer-site-virtual-source`, `product-docs/8.2/admin/sites.md`
- Memory: change-class WebUI product screen (Vitest + Playwright + product-docs)
- Cross-platform path review: no filesystem I/O. `C:/docs` strings are form/API
  fixtures (same pattern as existing panel tests), not path joins.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None at bug severity.

### suggestion — merge order with #3572

`formToVirtualProps` always includes `remoteUrl`/`branch` (empty string when
blank) on virtual PUTs. Production Jackson is `FAIL_ON_UNKNOWN_PROPERTIES`.
Servers that do not yet have PR #3572 `VirtualSiteProperties.remoteUrl` will
400 a virtual Save that includes those keys. Create-site PUT omits the keys
when they are undefined (safe). Persist of remotes requires #3568 / PR #3572.

### nit — client still requires root when a remote is set

`validateVirtualSiteForm` still returns `root-required` when `rootPath` is
blank even if `remoteUrl` is set. #3568 treats root as required only when the
remote is blank. Acceptable for this slice (do not re-implement checkout
rules); operators can enter a relative checkout folder.

## Prior report / Memory patterns

No prior 3573 report. Matched change-class completeness: Vitest helpers +
panel + Playwright surface + product-docs admin Sites panel.

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
