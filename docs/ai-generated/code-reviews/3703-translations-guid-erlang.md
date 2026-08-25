# Erlang review — #3703 Translations GET GUID vs numeric

**Branch:** `fix/issue-3703-translations-guid`  
**Scope:** uncommitted vs `HEAD` (WebUI Translations panel + REST adaptor)  
**Reviewer:** Erlang (strict, independent of implementer)  
**Date:** 2026-08-21

## Summary

Explorer Translations GET was stripping hyphenated GUIDs (`16777215-101-551` → `551`), which 404s while the full GUID returns 200. The change keeps the raw row id on GET, still uses numeric ids for create-variant POST, and the adaptor resolves both hyphenated GUIDs and bare numeric content ids (numeric path skips untyped `getGuid`).

Memory patterns hit: change-class closure (rest + sitemanage apibridge + WebUI + Playwright + product-docs); behavioral tests for new resolve logic; no non-portable path I/O.

## Recommendation

**approve** — May commit/push: yes

## Gate

No blocking bugs. Behavioral tests cover GUID vs numeric GET keys, adaptor dual-path, REST passthrough, and Playwright fail-closed GUID URL. Product-docs updated. Cross-platform path checklist N/A (no filesystem path construction).

## Issues

None.

## Notes (non-blocking)

- Create-variant POST remains numeric `itemIds` (REST contract). GET is the 404 surface.
- Playwright console-error gate may be noisy on H2; treat feature-related errors only as C5 fail.
- Duplicate #3704 not in this change.

## Cross-platform path checklist

N/A — no new file I/O, installers, or path assertions.
