# Erlang review: feat/locale-format-table

**Date:** 2026-07-29  
**Scope:** Uncommitted branch vs origin/development  
**Intent:** RXLOCALEFORMAT keyed by language string; resolver; login bootstrap.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

## Notes

- Keyed by `LANGUAGESTRING` (not LOCALEID) for customer locales.
- Product defaults mirror seed for UI without DB.
- Fallback chain matches TMX: exact → language-only → en-us.
- Tests: PSLocaleFormatResolverTest (7), Vitest login suite (42).

## Cross-platform

No filesystem path I/O. Clean.
