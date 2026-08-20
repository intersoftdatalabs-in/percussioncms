# Erlang review: #3639 Explorer workflow transition no-skip on H2

**Branch:** `fix/issue-3639-explorer-workflow-transition-no-skip`  
**Base:** `origin/main`  
**Scope:** WebUI `itemWorkflowApi` Jackson unwrap + perc-qa-automation Playwright no-skip proof  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** Jackson WRAP_ROOT unwrap before reading list fields; skip-with-BUG / no silent soft-skip when fixture exists; URL paths use `/` (not OS separators); Playwright companion for WebUI screen; product-docs for operator chrome

## Summary

Parent #3102 remaining Action-bar operator proof. `explorer-workflow-transitions.spec.js` previously probed Sites-root folder rows and `FIXTURE_SKIP`ped when `action-toolbar-group-workflow` was absent. Folders are not workflow-eligible (`isWorkflowEligibleItem`). Live `GET .../workflow/getTransitions/{id}` is Jackson-wrapped as `{ ItemStateTransition: { transitionTriggers: [...] } }`; the Explorer client read `state.transitionTriggers` on the envelope and merged an empty Workflow group.

Product fix unwraps `ItemStateTransition` / `PSItemStateTransition` and coerces trigger list envelopes. Playwright walks Sites → Pages, prefers a REST-listed page with triggers, asserts the one-click Workflow group, invokes once (200 or 4xx reject), and asserts folder rows hide the group. H2 / listed-eligible must not skip. Gap-matrix Workflow row left **Present**. Human QA #2743 not assigned.

## Issues

None (no bugs, missing behavioral tests, or non-portable path I/O).

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` **filesystem** path construction
- [x] URL / REST paths correctly use `/`
- [x] Helper unit tests assert encoded URL ids, not OS path strings
- [x] Temp files: none
- [x] Line-ending assertions: none (JSON / testid strings)

## Tests

- WebUI Vitest: `itemWorkflowApi.test.ts` (flat DTO, Jackson wrap, JAXB `string` wrapper, blank id)
- perc-qa-automation Node: `tests/unit/explorer-workflow-transitions.test.js` (8 pass)
- Playwright H2 C5: `explorer-workflow-transitions.spec.js` **2 passed, 0 skipped**; golden smoke **2 passed**
- Invoke of Expire/Quick Edit on Public FastForward pages returns HTTP 500 `PSTransitionException` 7459 (invalid transition). Explorer shows `explorer-server-actions-error`. Treated as documented honest reject; not a fixture skip.

## Change-class closure

| Companion | Status |
|-----------|--------|
| `itemWorkflowApi` unwrap | done |
| Vitest for unwrap | done |
| Playwright no-skip + folder negative | done |
| Helper + unit tests + `package.json` `test:unit` | done |
| `product-docs/8.2/admin/content-explorer.md` | done (folders do not show Workflow) |
| perc-qa-automation README surface | done |
| Gap-matrix Present flip | not done (leave Present; do not touch unrelated rows) |

## Notes

- `downstream_checked`: none — no `final`/`sealed` or signature break on existing methods; new exports are additive unwrap helpers.
- Do not steal assigned QA #2743.
