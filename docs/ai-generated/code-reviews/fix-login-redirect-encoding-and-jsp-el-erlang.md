# Erlang review: fix/login-redirect-encoding-and-jsp-el

**Scope:** Uncommitted changes for Jetty login redirect double-encoding + Jasper empty-EL in siteimprove include  
**Date:** 2026-07-23  
**Reviewer:** Erlang (independent of implementer)

## Summary

Two production blockers after CodeQL redirect rebuilds and modern Jasper EL:

1. **Login:** `new URI(..., getRawPath(), getRawQuery(), ...).toASCIIString()` double-encodes `%` → Jetty `Ambiguous URI path encoding` on post-login `sendRedirect`. Root leak was unauthenticated → login Location with `sys_redirect=http%253a%252f%252f...`.
2. **Dashboard JSP:** static include of `siteimprove_integration.html` contained JS character class with empty EL sequence `${}` → Jasper `Failed to parse the expression [${}]`.

Fix centralizes non-reencoding rebuild + decode defense in `PSRedirectValidation`; login/security filter call it; siteimprove regex reordered so `$` is not adjacent to `{`.

## Recommendation

**approve**

## Gate

- **May commit/push:** yes
- Bugs: none remaining in diff
- Behavioral tests: present for rebuild/decode and login resolve; JS sanitization test updated
- Cross-platform path/file I/O: **N/A** (no new filesystem path construction; URI/URL only)

## Issues

None blocking.

### Suggestions (non-blocking)

1. **decodeOverEncodedRedirect** stops on path-absolute even if query still over-encoded; acceptable for login use (absolute hosts or clean paths).
2. **WebUI/war/** copy of siteimprove is a packaged/war tree duplicate — keep lockstep with `src/main/webapp` as done.

## Memory patterns hit

- Double-encoding via multi-arg `URI` + raw components (Jetty UriCompliance)
- Jasper EL in static-included non-JSP text (including JS comments)

## Verification evidence (implementer)

- `modules/perc-security-utils` clean install
- `system` `PSLoginServletTest`
- Live: single-encoded login Location; post-login 302; dashboard/editor/design 200 after siteimprove deploy

