# Erlang review — #4150 leftover system IPSServerErrors (PSServer/handlers/parsers)

**Branch:** `fix/issue-4150-system-ipsservererrors-leftover`  
**Base:** `origin/main`  
**Date:** 2026-09-02  
**Reviewer:** Erlang (independent of implementer)

## Summary

Parent #2616 leftover slice (#4150): remaining named `system/src/main` production `IPSServerErrors` throw/log sites in `PSServer`, `PSApplicationHandler`, request/content parsers, console/remote console, `PSServerLogHandler`, and close peers now use typed `ServerErrorCodes` / typed constructors. `IPSServerErrors` remains the numeric bridge. Additive `IPSErrorCode` constructors on `PSRequestParsingException`, `PSInvalidRequestException`, and `PSServerLockException`. Dual-write skip tests cover leftover non-auditable codes; leftover authz codes (`NO_AUTHORIZATION`, community authentication) still dual-write. Residual allow-list shrunk for fully converted paths (files that still mention other `IPS*Errors` families stay listed).

## Scope

- Diff: `git diff origin/main...HEAD` (plus uncommitted working tree at review time)
- Production: `system/src/main/java/com/percussion/server/**` slice files listed in #4150
- Tests: `system/src/test/java/com/percussion/server/PSServerLeftoverErrorCodesSliceTest.java`
- Gate: `scripts/ipserrors-residual-allowlist.txt`, `scripts/test_verify_no_bare_ipserrors.py`, `scripts/README.md`
- Memory patterns hit: change-class closure (typed ctors + production retype + allow-list shrink + dual-write skip + producer module install); additive constructors (not `final` / signature-breaking); behavioral production throws (parsers + remote console)
- Cross-platform path review: N/A — no new filesystem path construction; tests construct exceptions / parse MIME types only
- Prior report: none on this branch (`4142` leftover report not on `main`)

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

- `PSServer`, `PSApplicationHandler`, `PSRequest`, `PSFileRequestHandler`, and `PSServerLogHandler` remain on the residual allow-list because they still contain other `IPS*Errors` families (`IPSHttpErrors`, `IPSDataErrors`, `IPSSecurityErrors`, `IPSSearchErrors`) — out of this slice’s `IPSServerErrors` scope (Job #4143 / Navigation #4144 / other catalogs).
- C2: new public constructors are additive overloads; `PSInvalidRequestException` is the only production subclass of `PSRequestParsingException`; no anonymous subclasses.
- Product docs: N/A (internal error-catalog retype; no operator/API/UI/config change).
- UI/Playwright C5: N/A.
