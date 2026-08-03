# Erlang review: WebUI country-flag-icons frontend dep

**Scope:** Uncommitted fix — add `country-flag-icons` to Maven frontend npm package + ambient types  
**Date:** 2026-08-03  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Incomplete change-class closure (dual package.json); multi-copy lockstep under WebUI

## Summary

`LocaleFlag` imports `country-flag-icons/react/3x2`. The dependency lived only on `WebUI/package.json` (optional root install). Maven `frontend-maven-plugin` installs from `WebUI/src/main/frontend/package.json`, so clean machines fail with “Cannot find module”. Machines with a prior root `npm install` under `WebUI/` still resolve via upward `node_modules` walk — explains machine-specific failure.

## Changes reviewed

|                         Path                          |                                                                      Role                                                                       |
|-------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| `WebUI/src/main/frontend/package.json`                | Declare dependency for Maven npm install                                                                                                        |
| `WebUI/src/main/frontend/package-lock.json`           | Lockfile for reproducible install                                                                                                               |
| `WebUI/src/main/ts/country-flag-icons-react-3x2.d.ts` | Ambient types: path map `"*"` → `node_modules/*` resolves nested package `main` (`index.cjs`) and misses package `exports.types` / `index.d.ts` |

## Issues

None (bugs). No new runtime logic; existing `LocaleFlag` / login tests cover flag UI. No path/file I/O.

## Cross-platform path checklist

N/A — npm metadata and TypeScript ambient module only.

## Verification

- `tsc --noEmit` from `WebUI/src/main/frontend` (exit 0 after fix)
- Vite build previously resolved the package when present under `frontend/node_modules`

