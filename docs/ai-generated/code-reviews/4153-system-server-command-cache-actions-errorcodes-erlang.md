# Erlang review — #4153 leftover system/server IPSServerErrors (command/cache/actions/clone/compare/config)

**Branch:** `fix/issue-4153-server-errors-console-cache`  
**Base:** `origin/main`  
**Date:** 2026-09-02  
**Reviewer:** Erlang (independent of implementer)

## Summary

Parent #2616 leftover slice (#4153): remaining named `system/src/main` production `IPSServerErrors` (and other `IPS*Errors` families on the same files) in console commands, cache handlers, actions, clone, compare, and config now use typed `ServerErrorCodes` / `CloneErrorCodes` / peer catalogs and typed constructors. `IPSServerErrors` remains the numeric bridge. Additive `IPSErrorCode` constructors on `PSConsoleCommandException`, `PSCacheException`, `PSCompareException`, `PSActionSetException`, `PSInternalError`, and `PSNonFatalError`. Dual-write skip tests cover leftover non-auditable codes; leftover authz codes (`NO_AUTHORIZATION`, `CloneErrorCodes.NOT_AUTHORIZED`) still dual-write. Residual allow-list shrunk for fully converted paths. OPEN PR #4152 files (PSServer/handlers/parsers/console/remote) were not touched.

## Scope

- Diff: `git diff origin/main...HEAD`
- Production: `system/src/main/java/com/percussion/server/{command,cache,actions,clone,compare,config}/**` plus additive exception ctors
- Tests: `PSServerCommandCacheActionsLeftoverErrorCodesSliceTest`, parser typed-code assertions, `PSCacheExceptionTypedCtorTest`
- Gate: `scripts/ipserrors-residual-allowlist.txt`, `scripts/test_verify_no_bare_ipserrors.py`, `scripts/README.md`
- Memory patterns hit: change-class closure (typed ctors + production retype + allow-list shrink + dual-write skip + producer module install); additive constructors (not `final` / signature-breaking); behavioral production throws (console parser)
- Cross-platform path review: N/A — no new filesystem path construction
- Prior report: none on this branch

## Recommendation

approve

## Gate

- Bugs: none
- Missing behavioral tests: no
- Non-portable path/file I/O: no
- May commit/push: yes

## Issues

None.

## Notes

- Job leftovers (`server/job/**`, #4143) and navigation leftovers (#4144) remain out of scope.
- PSServer/handler/parser/console/remote files remain on the residual allow-list because OPEN PR #4152 owns that slice; several of those files still mention other `IPS*Errors` families.
- C2: new public constructors are additive overloads; no production subclasses of `PSCacheException` / `PSConsoleCommandException` / `PSCompareException` / `PSActionSetException`.
- Product docs: N/A (internal error-catalog retype; no operator/API/UI/config change).
- UI/Playwright C5: N/A.
