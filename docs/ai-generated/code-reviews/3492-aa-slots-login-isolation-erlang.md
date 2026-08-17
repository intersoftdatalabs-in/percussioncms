# Erlang review: #3492 AA @aa-slots login isolation

**Branch:** `fix/issue-3492-aa-slots-login-isolation`  
**Scope:** uncommitted vs `HEAD` / commits not in `origin/main`  
**Reviewer persona:** Erlang (independent of implementer)  
**Date:** 2026-08-16

## Summary

Cycle Verify residual: `explorer-active-assembly.spec.js` `@aa-slots` timed out in `beforeEach` `loginAsAdmin` because `fillLoginForm` waited for `[data-testid=perc-login-form].or([data-testid=perc-login-root]).first()` to be visible. `#perc-login-root` is the empty `rxlogin.jsp` mount and stays hidden (0×0) until React paints the form — or stays hidden after redirect. Waiting on that root burned 30s.

Fix extracts a pure `classifyLoginSurface` helper. Hidden `perc-login-root` is never the login UI. `login` / `fillLoginForm` wait for a visible `perc-login-form` or legacy `j_username`, and skip fill when the URL already left `/login` or `perc-spa-app` / `assembly-host` is visible.

**Memory patterns hit:** missing behavioral tests (unit classifier added); Playwright companion for login helper; CMS `/` login paths (not OS file I/O); incomplete change-class (test:unit + README registered).

## Recommendation

**approve**

## Gate

- Bugs: none found after review
- Behavioral tests: present (`tests/unit/login-surface.test.js`, 11 passed; full `npm run test:unit` 303 passed)
- Cross-platform paths: URL/pathname only; no filesystem joins
- **May commit/push:** yes

## Issues

None.

## Notes (non-blocking)

- WebUI AssemblyHost was not changed. Live H2 `@aa-slots` passed after the helper fix (slot bar / add / remove chrome already present).
- Product-docs: N/A (Playwright helper isolation; operator login UX unchanged).
- `qa-up --skip-image-build` reported `RESULT:FAIL DETAIL:server_log_errors` for FastForward `CI_PS_products_on.gif` import. Docker `Health=healthy`, HTTP 200 `/Rhythmyx/rest/mimetypes`. Not a `Failed startup of context`. Pre-existing install noise, not this feature.

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction
- [x] Login path checks use URL `pathname` (`/Rhythmyx/login`) — URL form, not OS paths
- [x] Tests do not assert OS-only absolute path shapes
- [x] No temp/install path hardcodes
