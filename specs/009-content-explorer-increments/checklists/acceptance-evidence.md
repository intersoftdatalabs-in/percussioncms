# US1 Acceptance Evidence — Goal-trio runs on H2 (T009–T012)

**Cell**: `perc-matrix-cms-h2` (`TEST_CMS_URL=http://127.0.0.1:9993`, `TEST_DB_TYPE=h2`), healthy per `qa-health` before and during runs. Torn down after (`qa-down`).
**Date**: 2026-11-11. **Branch under test**: `4530-content-explorer-increments` (spec-only delta; product code = `main`).

## Battery results (all passing)

| Spec | Result | Covers |
|------|--------|--------|
| explorer-takedown.spec.js | **4 passed** | Single takedown incl. HTTP 400 named-item failure, cancel no-op |
| explorer-multi-takedown.spec.js | **3 passed** | Multi takedown, skipped folders named, cancel takes nothing |
| explorer-staging.spec.js | **7 passed** | Single stage/unstage incl. HTTP 400 named-item failures, cancel no-ops |
| explorer-multi-stage.spec.js | **3 passed** | Multi stage, skipped folders named |
| explorer-multi-remove-staging.spec.js | **3 passed** | Multi unstage, skipped folders named |
| explorer-schedule.spec.js | **5 passed** | Set/clear, invalid-range refusal, named failures |
| explorer-multi-schedule.spec.js | **4 passed** | Multi schedule set + invalid range + cancel + named failure |

**Total: 29/29 passed. SC-001 satisfied for the goal trio (single + multi, success + named-failure paths).**

## Environment incident (resolved, not a product finding)

First battery run failed all 7 specs at the login gate (`perc-login-form` never rendered). Root cause: the QA cell shipped a **stale SPA** — its `perc-modern-ui.js` predated the login testids (and the #5942–#5944 400-mapping code), so the two `explorer-staging` 400 tests additionally showed raw error text. Fixed by deploying the freshly built `WebUI/target/generated-webui/cm/modern` (built 2026-11-11 from the working tree) into the cell WAR; no Jetty restart needed. Re-run: all green.

**Process note**: future `qa-up` runs for Playwright gates should use `--then-qa-deploy-webui` (or the equivalent manual copy) whenever the image build may carry a stale web bundle, or the gate measures the wrong code.

## SC-001 verdict

Takedown, stage/unstage, and schedule acceptance from the product Explorer route is **proven on H2** for this scope. Named-failure reporting (`Home (HTTP 400)` + action-specific failure text, selection kept, no refresh on failure) holds on all three flows.

## Phase 4 — QA-tail execution (T013/T014, 2026-11-11)

Cell rebuilt from current `main` (`37880de7f9`), fresh SPA deployed (same stale-bundle guard), `qa-health` OK. Evidence comments posted on both tickets (labels untouched — QA owns state transitions).

- **#4553** (stage/remove-from-staging verification): `explorer-staging.spec.js` 7 passed, `explorer-multi-stage.spec.js` 3 passed, `explorer-multi-remove-staging.spec.js` 3 passed — single + multi, success + named-failure paths, selection kept, no refresh on failure, no console errors. Covers the ticket's test plan items 4–9 (hidden-until-selection, page + asset, folder blocked, FORBIDDEN-path error without success refresh via mocked 400/403/404/409 cases).
- **#5799** (tree expand-all/collapse-all): `explorer-tree-expand-collapse.spec.js` 2 passed — expand reveals loaded descendants, collapse folds to roots, selection preserved, no console errors.
- No spawned/named QA beyond the frozen list as of this run (T015). SC-002 count reconciled in baseline-freeze (T016): remaining open in-scope QA is #4553 + #5799 pending QA sign-off — verification evidence now attached to both.
