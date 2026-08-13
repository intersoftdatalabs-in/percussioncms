# Erlang review — #3270 PSServer init/lock/search Xlint

**Branch:** `fix/issue-3270-psserver-init-lock-search-xlint`  
**Scope:** leftover `@SuppressWarnings("unchecked")` on `PSServer.init` /
`initObjectStoreHandler` / `initMacros` / `initLogHandling` / `initSearch`,
`getUserRoleAttribute` lists, `getInternalRequest` extraParams, and
`PSServerLockManager` / `PSServerLockResult` lock maps vs `origin/main`.  
**Out of scope (intentionally left):** `#3267` dataHandlers / handler-state maps;
`#3213` command maps.

**Memory patterns hit:** prefer real generics over blanket suppressions;
extract typed helpers and test them; do not expand Xlint PRs into sibling
packages except the documented leftover types (`PSSearchConfig` custom props
feed `initSearch`).

## Summary

Typed leftover raw collections and removed unused method-level
`@SuppressWarnings("unchecked")` on the listed init/search/role-attribute
paths. Public `extraParams` is `Map<String, ?>` so existing
`Map<String, String>` and `Map<String, Object>` callers still compile.
Lock maps/lists are `Integer`/`PSServerLock`. Search custom properties are
`Map<String, String>`. Behavioral tests cover merge, attribute lookup, extra
param copy, search property copy, and lock-result mapping.

## Recommendation

approve

## Gate

May commit/push: yes

## Cross-platform path checklist

- No new filesystem path construction
- Test string `/tmp/idx` is a search custom-property value, not a file I/O path
- N/A for separators, temp dirs, line endings, scripts

## Issues

None (bugs). Remaining `@SuppressWarnings` on request-handler / handler-state
methods are owned by open PR `#3267` and were not retouched to reduce merge
conflict on `PSServer.java`.
