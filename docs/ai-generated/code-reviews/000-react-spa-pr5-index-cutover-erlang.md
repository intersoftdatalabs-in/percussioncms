# Erlang review — feat/000-react-spa-pr5-index-cutover

**Date:** 2026-07-27  
**Scope:** Uncommitted PR-5 aggressive `index.jsp` SPA cutover (both trees)  
**Base:** `development` @ `818bcb8338`  
**Recommendation:** **approve**  
**May commit/push:** **yes**

## Summary

Modern product views (`home`, `publish`, `workflow`, `admin`, `widgetbuilder`) no longer forward to `*Modern.jsp`. Both `cm/app/index.jsp` and `cm/pages/app/index.jsp` redirect with **proxyURL** to `/cm/app/spa.jsp?entry=…` using allowlisted deep-link params only (query contract, no hash). Legacy exits (`dash`, `editor`, `design`, `arch`, edit*) unchanged. Retired `*Modern.jsp` hosts re-enter the dispatcher via a shared include. Vitest static contract tests cover dual-tree alignment and publish rewire.

## Gate

| Check | Result |
|-------|--------|
| Bugs / behavioral regressions | None found |
| Missing behavioral tests for non-trivial logic | OK — JSP allowlist + map covered by `spaCutover.test.ts` + updated `publishNavRewire.test.ts` (same style as prior US8 rewire tests) |
| Cross-platform path/file I/O | N/A (URL paths only; `/` correct for HTTP) |
| Security (open redirect / reflected param) | OK — sections/tabs/ids allowlisted + `URLEncoder`; never emit `#` |

## Issues

None blocking.

### Notes (non-blocking)

1. **Double hop** from retired `*Modern.jsp` → `?view=` → `spa.jsp` is intentional and rare after cutover.
2. **Default null view** for Home no longer echoes arbitrary leftover query keys onto the SPA URL (stricter allowlist) — preferred for security.
3. **Pages tree** previously served classic `adminWorkflow.jsp` for `view=workflow`; dual-tree alignment now SPA (design mandate §3.4).

## Verification

- `cd WebUI && ../mvnw clean install` → **BUILD SUCCESS** (Surefire Tests run: 4, Failures: 0)
- Vitest: `spaCutover`, `publishNavRewire`, `UnavailableView` → **10 passed**

## Memory patterns hit

- Dual-tree lockstep for SPA hosts  
- proxyURL parity on redirects  
- Query-only SPA entry contract (never hash Location)
