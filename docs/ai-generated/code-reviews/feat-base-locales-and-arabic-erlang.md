# Erlang review: feat/base-locales-and-arabic

**Date:** 2026-07-29  
**Scope:** Uncommitted branch vs `origin/development`  
**Intent:** RXLOCALE ISBASE flag, Arabic base locale, login base-hiding filter, TMX policy docs.

## Summary

Adds `ISBASE` to schema/entity/seed, Arabic `ar` as base, server-side login
locale filter (`PSLocaleLoginSelection`), TMX header + fallback tests, docs.
Does not invent bare `en`/`fr`/… base rows (confirmed plan scope).

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Issues

| Severity | Finding | Status |
|----------|---------|--------|
| nit | Arabic TMX body not fully back-filled — header only; missing segs fall back to en-us | open (documented follow-up) |
| nit | Existing DBs get ISBASE default 0; upgrade `action=u` for es/hi only | open (acceptable) |

## Cross-platform path checklist

No new filesystem path construction. Clean.

## Tests observed

- `PSLocaleLoginSelectionTest`, `PSLocaleTest` ISBASE
- `PSTmxResourceBundleTest` including Arabic fallback
- Vitest login host contract
