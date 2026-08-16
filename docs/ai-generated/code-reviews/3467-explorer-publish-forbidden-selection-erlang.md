# Erlang review: Explorer Publish Now selection + FORBIDDEN error region (#3467)

**Branch:** `fix/issue-3467-publish-now-forbidden-error`  
**Base:** `origin/main` (`9e8a99a4c3`, includes #3465)  
**Date:** 2026-08-15  
**Persona:** Erlang (independent of implementer)

## Summary

Cycle Verify residual of #3451 / PR #3465: Playwright could click injected
`detail-row-42` and visible **Publish Now** while `selection.item` stayed
empty (Sites folder). Dispatch then returned `ACTION_NEEDS_ITEM` and the
generic banner, never calling publish or mounting
`explorer-server-actions-error` with FORBIDDEN text.

This change:

1. Always selects the clicked list row (`onSelectItem` not gated on
   `canRead`); missing ACL tokens are readable; id compare is string-stable.
2. Dispatch reads `selectionRef` so toolbar invoke is not stale.
3. Toolbar hides Publish Now until `resolvePublishKind` is page/asset.
4. `ACTION_NEEDS_ITEM` mounts `explorer-server-actions-error` (not only the
   generic banner). Thrown HTTP 200 FORBIDDEN still sets both and does not
   refresh.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral Vitest covers row select without ACL, toolbar
hide until page selected, folder Publish Now does not publish, and the
existing FORBIDDEN 200 shell path. Playwright waits for
`data-selected-item-id="42"` before Publish Now. Product-docs updated.

## Change-class closure

Change class: **WebUI Explorer selection + Publish Now error chrome**.

| Companion | Status |
|-----------|--------|
| Shell `selectionRef` + `data-selected-item-id` | present |
| Toolbar hide Publish Now when not publishable | present |
| Vitest (shell, DetailList, enablement, dispatch, selection) | present |
| Playwright `explorer-action-dispatch.spec.js` | updated |
| product-docs admin + rest | updated |

## Cross-platform path checklist

No filesystem path I/O. Publish and pathmanagement URLs use `/` (URL form).
**Outcome: clean.**

Memory patterns hit: missing behavioral test (covered); WebUI Playwright
companion (updated); incomplete change-class (product-docs + Vitest +
Playwright present).

## Issues

None.
