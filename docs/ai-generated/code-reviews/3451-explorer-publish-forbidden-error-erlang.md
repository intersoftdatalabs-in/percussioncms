# Erlang review: Explorer Publish Now HTTP 200 FORBIDDEN error chrome (#3451)

**Branch:** `fix/issue-3451-explorer-publish-forbidden-error`  
**Base:** `origin/main`  
**Date:** 2026-08-15  
**Persona:** Erlang (independent of implementer)

## Summary

Explorer Publish Now already throws on HTTP 200 `{status:"FORBIDDEN"}` via
`mapPublishResponse` / `publishSelectedItem`. The shell only mounted
`[data-testid=explorer-server-actions-error]` for catalog `menuLoadError`.
Invoke failures wrote the generic `error` alert instead, so Playwright
`explorer-action-dispatch.spec.js` never saw the server-actions error region.

This change adds `actionInvokeError` and mounts the same testid for
`handleMenuInvoke` / `handleActionError` failures, showing the thrown
FORBIDDEN/licensing text. Successful invoke still clears the region and
does not refresh the list on throw.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral Vitest covers the FORBIDDEN 200 path on the
shell (error region text + no extra folder refresh). Existing
`itemPublish` throw tests remain. Playwright spec already asserts the
testid; product-docs N/A (no new operator chrome copy).

## Change-class closure

Change class: **WebUI Explorer action-invoke error chrome**.

| Companion | Status |
|-----------|--------|
| Shell `actionInvokeError` + testid | present |
| Vitest (`ContentExplorerShell` FORBIDDEN 200) | present |
| `itemPublish` throw tests | already present |
| Playwright `explorer-action-dispatch.spec.js` | already present |
| product-docs | N/A — server warning text only; no new chrome string |

## Cross-platform path checklist

No filesystem path I/O. Publish URLs use `/` (URL form). **Outcome: clean.**

Memory patterns hit: missing behavioral test for changed logic (covered);
WebUI Playwright companion (existing spec); incomplete change-class
(Playwright + Vitest present).

## Issues

None.
