# Erlang review — #3212 PSApplicationHandler/PSServer dataHandlers Xlint

**Date:** 2026-08-12  
**Branch:** `fix/issue-3212-datahandlers-xlint`  
**Scope:** uncommitted `system` typing of dataHandlers / request-root maps vs `origin/main`  
**Memory patterns hit:** prefer real generics over blanket `@SuppressWarnings("unchecked")`; restore static test fixtures; do not expand Xlint PRs into sibling packages.

## Recommendation

**approve** — May commit/push: yes

## Gate

No bugs found. Behavioral tests cover request-root maps, handler-state merge, status XML, and log map keys. No filesystem path I/O in this change (cross-platform path checklist N/A).

## Issues

None.

## Notes

- `getInternalRequestHandler(String)` and `PSServer.getApplicationHandler` now return `null` instead of throwing `ClassCastException` when the map value is the wrong interface. Safer; production puts still implement the expected types.
- Residual Xlint remains on `PSServer` (`init`, objectstore/macros/log/search, role attributes) and `#3213` content/clone/actions — out of scope.
