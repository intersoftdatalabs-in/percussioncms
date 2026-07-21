# Erlang review — issue #936, fix pack 1 (initial)

**Reviewer:** Erlang (independent pre-merge reviewer; not the author)
**Branch:** `development` (uncommitted changes, working tree)
**Scope:** `WebUI/src/main/ts/publishing/publishActions.ts`,
`WebUI/src/main/ts/publishing/sections/SiteWorkspace.tsx`,
`WebUI/src/test/ts/publishing/publishActions.test.ts`,
`WebUI/src/test/ts/publishing/siteWorkspaceBadConfig.test.tsx` (new).

**Recommendation:** `request-changes` — one real bug and one hard-gate
behavioral-test gap; do not commit until both are addressed.

## Findings

1. **bug** — `SiteWorkspace.tsx:97-105,273-277,361-365` and
   `publishActions.ts:147-151`. The single
   `preflightActionMessage()` helper conflates two message-rendering
   policies. `mapPublishError()` always populates `result.message`
   (with raw body text or a literal token like `"BADCONFIG"`), so the
   catch branch now displays that raw body instead of the prior
   localized `MSG.PUBLISH_FORBIDDEN` / `MSG.PUBLISH_BADCONFIG`.
   Likewise, a 200 BADCONFIG response with no `warningMessage`
   displays the literal token `"BADCONFIG"` instead of the localized
   string. **Fix:** keep the old localized behavior for the catch
   branch; for the new response branch, use the server warning when
   present and otherwise fall back to the i18n catalog. Leave
   `message` unset when no warning exists so the state-specific
   fallback is reachable.

2. **bug — strict behavioral-test gate** —
   `SiteWorkspace.tsx:328-365` and the new
   `siteWorkspaceBadConfig.test.tsx`. The new component regression
   test exercises only `runFullPublish()`; neither the ordinary
   incremental path nor `publishIncrementalWithApproval()` is
   behaviorally tested with a wrapped BADCONFIG response, and the
   catch-path localization regression is also untested. The pure
   mapper tests cannot detect incorrect API-branch wiring, missing
   early returns, or UI state handling in `runIncrementalPublish()`.
   **Fix:** add component tests that click the incremental action for
   both API variants, assert `role="alert"` and the server warning,
   and add rejected 403/400 cases for the catch behavior.

3. **suggestion** — `publishActions.ts:33-45,136-151`. The
   preflight tokens directly covered (`FORBIDDEN`, `INVALID`,
   `NOSTAGING_SERVERS`, `BADCONFIG`, `BADCONFIGMULTIPLESITES`) are
   the right ones for `PSSitePublishService.createResponseWontPublish`,
   but the nonblocking response path serializes job states using
   `State.getDisplayName()` rather than enum names, so
   `COMPLETED_W_FAILURE`, `ABORTED`, and `CANCELLED` are not
   recognized and would currently fall through as success. **Fix:**
   either explicitly document that this mapper is limited to direct
   preflight responses (those that arrive as `State.toString()`
   names), or add tests and mappings for the actual restart/terminal
   wire values before claiming broader application-level failure
   coverage.

## Summary

The wrapped-response handling is correct. `PSSitePublishResponse` has
`@JsonRootName("SitePublishResponse")`, the sitemanage Jackson
resolver enables `WRAP_ROOT_VALUE`, the legacy `PercPublisherService`
confirms the wrapper, and `client.ts` parses the JSON before
`mapPublishResponse()` runs. The unwrapped fallback, null/undefined
guards, `unknown` usage, state returns, and modified async branches
are otherwise sound; no new race, double-set-state, Node.js API, or
path/I/O issue was found.

The focused publishing suite passes 24 files / 96 tests, and
TypeScript type-checking is clean, but the tests do not cover the
modified incremental integration or the changed catch behavior. The
first finding is a real user-visible regression, and the missing
behavioral coverage is a hard pre-commit gate. **Do not commit or
push this change pack until those issues are addressed.**
