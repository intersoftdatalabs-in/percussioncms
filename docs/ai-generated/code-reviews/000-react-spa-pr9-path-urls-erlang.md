# Erlang review — PR-9 path-based SPA URLs

| Field | Value |
|-------|-------|
| **Branch** | `feat/000-react-spa-pr9-path-urls` |
| **Base** | `development` @ PR-8 merge |
| **Date** | 2026-07-27 |
| **Scope** | BrowserRouter + `PSWebUiSpaFallbackFilter` + path↔entry helpers + tests/docs |
| **Recommendation** | **approve** |
| **May commit/push** | **yes** |

## Summary

PR-9 switches the SPA client router from HashRouter to BrowserRouter with a
context-aware basename (`/cm/app` or `/cm/pages/app`, optional `/Rhythmyx`
prefix). Server deep links and login return stay on the query contract
(`spa.jsp?entry=…`); a synchronous handoff rewrites that document URL to a
clean path before the router mounts. GET path refreshes are served by an
internal forward filter that maps allowlisted SPA first segments to
`spa.jsp?entry=…` (preserving section/tab/query), never rewriting real JSPs or
static assets.

## Gate

| Check | Result |
|-------|--------|
| Bugs (logic / security) | None found — allowlisted entries only; traversal rejected; no open redirect |
| Behavioral tests | Java `PSWebUiSpaFallbackFilterTest` (7); Vitest App/deepLinks/session/spaCutover |
| Cross-platform path/file I/O | N/A for filter path math (URL paths use `/`); no OS filesystem |
| Build | `cd WebUI && ../mvn-env.sh clean install` → **BUILD SUCCESS**; Surefire 11 tests |

## Issues

None blocking.

### Nits

1. Filter does not re-allowlist `section`/`tab` tokens server-side (relies on existing
   spa.jsp / client allowlists after forward). Acceptable; same trust boundary as query
   handoff.
2. `useMemo` used once for sync URL handoff in `App` — intentional first-paint rewrite.

## Evidence

- Java: Tests run: 11 (filter 7 + gadget 4), Failures: 0
- Vitest: App + deepLinks + auth + spaCutover green after basename handoff fix
