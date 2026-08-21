# Erlang review: #3668 Explorer Expire workflow transition HTTP 200

**Branch:** `fix/issue-3668-explorer-expire-transition`  
**Base:** `origin/main`  
**Scope:** Hibernate `PSTransitionsContext` first-row cursor + Playwright 200 gate  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** JDBC constructor/factory cursor parity; do not treat workflow 500 as honest skip; Playwright companion for Explorer chrome; product-docs for operator-facing Workflow invoke

## Summary

QA #2743 step 6: Explorer listed **Expire** for item 551 then `GET …/transitionWithComments/{id}/Expire` returned HTTP 500 `Failed to perform a transition` (`PSTransitionException` 7459 `INVALID_TRANSITION`). `#3639` unwrap already listed triggers; this residual is the **perform** path.

`PSExitPerformTransition` reads `getTransitionFromStateID()` immediately after `PSTransitionsContext.loadFromHibernate(wf, trigger, fromState)`. JDBC constructors called `moveNext()` before return so getters reflected the first row. Hibernate `populateFromHibernate` left the cursor at `-1`, so from-state stayed `0` and every user trigger (Expire first as Public default) failed.

Fix: when rows are non-empty, position on row 0 in `populateFromHibernate` (JDBC constructor parity). Unit test in `system`. Playwright requires HTTP 200 (Expire when listed). H2 C5: Expire contentId 551 Public→Archive `WF-4001` SUCCESS.

## Issues

None (no bugs, missing behavioral tests, or non-portable path/file I/O).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` **filesystem** path construction
- [x] URL / REST paths correctly use `/`
- [x] Helper unit tests assert encoded URL ids, not OS path strings
- [x] Temp files: none
- [x] Line-ending assertions: none

## Tests

- `system`: `PSTransitionsContextPopulateFromHibernateTest` (3 pass) in `cd system && ../mvnw.cmd clean install` — Tests run: 2289, Failures: 0
- `modules/extensions-workflow`: `cd modules/extensions-workflow && ../../mvnw.cmd clean install` — Tests run: 67, Failures: 0 (disabled Hibernate suite still skipped)
- perc-qa-automation Node: `explorer-workflow-transitions.test.js` 8 pass
- Playwright H2 C5: `explorer-workflow-transitions.spec.js` **2 passed, 0 skipped**; golden **2 passed**
- Live proof: `WF-4001` Expire item 551 Public→Archive SUCCESS; server.log ERROR count 0

## Change-class closure

| Companion | Status |
|-----------|--------|
| Hibernate cursor = JDBC first-row | done |
| System unit test for getters without `moveNext` | done |
| Playwright HTTP 200 (no 500-as-honest) | done |
| `product-docs/8.2/admin/content-explorer.md` Workflow invoke | done |
| perc-qa-automation README surface | done |

## Notes

- `downstream_checked`: grep `extends PSTransitionsContext` / anonymous subclass — none. `extensions-workflow` standalone clean install green (consumer of `loadFromHibernate`).
- Do not steal assigned QA #2743.
