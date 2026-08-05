# Erlang review — issue #1867 (jsdom location navigation mock)

**Change class:** Vitest / jsdom test-infrastructure (shared setup mock)  
**Scope:** `WebUI` only  
**Production code:** unchanged

## Summary

Install a shared Vitest setup mock so `window.location.href` / `assign` /
`replace` do not emit jsdom “Not implemented: navigation” when components
redirect under tests. Navigations are applied via `history.pushState` /
`replaceState` so pathname/search remain observable for React Router.

Also updates the outdated `tmx.jsp` escaping contract test (pre-existing suite
blocker on `main`) to assert `PSTmxJsCatalog.toJsObjectEntries` instead of
inline `XSSValidation` (GH-1611 architecture).

## Checklist

|                Gate                 |                                     Result                                     |
|-------------------------------------|--------------------------------------------------------------------------------|
| Bugs in new logic                   | Pass — mock no-ops when `location` non-configurable; invalid URLs swallowed    |
| Behavioral tests                    | Pass — `jsdomLocationNavigation.test.ts` + Dashboard legacy redirect assertion |
| Cross-platform paths                | N/A — no filesystem path I/O in mock                                           |
| Production navigation semantics     | Unchanged — setup file only runs under Vitest                                  |
| Companions                          | setupFiles wired in both `WebUI/vite.config.ts` and frontend vite config       |
| Spotless apply then check           | Pass (WebUI module)                                                            |
| `cd WebUI && ../mvnw clean install` | BUILD SUCCESS                                                                  |

## Residual

None for #1867. Pre-existing suite green after tmx contract alignment.
