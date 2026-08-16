# Erlang review — #3482 Home Create reserved popup

**Branch:** `fix/issue-3482-home-create-reserved-popup` (stacked on `cluster/night-issue-20260816-explorer-editor`)
**Scope:** uncommitted WebUI editor host + AssetWizard + Vitest + Playwright gate
**Date:** 2026-08-16

## Summary

Two cooperating defects left the reserved Create-asset popup on `about:blank`:

1. **Relative URL on about:blank** — path-only `location.href` has no valid URL base. Fix: resolve against the opener and `location.assign` the absolute `spa.jsp?entry=editor` URL. Retry also reserves a window.
2. **Bare JSON create body** — CXF JAXB expects `{ItemCreateRequest:{…}}` (`@XmlRootElement`). A bare `{contentType}` is HTTP 400, create throws, reserved window never navigates. Fix: `wrapItemCreateRequest` (peer of AddFolder/Page envelopes).

Live H2 QA: `home-react-editor.spec.js` Create-asset test **passed** after both fixes + rest/sitemanage hot-deploy.

Memory patterns hit: missing behavioral tests (covered); change-class closure (Vitest + Playwright); JAXB root envelope (AddFolder/Page peers).

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None blocking.

### Nits (non-blocking)

- `navigateReservedWindow` re-resolves a URL already made absolute by `openEditorHost`. Harmless (scheme fast-path).
- Playwright `not.toHaveURL(/^about:blank$/)` adds wait; leftover-JSP request listener on the popup is extra belt-and-suspenders.

## Change-class closure

| Companion | Status |
|-----------|--------|
| Production: `openEditorHost` + `AssetWizard` retry | yes |
| Vitest reserved-window absolute assign + opener-base resolve | yes |
| Playwright `tests/home-react-editor.spec.js` | yes (timeout + leave-blank assert) |
| product-docs | N/A — operator Create-asset steps unchanged |
| Java public API / C2 reverse-deps | N/A — TS only |

## Cross-platform path checklist

- No new filesystem path joins
- URL/query paths correctly use `/`
- Tests use URL strings, not OS path assertions

## Tests

- `openEditorHost.test.ts` — reserved window gets `https?://` + `entry=editor`, not `about:blank`; `location.assign`; `resolveEditorNavigationHref` against opener vs `about:blank`
- `AssetWizard.test.tsx` — real `openEditorHost` assigns editor URL; retry passes `reservedWindow`
- Playwright gate unchanged path; stronger URL wait
