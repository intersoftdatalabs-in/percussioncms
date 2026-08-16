# Erlang review: Explorer Publish Now selection + FORBIDDEN error region (#3467)

**Branch:** `fix/issue-3467-publish-now-forbidden-c5` (absorbed into cluster union with #3487)  
**Base:** `origin/main` (`d0838a584f`, includes #3465 + #3466)  
**Date:** 2026-08-16  
**Persona:** Erlang (independent of implementer)

Re-review of cherry-pick `809da1` + `4dc9fcb` onto current `main` (closed #3486/#3487 commits were not on `origin/main`).

## Summary

Cycle Verify residual of #3451 / PR #3465: Playwright could click injected
`detail-row-42` and visible **Publish Now** while `selection.item` stayed
empty (Sites folder). Dispatch then returned `ACTION_NEEDS_ITEM` and the
generic banner, never calling publish or mounting
`explorer-server-actions-error` with FORBIDDEN text.

This change:

1. Row click selects the list item (`onSelectItem` still gated on
   `canRead` after 4dc9fcb so aria-disabled NONE rows stay unselectable).
   Missing ACL tokens are readable; id compare is string-stable.
2. Dispatch reads `selectionRef` so toolbar invoke is not stale.
3. Toolbar **and** context-menu hide Publish Now until `resolvePublishKind`
   is page/asset (Sites-folder-only is not publishable).
4. `ACTION_NEEDS_ITEM` mounts `explorer-server-actions-error` (not only the
   generic banner). Thrown HTTP 200 FORBIDDEN still sets both and does not
   refresh.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral Vitest covers row select without ACL, toolbar
and context-menu hide until page selected, folder Publish Now does not
publish, and the existing FORBIDDEN 200 shell path. Playwright waits for
`data-selected-item-id="42"` before Publish Now. Product-docs updated.
C5 on H2 QA (`perc-matrix-cms-h2`, HTTP 200 HEALTH=healthy): 
`explorer-action-dispatch.spec.js` 2 passed (including FORBIDDEN) and
`test:golden` 2 passed. No Publish Now / sitemanage/publish server ERROR
in the test window.

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
