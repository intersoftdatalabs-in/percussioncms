# Erlang review: issue #2119 UI-TEST-01 residual A list panelErrMsg

**Branch:** `fix/issue-2119-list-panel-errmsg-ladder`  
**Date:** 2026-08-06  
**Reviewer persona:** Erlang (strict, independent)

## Summary

Expands seven Developer list-panel Vitest files to the full `panelErrMsg` ladder already used by `SearchesPanel` / `ContentTypesPanel` / `ViewsPanel`: session-redirect, ApiError status, `Error.message`, and non-Error fallback. No production UI or API changes.

## Scope

- `WebUI/src/test/ts/developer/ServerConfigsPanel.test.tsx`
- `WebUI/src/test/ts/developer/ControlsPanel.test.tsx`
- `WebUI/src/test/ts/developer/SitesPanel.test.tsx`
- `WebUI/src/test/ts/developer/WorkflowsPanel.test.tsx`
- `WebUI/src/test/ts/developer/PipelinesPanel.test.tsx`
- `WebUI/src/test/ts/developer/SharedFieldsPanel.test.tsx`
- `WebUI/src/test/ts/developer/SystemDefPanel.test.tsx`

**Base:** `origin/main`  
**Memory patterns hit:** peer-test closure (UI-TEST-01 ladder)  
**Cross-platform path review:** N/A — test-only, no file I/O or path joins.

## Recommendation

**approve**

## Gate

|             Check              |                         Result                          |
|--------------------------------|---------------------------------------------------------|
| Bugs                           | none                                                    |
| Behavioral tests for new logic | N/A — tests only; production already uses `panelErrMsg` |
| Non-portable paths             | none                                                    |
| Change-class companions        | Mirrors full ladder from Searches/ContentTypes peers    |
| May commit/push                | **yes**                                                 |

## Issues

None.
