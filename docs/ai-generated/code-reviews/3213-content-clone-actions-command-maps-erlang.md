# Erlang review — #3213 content/clone/actions command-map Xlint

- **Branch:** `fix/issue-3213-content-clone-actions-command-maps`
- **Date:** 2026-08-12
- **Reviewer:** Erlang (pre-commit, implementer-independent)
- **Recommendation:** approve
- **Gate:** May commit/push: yes
- **Memory patterns hit:** prefer real generics over class-level `@SuppressWarnings`; behavioral tests for typed maps; do not change public API shape without reverse-dep grep

## Summary

PR-sized `-Xlint` cleanup of remaining `com.percussion.server` content / clone / actions / command maps after #3186. Production maps are now parameterized (`Map<String,String>` form content, `Map<String,Object>` clone/internal-request params, `Map<String,ActionResult>` action results, `ConcurrentHashMap<String,Object>` console commands). No new class-level suppressions. No path/file I/O.

## Gate

- **Bugs:** none found
- **Behavioral tests:** present (`PSFormContentParserTest`, `PSCloneBaseTypedTest`, `PSActionSetResultTest`, `PSActionSetRequestHandlerTypedTest`, `PSConsoleCommandParserTest`)
- **Cross-platform paths:** N/A (no filesystem path construction)
- **Change-class companions:** tests + standalone `system` clean install

## Issues

None.

## Notes

- `PSActionSet.toXml()` still cannot adopt a `Document` created in a different owner document; tests use `setSuccess(..., null)` for XML status assertions. Pre-existing, not introduced here.
- Remaining command-package raw **lists** (`PSConsoleCommandLogDump`) are out of this map slice.
