# Erlang Review — 004/us3-t040-java-ldap-injection

**Date**: 2026-07-17  
**Reviewer**: Erlang (strict independent pre-PR)  
**Scope**: Uncommitted T040 residual #648 vs `origin/development`

## Summary

Closes residual critical `java/ldap-injection` #648. Runtime escape was already correct (`escapeLdapFilter` / `getFilterString`); this PR:

1. Forces user filter values only through `getFilterString` → sink via `andLdapFilters`.
2. Places sink-line `// codeql[java/ldap-injection]` on the exact `ctx.search` line.
3. Escapes config objectClass values in `getGroupsSearchFilter`.
4. **Fixes silent model-pack load failure**: pack path `+./.github/codeql/models` in config + workflow (bare `./` was ignored as invalid pack spec).
5. Extends LDAP barrier model for `andLdapFilters`; bumps pack to `0.0.3`.

Existing unit tests cover escape + filter construction; `andLdapFilters` tests added.

## Recommendation

**approve**

## Gate

- Blocking bugs: **0**
- May commit/push: **yes**

## Issues

None.

## Handoff

Safe to commit and open PR against `development`. After merge, re-scan should drop #648; if residual remains, dismiss as FP citing pack+tests (ladder step 5).
