# Erlang review: #4112 SPA Action Menus create/delete (UI-02)

**Scope:** `fix/issue-4112-spa-action-menu-create-delete` vs `origin/main` (stacked on #4171 / PR #4229 JAXB bind).
**Change class:** WebUI Developer catalog persist proof (create POST + delete GET 404) plus rest-jax-rs JSON provider companions from #4171.
**Memory patterns hit:** incomplete change-class closure (Playwright persist round-trip vs chrome-only); H2 POST JAXB unexpected-element (`allowedWorkflowTransitionsRequest`); no `window.confirm` (CatalogConfirmDialog peer).

## Summary

SPA create/delete chrome already ships on `main`. This slice stacks the open #4171 bind so Admin `POST /services/actions` is `ActionMenu`, then proves UI-02: catalog GET lists the created name; DELETE omits it (following GET 404); system Edit stays (409). Vitest covers catalog list-after-create and list-omit-after-delete. Out of scope: UI-03 tabs, UI-04 children.

## Recommendation

approve

## Gate

- Bugs: none
- Behavioral tests: Vitest `ActionMenusPanel` create+back lists row / delete omits row; `ActionMenuDetailPanel` 400/409/404/403/system already on main; Playwright `developer-action-menu-editor.spec.js` GET catalog + GET 404; rest `ActionMenuCreateCxfUnmarshallTest` from stacked #4171
- Cross-platform paths: N/A (no new filesystem I/O)
- May commit/push: yes

## Issues

None.

## Companions

- rest + sitemanage: JAXB skip Jettison default JSON provider (#4171)
- WebUI Vitest: catalog round-trip after POST/DELETE
- perc-qa-automation: H2 Playwright surface
- product-docs: `product-docs/8.2/admin/developer-action-menus.md` GET 404 after delete
- Dual-ship `WebUI/war`: N/A (no production TS/JSP change)

C2: no type made final/sealed; no public signature change. Reverse-dep `projects/sitemanage` is in the stacked JAXB change set.
