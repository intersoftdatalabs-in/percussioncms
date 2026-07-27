# Erlang review — feat/000-react-spa-pr2-app-shell

**Date:** 2026-07-27  
**Scope:** Pure React SPA PR-2 — App shell, TopNav, entry query, lazy bridge, 401→Login.  
**Memory patterns hit:** open redirects; missing behavioral tests; bridge race; secrets.

## Summary

PR-2 adds authenticated SPA chrome (`App` + `HashRouter` + `AppLayout`/`TopNav`), allowlisted `parseEntryQuery`, mid-session 401 redirect to React Login with query return URL, and race-safe async `loadComponent` bridge mounts. Feature routes are placeholders until PR-3/4 embed real shells.

## Recommendation

**approve**

## Gate

| Check | Result |
|-------|--------|
| Bugs | None found |
| Behavioral tests | 27 vitest (entry, session, App, bridge races, login) |
| Open redirect | 401 return + buildLoginReturnUrl allowlist |
| Bridge races | generation token on mount/unmount |
| May commit/push | **yes** |

## Issues

None (hard gate).

### Suggestions

1. Placeholders intentionally defer shell embed to PR-3.
2. spa.jsp role resolution uses Spring beans; failure soft-falls to reduced nav.

## Test evidence

- Vitest app/login/bridge: 27 pass  
- WebUI clean install: see PR body  
