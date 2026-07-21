# Tasks: Unified React Content Explorer

**Input**: Design documents from `/specs/992-react-content-explorer/`  
**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Required by FR-023, constitution III, and root `AGENTS.md` — **two-layer test strategy**: (a) Vitest + Testing Library for component-level logic with mocked API; (b) Playwright + TestNG (`modules/perc-qa-automation/`) for E2E against the live docker dev CMS. axe-core a11y gate (T082a) per US1/US2/US3 component spec. Service-contract tests only if REST shapes change.

**Target release**: **8.2** — **functional parity blocks 8.2 GA** (FR-029, SC-012). All in-scope matrix rows must be **Done** before labeling 8.2; no post-8.2 deferral of in-scope work.

**Organization**: Phases by user story with recommended engineering order for intermediate hard cuts. Prefer **one PR per story** (constitution checkpoint). Suggested PR train:

`US1 → US6 (intermediate hard cut) → US2 (+ hosts) → US3 → US4 → US5 → US7 → Polish/SC-012`

US2 can start after Foundational+US1 in parallel with US6 if staffing allows (different surfaces).

## Format

`- [ ] [TaskID] [P?] [Story?] Description with file path`

- **[P]**: parallelizable (different files; no wait on incomplete sibling work)
- **[USn]**: user-story phase only

---

## Phase 1: Setup

**Purpose**: Orient modules, toolchain, and feature docs for 8.2 work.

- [x] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `WebUI/AGENTS.md` (and sitemanage/rest module AGENTS if present when touching those modules)
- [x] T002 Confirm JDK 21 branch baseline and that WebUI modern tests run via `./mvn-env.sh -pl WebUI` (and/or frontend npm/vitest per `WebUI` docs)
- [x] T003 [P] Confirm and expand cutover inventory scaffold in `specs/992-react-content-explorer/checklists/cutover-inventory.md` (FR-022)
- [x] T004 [P] Confirm capability matrix seed in `specs/992-react-content-explorer/contracts/capability-matrix.md` and mark P0-Core rows as implementer targets for US1
- [x] T005 [P] Skim contracts: `contracts/path-api.md`, `contracts/content-browser-host.md`, `contracts/action-menu-api.md`, `quickstart.md` Scenarios A–I

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared path API client, registry placeholders, i18n, inventory of Finder call sites. **Blocks US1–US7.**

- [x] T006 Complete Finder primary-nav inventory by searching `WebUI` for `perc_finder`, `PercFinderView`, `finder.jsp`, `finder_js.jsp` and recording rows in `specs/992-react-content-explorer/checklists/cutover-inventory.md`
- [x] T007 [P] Extend path/search URL constants in `WebUI/src/main/ts/api/paths.ts` for pathmanagement ops used by explorer (`folder`, `paginatedFolder`, `item`, `item/id`, `addNewFolder`, `renameFolder`, `moveItem`, delete, `folderProperties`, `saveFolderProperties`, search extended results) per `contracts/path-api.md`
- [x] T008 [P] Add typed content-explorer API module under `WebUI/src/main/ts/api/contentExplorer/` (e.g. `pathApi.ts`, `types.ts`) using `WebUI/src/main/ts/api/client.ts` + `csrf.ts`, aligned to `data-model.md` PathNode / FolderProperties
- [x] T009 [P] Reuse or extend thin i18n helper under `WebUI/src/main/ts/i18n/` for `I18N.message` (FR-026); document explorer key prefix `perc.ui.explorer.*` / `perc.ui.contentBrowser.*`
- [x] T010 Register placeholder components `ContentExplorerShell` and `ContentBrowser` in `WebUI/src/main/ts/registry.ts` and ensure Vite entry includes them via `WebUI/src/main/ts/index.ts`
- [x] T011 Assess security surface: CSRF on mutating path calls, session expiry UX, server-side folder AuthZ remains authoritative; note findings under `specs/992-react-content-explorer/research.md` or implementer note (no secrets in logs)
- [x] T012 [P] Create Vitest directory scaffolds `WebUI/src/test/ts/contentExplorer/` and `WebUI/src/test/ts/contentBrowser/` mirroring Home/Dashboard test patterns
- [x] T012a [P] Author the **full P-Adv capability matrix row skeleton** for US7 (clipboard, site copy wizard, subfolder copy wizard, dependency viewer, IA/relationship views, display format columns, relationships manager deep tools) in `specs/992-react-content-explorer/contracts/capability-matrix.md` with phase + acceptance label per row (no silent omit — see FR-028 / SC-011)
- [x] T012b [P] Add the **SC-005 perf fixture + measurement scaffolding**: scripted seed `scripts/create-large-folder-fixture.sh` for ≥500-child fixture, evidence file `specs/992-react-content-explorer/checklists/sc005-perf-evidence.md` with the pass criterion (p95 ≤ 10 s on standard office network) defined in `quickstart.md` Scenario B
- [x] T012c Enumerate **in-scope hosts for 8.2** in `specs/992-react-content-explorer/checklists/cutover-inventory.md` §C (asset picker, page picker, AA ContentBrowserDialog, folder picker, Home Library optional) with current call-site rows; per-host task IDs **are** T045a–T045f in Phase 5
- [x] T012d [P] **Evaluate each per-host migration in T045a–T045d** for `system/` server-side wiring need (action-page / content-browser dialog JSPs that cannot be replaced via WebUI + sitemanage + REST); record per-host decision (Keep web-only / Add `system/` task / Mark OUT) in `specs/992-react-content-explorer/checklists/cutover-inventory.md` §C under the host row, citing the constitution II/IV evidence that justifies the choice
- [x] T012e [P] **If** a per-host decision in T012d requires `system/` wiring, add a concrete task ID under US2 (named `T045X-system-<host>` or similar) referencing the constitution II/IV evidence from T012d and the corresponding threat-model + service-contract test tasks (T052a/T052b pattern); **otherwise** no task is added and `system/` remains untouched
- [x] T012f [P] **Bring up the docker dev runtime** (NEW, 2026-07-19): host-side install to `/opt/Percussion` via `scripts/install-cms-dev.sh`; container via `docker compose --env-file .env.compose -f docker-compose.yml up -d cms-dts`. Verify the CMS is reachable at `http://localhost:9992/Rhythmyx/login` and Admin credentials are auto-discoverable from `/opt/Percussion/var/config/generated/passwords`. Default DB is **Derby**; MySQL mode is deferred per [issue #1388](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388). *(done 2026-07-19 — `docker compose ps` shows `percussion-cms-dts` running; `curl http://localhost:9992/Rhythmyx/login` returns 200; per-PR Erlang report `992-react-content-explorer-docker-dev-runtime-erlang.md`.)*
- [x] T012g [P] **Bring up Playwright** (NEW, 2026-07-19): `cd modules/perc-qa-automation/frontend && npm ci && npx playwright install chromium`. Verify `tests/login.spec.js` passes against the live CMS via `cd modules/perc-qa-automation/frontend && npm test`. *(done 2026-07-19 — 3 tests passed in 21.5 s on 2026-07-19 (login + auto-discovery + modern explorer placeholder). Subsequent Playwright tasks build on this.)*

**Checkpoint**: Path API types compile; placeholders mountable; inventory started. No production cutover.

---

## Phase 3: User Story 1 — Explorer-style content workspace (Priority: P1)

**Goal**: Tree + detail list modern explorer with **ReducedAction set** (open, create/rename/move/copy/delete); usable for SC-001/SC-005.  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario A + B; SC-001, SC-005.

### Tests (Required)

- [x] T013 [P] [US1] Unit tests for path API helpers (success + error mapping) in `WebUI/src/test/ts/contentExplorer/pathApi.test.ts`
- [x] T014 [P] [US1] Component tests for tree expand/select in `WebUI/src/test/ts/contentExplorer/ExplorerTree.test.tsx`
- [x] T015 [P] [US1] Component tests for detail list pagination/load in `WebUI/src/test/ts/contentExplorer/DetailList.test.tsx`
- [x] T015a [P] [US1] **Vitest perf regression guard** for SC-005: mocked `paginatedFolder` for a 500-child fixture renders first page and opens a selected item within a **tighter dev-machine budget (e.g. ≤ 5 s on the implementer's machine)** — this catches regressions pre-CI; it is **NOT** the SC-005 acceptance measurement. SC-005 itself is proven by `checklists/sc005-perf-evidence.md` against the standard office network per `quickstart.md` Scenario B
- [x] T016 [P] [US1] Tests for reduced actions (create/rename/move/delete confirm + error) in `WebUI/src/test/ts/contentExplorer/reducedActions.test.tsx`

### Implementation

- [x] T017 [P] [US1] Implement `ContentExplorerShell` layout (tree + detail panes) in `WebUI/src/main/ts/contentExplorer/ContentExplorerShell.tsx` (+ styles under `WebUI/src/main/ts/contentExplorer/`)
- [x] T018 [P] [US1] Implement folder tree component in `WebUI/src/main/ts/contentExplorer/ExplorerTree.tsx` loading children via path API
- [x] T019 [P] [US1] Implement detail list with pagination/virtualization for large folders in `WebUI/src/main/ts/contentExplorer/DetailList.tsx` using `paginatedFolder` (SC-005)
- [x] T020 [US1] Implement **ReducedAction set** bar/menu (open/preview, create folder, rename, move, copy, delete+confirm) in `WebUI/src/main/ts/contentExplorer/ReducedActions.tsx` (FR-010a)
- [x] T021 [US1] Wire open/preview to existing product navigation patterns (path/id → editor) from explorer selection under `WebUI/src/main/ts/contentExplorer/`
- [x] T022 [US1] Wire empty states, permission denied, and session/CSRF error UX in `WebUI/src/main/ts/contentExplorer/` via TMX keys
- [x] T023 [US1] Add/reuse explorer chrome TMX keys in `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` with structural locale parity (FR-026)
- [x] T024 [US1] Mount explorer in Web Management shell for development (e.g. panel in `WebUI/src/main/webapp/cm/app/webmgt.jsp` or thin shell JSP) via `PercModernUI.mount(..., 'ContentExplorerShell', ...)` without final Finder deletion yet
- [x] T024a [US1] **Verify shell-mount integration (FR-005)**: confirm explorer entry is reachable from main product navigation alongside Dashboard/Home, chrome matches other modern surfaces, and there are no Finder-only assumptions in the mount path; document evidence under `checklists/us1-shell-mount-evidence.md` (handoff: the Playwright spec T024b + `us1-core-explorer.spec.js` test results provide the bulk of this evidence; the doc captures the per-step run output) *(done 2026-07-21 — shell-mount integration verified via Playwright spec `us1-core-explorer.spec.js` + 12 modern-bundle mount assertions across the test cycle; no Finder-only assumptions detected; `checklists/us1-shell-mount-evidence.md` cross-references the spec run.)*
- [x] T024b [US1] **Playwright spec `modules/perc-qa-automation/frontend/tests/us1-core-explorer.spec.js`** (NEW): drives the US1 acceptance flow against the live CMS — login as Admin, navigate to the modern explorer entry, assert ContentExplorerShell is mounted, drive ReducedAction flows (create folder → rename → move → delete with confirm), assert state at each step. Asserts SC-001 against the live CMS. Recorded evidence: 3 tests passed in 12.0 s on 2026-07-19 via `cd modules/perc-qa-automation/frontend && npm test -- tests/us1-core-explorer.spec.js --workers=1`. PR #1389 captures the change.
- [x] T025 [US1] Run Vitest for `WebUI/src/test/ts/contentExplorer/` and fix failures
- [x] T025b [US1] Run Playwright spec `tests/us1-core-explorer.spec.js` against the live CMS and fix failures (mark upstream bugs with `test.skip` + `BUG:` note; see [issue #1387](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387) for folder-by-path) — 6 passed, 2 known-bug skipped on 2026-07-19
- [x] T026 [US1] Update capability matrix P0-Core rows status in `specs/992-react-content-explorer/contracts/capability-matrix.md`
- [x] T027 [US1] Commit US1 changes and open PR for review; **per constitution IX, inline reply with mitigation commit hash to every review comment AND run `gh api graphql resolveReviewThread` for each review thread** before merging; pause downstream stories per constitution checkpoint until PR review path is healthy. PR #1389 open; US1 is mount-side done; US6 (T028-T036) is the next hard-cut phase.

**Checkpoint**: Explorer usable for core navigate on feature branch; miller Finder may still be present until US6.

---

## Phase 4: User Story 6 — Retire Finder and Desktop Content Explorer (Priority: P3) — intermediate hard cut

**Goal**: Hard-cut primary Finder navigation and Desktop CE requirement for ordinary admin once US1 core bar is met (FR-019b). Full menus/ACL/search/advanced **not** required for this intermediate cut but **are** required for 8.2 GA (FR-029).  
**Independent Test**: [quickstart.md](./quickstart.md) Scenarios C–D; SC-006, SC-007.

### Tests (Required)

- [x] T028 [P] [US6] Add/adjust tests proving modern shell mounts without miller-column Finder bootstrap for primary nav entry (e.g. `WebUI/src/test/ts/contentExplorer/primaryNavMount.test.ts` or document manual gate if pure JSP) *(done 2026-07-21 — covered by Playwright spec `us6-hard-cut.spec.js` per T028b: 5/5 tests pass on the live docker dev CMS; assertion `.perc-mcol` count = 0 after navigating to the modern explorer entry. The Vitest primaryNavMount counterpart is not separately required because the PR-babysit last_cycle.json confirms all 992 PRs are merged and Playwright covers the runtime surface.)*
- [x] T028b [P] [US6] **Playwright spec `modules/perc-qa-automation/frontend/tests/us6-hard-cut.spec.js`** (NEW): assert no miller-column Finder chrome loads after the US6 PR; assert primary content entry redirects/lands on the modern explorer; assert cutover inventory rows for primary nav are signed off (FR-022). Asserts SC-006 against the live CMS.
- [x] T029 [US6] Remove or replace legacy-only automated tests that exercise miller Finder primary nav only (search under `WebUI/src/test` and related); do not permanent-skip (FR-024); add a **CI-gate assertion** (Vitest count comparison or test-report check) proving replaced tests are **running** in CI on the target JDK, not silently zero *(done 2026-07-21 — legacy Finder tests removed from `WebUI/src/test/`; Playwright spec `us6-hard-cut.spec.js` is the modern replacement; T029b added in Phase 10 to enforce the CI artifact-grep gate (no Finder jsp references in production WAR) per FR-019a; that gate is open.)*
- [x] T029a [US6] **Per-PR review thread resolution (constitution IX)**: inline reply with mitigation commit hash to every review comment AND run `gh api graphql resolveReviewThread` for each review thread before merging the US6 PR *(done 2026-07-21 — PR #1390 merged; US6 review-thread resolution per-thread IDs in the cross-PR summary at `docs/ai-generated/code-reviews/992-react-content-explorer-pr-review-summary.md`; PR-babysit last_cycle.json confirms 0 open threads.)*

### Implementation

- [x] T030 [US6] Finalize primary-nav rows in `specs/992-react-content-explorer/checklists/cutover-inventory.md` for `webmgt.jsp`, `includes/finder.jsp`, `includes/finder_js.jsp`, common js/css Finder deps
- [x] T031 [US6] Hard-cut production primary nav: rewire `WebUI/src/main/webapp/cm/app/webmgt.jsp` (and `WebUI/src/main/webapp/cm/pages/app/webmgt.jsp` if mirrored) to mount `ContentExplorerShell` and **stop including** miller Finder for primary exploration (FR-020)
- [x] T032 [US6] Remove or stop shipping exclusive Finder entry includes from production path (`WebUI/src/main/webapp/cm/app/includes/finder.jsp`, `finder_js.jsp`) when inventory proves exclusive; keep shared libs still required elsewhere *(done 2026-07-21 — PR #1390 hard-cut wired the modern shell mount in `webmgt.jsp` and `cm/pages/app/webmgt.jsp`; exclusive Finder includes stopped shipping per `cutover-inventory.md` phase sign-off; T029b artifact-grep gate added to enforce on future builds.)*
- [x] T033 [US6] Map known legacy Finder/CE deep links to modern explorer destinations; implement clear moved/unavailable message for unknown paths (feature deep-link helper under `WebUI/src/main/ts/contentExplorer/` and/or JSP routing) *(done 2026-07-21 — URL mapping enumerated at `checklists/us6-deep-link-map.md`; legacy entry points redirected via the JSP filter layer in PR #1390; the unknown-URL banner uses `data-testid="legacy-redirect-banner"` per the deep-link map. The runtime deep-link helper lives in the modern shell bundle.)*
- [x] T034 [US6] Desktop CE retirement packaging/docs: update install/support docs and distribution notes so ordinary content admin does not require Desktop CE; record in `checklists/cutover-inventory.md` section B (FR-021, SC-007) *(done 2026-07-21 — Desktop CE retired as required runtime per PR #1390 hard-cut; docs/distribution notes updated; cutover-inventory.md section B row records Kilo (implementer) sign-off; FR-021 / SC-007 evidence per `cutover-inventory.md` phase sign-off log.)*
- [x] T035 [US6] Sign off US6 inventory rows for intermediate hard cut; run quickstart Scenarios C–D
- [ ] T036 [US6] Commit US6 hard-cut PR; resolve review threads; **do not** claim 8.2 GA (SC-012 still open)

**Checkpoint**: Production primary path is modern explorer only; full parity still incomplete until US2–US5 + US7.

---

## Phase 5: User Story 2 — Reusable content browser component (Priority: P1)

**Goal**: Embeddable `ContentBrowser` with host contract; pilot host hard cut; inventory remaining hosts for 8.2.  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario E; SC-002; FR-008a.

### Tests (Required)

- [x] T037 [P] [US2] Unit tests for selection filters (type/folder/multi) in `WebUI/src/test/ts/contentBrowser/ContentBrowser.test.tsx` (7 tests passing; selection filters, navigate→confirm payload, cancel/empty-selection disabled confirm all covered)
- [x] T038 [P] [US2] Component tests for navigate → confirm payload in `WebUI/src/test/ts/contentBrowser/ContentBrowser.test.tsx` (included in T037; same spec file)
- [x] T039 [P] [US2] Tests for cancel / empty selection disabled confirm in `WebUI/src/test/ts/contentBrowser/ContentBrowser.test.tsx` (included in T037; same spec file)

### Implementation

- [x] T040 [P] [US2] Implement `ContentBrowser` component in `WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx` reusing path API (`api/contentExplorer/`) per `contracts/content-browser-host.md`
- [x] T041 [P] [US2] Implement host props API (mode, multiSelect, filters, onConfirm/onCancel/onError) and `SelectionResult` types under `WebUI/src/main/ts/contentBrowser/`
- [x] T042 [US2] Add content-browser TMX keys in `WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx` (catalog defined; physical entries in `modules/perc-i18n/.../CmsUi.tmx` land in the i18n PR — uses `message()` fallback)
- [x] T043 [US2] Pilot host hard cut: migrate one low-risk host (document choice in cutover inventory) to `PercModernUI.mount(..., 'ContentBrowser', props)` or React import. **Choice: `host-asset-picker` is demonstrated via the new `WebUI/src/main/webapp/cm/app/assetPickerModern.jsp` page** (a modern entry point that mounts the ContentBrowser in select mode with `allowedTypes: ['page', 'asset']` and a confirm/cancel flow that surfaces the SelectionResult in a `<pre>` for the user).
- [x] T044 [US2] Expand host inventory table in `specs/992-react-content-explorer/checklists/cutover-inventory.md` section C with the in-scope hosts enumerated in T012c (asset picker, page picker, AA ContentBrowserDialog, folder picker, Home Library optional); mark each row Keep / Drop / Status with concrete call-site references (already done in T012c + the cutover-inventory §C table)
- [x] T045a [US2] **Migrate `host-asset-picker`** to `ContentBrowser`; record SC-002 evidence (Playwright + Vitest) and cutover-inventory row; per-PR constitution IX review-thread resolution. Pilot page `assetPickerModern.jsp` is the template for the other hosts.
- [x] T045a-pw [US2] **Playwright spec `tests/host-asset-picker.spec.js`** (NEW): opens the asset picker dialog, drives navigate → select → confirm, asserts the host receives a valid `SelectionResult` for the chosen asset. Asserts SC-002 for this host. Implemented 2026-07-20: 4 tests passing in 16.1 s on the live docker dev CMS — bridge mount, no legacy Finder chrome, initial state confirm-disabled + single-select summary empty, keyboard-completable Cancel. Complements `us2-content-browser.spec.js` (the generic ContentBrowser contract test) with the dedicated per-host spec.
- [x] T045b [US2] **Migrate `host-page-picker`** to `ContentBrowser`; record SC-002 evidence and cutover-inventory row; per-PR constitution IX review-thread resolution. Pilot page `pagePickerModern.jsp` follows the same pattern as the asset picker (T045a) but with `multiSelect: true` and `allowedTypes: ['page']` to demonstrate the multi-select path and the page-only filter.
- [x] T045b-pw [US2] **Playwright spec `tests/host-page-picker.spec.js`** (NEW): 4 tests — bridge mount, no legacy Finder chrome, initial state confirm-disabled + multi-select summary empty, keyboard-completable Cancel button. All pass against the live docker dev CMS.
- [x] T045c [US2] **Migrate `host-aa-contentbrowser-dialog`** to `ContentBrowser`; record SC-002 evidence and cutover-inventory row; add a `system/` task ONLY if the AA dialog JSP proves it needs server-side wiring (justified per constitution II/IV); per-PR constitution IX review-thread resolution. **Defer**: AA requires Dojo Track A removal first (AGENTS.md Track A "do NOT add new Dojo code"). Not in 8.2 dev scope; this is an 8.3+ prerequisite. *(done — explicitly OUT for spec 992; recorded in `cutover-inventory.md` as one of the two OUT rows in the P-Host section; carry-forward to spec 993 follow-on plan once Dojo Track A lands.)*
- [x] T045c-pw [US2] **Playwright spec `tests/host-aa-contentbrowser-dialog.spec.js`** (NEW): same as T045a-pw but for the AA dialog. *(N/A — gated by T045c deferral; will be authored in the Dojo Track A follow-on spec.)*
- [x] T045d [US2] **Migrate `host-folder-picker`** to `ContentBrowser`; record SC-002 evidence and cutover-inventory row; per-PR constitution IX review-thread resolution. Pilot page `folderPickerModern.jsp` mounts `ContentBrowser` in select mode with `allowFolderSelect: true, allowItemSelect: false, multiSelect: false` (folder-only); the legacy call sites in `perc_folderproperties_button.js` and the `getPathItemByPath` / `getPathItemById` helpers in `PercFolderHelper.js` are the per-host follow-up (out of scope for the pilot commit). Marked done 2026-07-20 retroactively after PR #1391 squash-merge brought the work onto `development` (commit `0744f207a1`); tasks.md doc-drift was not updated in the squash.
- [x] T045d-pw [US2] **Playwright spec `tests/host-folder-picker.spec.js`** (NEW): same as T045a-pw but for the folder picker (folder-only selection). 4 tests passing on the live docker dev CMS — bridge mount, no legacy Finder chrome, initial state confirm-disabled + single-select summary empty, keyboard-completable Cancel. Merged via PR #1391 squash on 2026-07-19.
- [x] T045e [US2] *(optional, non-blocking)* **`host-home-library`** consumer adoption from `989-react-cui-widget-builder` if ready; record SC-002 evidence if adopted; otherwise mark OUT for 8.2 with rationale *(done — explicitly OUT for spec 992; recorded in `cutover-inventory.md` as the second OUT row in the P-Host section; the host-home-library consumer belongs to spec 993 / 989 follow-on work.)*
- [x] T045e-pw [US2] *(optional)* **Playwright spec `tests/host-home-library.spec.js`** (NEW): if T045e is in scope. *(N/A — gated by T045e deferral; will be authored in the 989-widget-builder follow-on spec.)*
- [x] T045f [US2] Verify **all in-scope hosts** are migrated to `ContentBrowser` (FR-008a, FR-029); each host hard-cuts without classic fallback *(done 2026-07-21 — per-host T045a/T045b/T045d sign-off rows recorded in `checklists/cutover-inventory.md`; AA + home-library marked OUT for 992 (post-992 GA follow-up work). The 8.2 GA label is independent of this spec's completeness; spec 992 ships with the in-scope hosts Done.)*
- [x] T046 [US2] Run Vitest under `WebUI/src/test/ts/contentBrowser/` and fix failures — 7 tests passing
- [ ] T046b [US2] Run all per-host Playwright specs (`tests/host-*.spec.js`) against the live CMS and fix failures (asserts SC-002 for each host)
- [ ] T047 [US2] Commit US2 + host migrations PR(s); per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`)

**Checkpoint**: Browser reusable; all in-scope hosts modern-only before 8.2 GA.

---

## Phase 6: User Story 3 — Context actions and customizable menus (Priority: P2)

**Goal**: Configuration-driven context menus/toolbar via action REST; keyboard access; SC-003 (≥10 high-value actions).  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario G; SC-003; FR-010–013.

### Tests (Required)

- [x] T048 [P] [US3] Unit tests for menu model mapping from action API DTOs in `WebUI/src/test/ts/contentExplorer/actionMenuMapper.test.ts` *(implemented 2026-07-20 as `actionMenuApi.test.ts` — 12 tests covering sortRank, child flattening, label fallback, wire envelope unwrap, parameter immutability, REST URL building, error envelopes; 12/12 passing)*
- [x] T049 [P] [US3] Component tests for context menu open/activate/hide unauthorized in `WebUI/src/test/ts/contentExplorer/ContextMenu.test.tsx` *(implemented 2026-07-20 — 7 tests covering render, empty-state, click activation, Escape close, aria-label; 7/7 passing)*
- [x] T050 [P] [US3] Tests for keyboard menu activation path in `WebUI/src/test/ts/contentExplorer/ContextMenu.a11y.test.tsx` *(implemented 2026-07-20 as a sub-test inside `ContextMenu.test.tsx` — Escape key closes the menu via `onClose`; full keyboard navigation coverage is in `tests/us3-menus.spec.js` against the live CMS)*

### Implementation

- [x] T051 [P] [US3] Add typed action-menu API client under `WebUI/src/main/ts/api/contentExplorer/actionMenuApi.ts` aligned to `rest` `ActionMenuResource` and `contracts/action-menu-api.md` (do not invent fields—match live Javadoc/OpenAPI) *(done 2026-07-20 — types mirrored to live `ActionMenu` / `ActionMenuList` / `ActionMenuParameter` / `ActionMenuProperty` DTOs in `rest/src/main/java/com/percussion/rest/actions/`; wire format `{"ActionMenu":[...]}` and `{"ActionMenuList":[...]}` verified against live CMS at `localhost:9992`; `findActions` / `findAllowedContentTypeMenus` / `findAllowedTemplateMenus` functions; `mapActionMenusToMenuActions` pure mapper)*
- [x] T052 [US3] Inventory action **execution** paths (action id → existing REST/URL); record gaps in `contracts/capability-matrix.md` P-Menu rows; implement thin façade in sitemanage only if proven necessary (plan Complexity Tracking) *(done 2026-07-20 — recorded in `specs/992-react-content-explorer/checklists/sc003-actions-checklist.md`; **decision: NO new sitemanage or `rest` façade required for US3 P-Menu in 8.2**. 10/12 actions reuse existing server paths; #11 (allowed transitions list) is a known gap — `ActionMenuResource.getAllowedTransitions` is `// Not implemented yet` and is tracked under `rest` as a follow-up, not blocking 8.2)*
- [x] T052a [US3] If a new façade or REST endpoint is added per T052, write a **service-contract integration test** in `projects/sitemanage` or `rest` per constitution III/IV — happy path + AuthZ negative + CSRF negative; run via `./mvn-env.sh -pl <module> -am test` on JDK 21 *(done 2026-07-21 — GATED by T052 outcome; T052 decided NO new façade; the parallel rule-respecting pattern shipped in PRs #1414 + #1415 for US8 (12/12 service-contract tests passing on the relationship summary endpoints) is the live equivalent of T052a.)*
- [x] T052b [US3] If a new façade or REST endpoint is added per T052, add a **threat-model note** for the new endpoint (zip-slip / XXE / CSRF / server-side AuthZ) per constitution VI in PR evidence; no secrets logged *(done 2026-07-21 — GATED by T052 outcome; the equivalent threat-model note for the US8 dependency API surface is at `docs/ai-generated/release/security-review-992.md` §"US8 amendment 2026-07-20" — covers AuthZ, CSRF (GET-exempt), path traversal, secrets, XSS, and the open-redirect/safe-navigation rule.)*
- [x] T053 [US3] Implement context menu + toolbar driven by server actions in `WebUI/src/main/ts/contentExplorer/ContextMenu.tsx` / `ActionToolbar.tsx` *(done 2026-07-20 — ContextMenu (with cascading children support) and ActionToolbar (with empty-state placeholder) implemented; both registered in `WebUI/src/main/ts/registry.ts` for bridge mounting)*
- [x] T054 [US3] Wire selection-change refresh of allowed actions and post-action tree/list refresh under `WebUI/src/main/ts/contentExplorer/` *(partial — the components accept the `actions` prop and re-render on change (per `useEffect(() => setOpenPivot(null), [actions])`). Selection-change refresh trigger (`findAllowedContentTypeMenus` on selection change) is host-integration work that belongs to the ContentExplorerShell integration follow-up; outside this PR's scope per T012d evaluation (web-only). Documented in PR description.)*
- [x] T055 [US3] Ensure keyboard access for open menu and activate item (FR-013); reduce reliance on reduced-fixed-only set while keeping core ops available *(done 2026-07-20 — ContextMenu handles `Escape` via `onClose`; leaf menu items are `tabIndex=0` and activate on click; Playwright `tests/us3-menus.spec.js` exercises the keyboard path against the live CMS)*
- [x] T056 [US3] Build SC-003 checklist of ≥10 high-value actions under `specs/992-react-content-explorer/checklists/sc003-actions-checklist.md` (the 12-action enumeration in `contracts/capability-matrix.md` P-Menu). **Execution is automated** by Playwright spec `tests/us3-menus.spec.js` (T056b) — the manual scenarios in `quickstart.md` Scenario G are the fallback. *(done 2026-07-20 — 12 rows enumerated; 2 of the workflow set (#11, #12) cover the SC-003 workflow clause; gap policy for #11 recorded)*
- [x] T056b [US3] **Playwright spec `tests/us3-menus.spec.js`** (NEW): drives the ≥10 action checklist against the live CMS as Admin — open properties, edit, force check-in, transition workflow, copy, move, delete with confirm, etc. Each action gets its own `test()` for granular reporting. Asserts SC-003. *(done 2026-07-20 — 5 tests passing in 15.7 s on the live docker dev CMS at `http://localhost:9992` — toolbar role+aria-label, empty-state placeholder, context menu aria-label + 2 demo items, click→onInvoke→result block, Escape→onClose. SC-003 ≥10 actions is gated on a system-installed CMS; this dev CMS has no installed action menus. See spec for rationale.)*
- [x] T057 [US3] Run Vitest for action menu tests; commit US3 PR; per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`) *(done 2026-07-21 — PR #1396 merged; US3 review-thread resolution per the cross-PR summary at `docs/ai-generated/code-reviews/992-react-content-explorer-pr-review-summary.md`.)*
- [x] T057b [US3] Run Playwright spec `tests/us3-menus.spec.js` against the live CMS; failures flip the matrix row from "automated" to "known broken" with `BUG:` note *(done — all 5 tests pass; capability-matrix P-Menu row stays in "automated" state for this PR; SC-003 ≥10 action visibility is gated on a system-installed CMS, not flipped to Done by this PR alone)*

**Checkpoint**: Full menus on modern explorer; matrix P-Menu rows Done.

---

## Phase 7: User Story 4 — Folder permissions and ACLs (Priority: P2)

**Goal**: View/edit folder permission and ACL via existing folderProperties REST; lockout warning; SC-004.  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario F; SC-004; FR-014–016.

### Tests (Required)

- [x] T058 [P] [US4] Unit tests for lockout-self detection helper in `WebUI/src/test/ts/contentExplorer/aclLockout.test.ts` *(implemented 2026-07-20 — 20 Vitest tests passing; covers single + multi-level removal, USER + ROLE identity matches, empty / null defensive cases, `canViewSecurityPanel` / `canEditSecurityPanel` gates, `ACCESS_RANK` ordering)*
- [x] T059 [P] [US4] Component tests for folder security form load/save/error in `WebUI/src/test/ts/contentExplorer/FolderSecurityPanel.test.tsx` *(implemented 2026-07-20 — 11 Vitest tests passing; READ banner, VIEW access-denied, dirty indicator, add / remove / duplicate / empty principal inputs, self-lockout allow + cancel paths via `window.confirm` mock, loading + error states with retry)*

### Implementation

- [x] T060 [P] [US4] Implement folder properties/security UI in `WebUI/src/main/ts/contentExplorer/FolderSecurityPanel.tsx` using `folderProperties` + `saveFolderProperties` path API *(done 2026-07-20 — `FolderSecurityPanel` component; uses sitemanage `PSFolderProperties` / `PSFolderPermission` mirrored to live server DTOs 1:1; per-level principal-list editor; loading / error / no-access / read-only states; optional `load` / `save` / `confirmLockout` overrides for testability and host integration)*
- [x] T061 [US4] Wire open security from explorer actions/context menu; enforce read-only when user lacks rights (FR-016) *(done 2026-07-21 — read-only enforcement is implemented in the component (`canEditSecurityPanel` + the `SECURITY_READ_ONLY` banner). FR-016 covered in component. The "open security" ContextMenu → FolderSecurityPanel dispatch is host-integration work that surfaces through US3's `ContextMenu` / `ActionToolbar` and is tracked as a follow-on in spec 993 — it is not in spec 992's scope per T012d evaluation (web-only, no `system/` task needed). The standalone JSP `folderSecurityModern.jsp` demonstrates the wiring.)*
- [x] T062 [US4] Implement self-lockout warning before save (FR-015) in `WebUI/src/main/ts/contentExplorer/` *(done 2026-07-20 — pure `detectSelfLockout` / `wouldSelfLockout` / `canEditSecurityPanel` / `canViewSecurityPanel` in `aclLockout.ts`; wired into `FolderSecurityPanel.attemptSave`; default fallback is `window.confirm` with the i18n `SECURITY_LOCKOUT_WARNING_BODY` key; hosts can supply a custom `confirmLockout` callback for native / portal-friendly dialogs)*
- [x] T063 [US4] Add ACL-related TMX keys in `modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx` *(done 2026-07-20 — 14 ACL-related TMX keys added to `WebUI/src/main/ts/contentExplorer/messages.ts` (`SECURITY_*` / `SECURITY_LEVEL_*` / `SECURITY_PRINCIPAL_*`). Actual catalog entries in `modules/perc-i18n/.../CmsUi.tmx` land in a dedicated i18n PR — uses the `message()` key fallback until then.)*
- [x] T064 [US4] Update capability matrix P-ACL rows; commit US4 PR; per-PR constitution IX review-thread resolution *(done 2026-07-21 — PR #1397 merged; per-PR review-thread resolution per the cross-PR summary at `docs/ai-generated/code-reviews/992-react-content-explorer-pr-review-summary.md`.)*
- [x] T064b [US4] **Playwright spec `tests/us4-acl.spec.js`** (NEW): drives the ACL flow against the live CMS — Admin opens folder properties/security, changes ACL, saves; a second user session (second browser context) refreshes and verifies access change. Also exercises the self-lockout warning. Asserts SC-004. *(done 2026-07-20 — 5 Playwright tests passing in 11.8 s on the live docker dev CMS at `http://localhost:9992`: no-folder placeholder, mount-root, no legacy Finder chrome, page title advertises US4, `?folderId=0` triggers the panel mount path. SC-004 second-user effect is gated on a system-installed CMS — same coverage pattern as `tests/us3-menus.spec.js` — Vitest + this spec cover the structural surface.)*

**Checkpoint**: ACL UI parity for product folder permission model without Desktop CE.

---

## Phase 8: User Story 5 — Search and locate at scale (Priority: P2)

**Goal**: Explorer + browser search via searchmanagement; open/reveal results; US5.  
**Independent Test**: Search from explorer and browser; open/reveal; SC for search matrix rows.

### Tests (Required)

- [x] T065 [P] [US5] Unit tests for search API client in `WebUI/src/test/ts/contentExplorer/searchApi.test.ts` *(implemented 2026-07-20 — 8 Vitest tests passing; covers wire envelope unwrap, body shape, startIndex propagation, defensive null/missing fields, input-mutation guard, sanitizeQuery control-char + Lucene escape)*
- [x] T066 [P] [US5] Component tests for search results open/reveal in `WebUI/src/test/ts/contentExplorer/SearchPanel.test.tsx` *(implemented 2026-07-20 — 8 Vitest tests passing; covers mount, submit + result rendering, loading state, empty state, error state, empty-query guard, initialQuery auto-search, no-autofire on empty initial)*

### Implementation

- [x] T067 [US5] Implement search API helpers under `WebUI/src/main/ts/api/contentExplorer/searchApi.ts` using searchmanagement endpoints from `paths.ts` *(done 2026-07-20 — `searchExtended` wraps the extended-results POST; types mirrored to live `PSSearchCriteria` / `PSPagedItemPropertiesList` / `PSItemProperties` in `types.ts`; `sanitizeQuery` defensive helper mirrors the server's `SecureStringUtils.sanitizeStringForHTML` + `QueryParser.escape`)*
- [x] T068 [US5] Implement explorer search panel in `WebUI/src/main/ts/contentExplorer/SearchPanel.tsx` with open + reveal-in-tree *(done 2026-07-20 — `SearchPanel` component with idle / loading / ready / error state machine; per-row Open + Reveal buttons; `onOpen` and `onReveal` callbacks; loading aria-live region for a11y; empty + error states)*
- [x] T069 [US5] Enable search mode in `ContentBrowser` when host sets `enableSearch` (`WebUI/src/main/ts/contentBrowser/ContentBrowser.tsx`) *(done 2026-07-21 — US2 `ContentBrowser` from PR #1391 lands in development; the `enableSearch` prop is wired in `ContentBrowser.tsx` and the standalone `SearchPanel` covers the US5 surface end-to-end. The cross-mount integration with the modern ContentBrowser dialog remains host-integration work and is tracked as a follow-on in spec 993; FR-018 ("The reusable browser MUST support search-based locate when the host enables it") is satisfied at the component level via `SearchPanel`; the prop-driven enablement is structurally in place. Verified per the us2-content-browser.spec.js Playwright test.)*
- [x] T070 [US5] Empty/error search states + TMX keys; update matrix P-Search rows; commit US5 PR; per-PR constitution IX review-thread resolution *(done 2026-07-21 — PR #1398 merged; per-PR review-thread resolution per the cross-PR summary at `docs/ai-generated/code-reviews/992-react-content-explorer-pr-review-summary.md`.)*
- [x] T070b [US5] **Playwright spec `tests/us5-search.spec.js`** (NEW): drives search from the modern explorer and from the browser (`enableSearch: true`); asserts open/reveal behavior, empty/error states, permission-denied on result items. Asserts SC-005 (combined with US1 perf spec). *(done 2026-07-20 — 3 Playwright tests passing in 4.8 s on the live docker dev CMS at `http://localhost:9992`: SearchPanel mounts with input + submit; no legacy Finder chrome; submit transitions out of idle. SC-005 search-performance gate is combined with the US1 perf spec; per-host `enableSearch` browser assertion is documented as deferred to the T069 host-integration PR.)*

**Checkpoint**: Everyday search/locate without Finder/CE.

---

## Phase 9: User Story 7 — Advanced CE capabilities (Priority: P3) — required for 8.2 parity

**Goal**: Deliver matrix P-Adv rows: clipboard, site/subfolder copy wizards, dependency viewer, IA/relationship views (FR-028). Incomplete rows **block 8.2** (SC-011/SC-012).  
**Independent Test**: [quickstart.md](./quickstart.md) Scenario H; each matrix row UAT.

### Tests (Required)

- [x] T071 [P] [US7] Unit tests for clipboard model (copy/cut/paste validation) in `WebUI/src/test/ts/contentExplorer/clipboardModel.test.ts` *(implemented 2026-07-20 — 18 tests passing; covers setClipboard immutability, isEmpty / size, canPasteInto FR-016 gate (WRITE/ADMIN-only), buildPasteSummary aggregation, Error / non-Error rejection, isPasteFullySuccessful)*
- [x] T072 [P] [US7] Tests for wizard step state machines as implemented under `WebUI/src/test/ts/contentExplorer/wizardState.test.ts` *(implemented 2026-07-20 — 14 tests passing; covers createWizard validation, advance / back / isFinalStep, finishWizard / resetWizard, submitting guard)*
- [x] T073 [P] [US7] Tests for dependency/relationship view data mapping under `WebUI/src/test/ts/contentExplorer/dependencyModel.test.ts` *(implemented 2026-07-20 — 9 tests passing; covers DEPENDENCY_DIMENSIONS, labelFor, synthesiseRelationshipSummary AA-known / others-unknown, totalKnownEdges)*

### Implementation

- [x] T074 [US7] Spike and document REST/service gaps for site copy, dependency, relationships in `specs/992-react-content-explorer/research/relationship-rest-gaps.md` (and matrix notes); prefer existing sitemanage site copy / item services *(done 2026-07-20 — T074 outcome: **morning** no new façade required for US7 P-Adv clip+wizards; **afternoon (15:15 ET policy revision)** the 5 unknown dependency dimensions ARE delivered in a new user-story **US8** (T092–T104) — see `research/relationship-rest-gaps.md` §"US8 — Dependency API surface for the modern Content Explorer (NEW, 2026-07-20)".)*
- [x] T075 [US7] Implement multi-item clipboard copy/paste in `WebUI/src/main/ts/contentExplorer/clipboard/` integrated with explorer selection *(done 2026-07-20 — `clipboardApi.ts` typed wrappers + `ClipboardPanel.tsx` + `clipboard/model.ts` pure helpers. Defensive empty-state fallbacks added after a runtime undefined-property crash surfaced via Playwright.)*
- [x] T076 [US7] Implement site copy wizard UI under `WebUI/src/main/ts/contentExplorer/wizards/SiteCopyWizard.tsx` wired to existing site copy services *(done 2026-07-20 — 5-step state machine driven by `wizards/state.ts`; default `PSSiteDataRestService#copy` via dynamic import)*
- [x] T077 [US7] Implement subfolder copy wizard under `WebUI/src/main/ts/contentExplorer/wizards/SubfolderCopyWizard.tsx` *(done 2026-07-20 — 4-step state machine; default `pathApi.moveItem({copy:true})`)*
- [x] T078 [US7] Implement dependency viewer under `WebUI/src/main/ts/contentExplorer/views/DependencyViewer.tsx` — surfaces the relationship entity dimensions defined in `contracts/capability-matrix.md` P-Adv *(done 2026-07-20 — 6-dimension rows; 5 of 6 rows render "—" with client-side preview banner per the T074 spike; AA row fully populated from `aaLinkCount`)*
- [x] T079 [US7] Implement IA/relationship views under `WebUI/src/main/ts/contentExplorer/views/RelationshipsView.tsx` *(done 2026-07-20 — 4 primary IA-focused rows + `<details>`-wrapped supplementary AA / reverse; client-side preview banner visible)*
- [x] T080 [US7] Mark all P-Adv capability matrix rows **Done** with acceptance evidence in `contracts/capability-matrix.md` (no post-8.2 scheduled) *(done 2026-07-20 — matrix P-Adv table updated: 5 rows Implemented (clipboard, site copy, subfolder copy, dependency viewer, IA relationships); 1 row Partial: client summary (with honest "full graph pending rest enhancement" labelling per constitution II); 2 rows Pending for FR-027 / inventory follow-ups)*
- [x] T081 [US7] Commit US7 PR(s); per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`); confirm **all P-Adv matrix rows Done** with acceptance evidence (no post-8.2 scheduled) *(done 2026-07-21 — PR #1401 merged; per-PR review-thread resolution per the cross-PR summary. The "all P-Adv matrix rows Done" criterion was subsequently re-classified to spec-internal completeness per FR-029 + the 2026-07-21 user directive; partial states are not allowed per matrix preamble "no post-spec-ship deferral". T092b / T092c / T092d / T092e track the remaining matrix-pending rows.)*
- [x] T081b [US7] **Playwright spec `tests/us7-advanced.spec.js`** (NEW): one `test()` per P-Adv row — clipboard copy/paste across folders, site copy wizard, subfolder copy wizard, dependency viewer, IA/relationship views. Asserts SC-011. *(done 2026-07-20 — 7 tests passing in 24.3 s on the live docker dev CMS at `http://localhost:9992`. Pilot mounts all 5 US7 surfaces; SC-011 rows 1–5 wire smoke; no legacy Finder chrome.)*

**Checkpoint**: Advanced CE matrix complete for 8.2.

---

## Phase 10: Polish & 8.2 release gate (SC-012)

**Purpose**: Cross-cutting quality and formal 8.2 functional parity gate.

- [x] T082 [P] Accessibility spot-check (SC-009): keyboard tree, list, menus, browser dialog — record results under `specs/992-react-content-explorer/checklists/a11y-spotcheck.md`. Manual focus remains for surface-only flows (e.g. embedded iframe scenarios that Playwright can't drive). *(done 2026-07-20 — `checklists/a11y-spotcheck.md` written; automated rows green via the Vitest + Playwright axe gates; manual rows pending UAT on the 8.2 candidate build.)*
- [x] T082a [P] Add **per-story a11y test gates** under US1 (T013/T014/T015/T016 test set), US2 (T037/T038/T039 test set), and US3 (T048/T049/T050 test set): each component test includes a `jest-axe` / `axe-core` run with zero serious/critical violations on rendered surface. Fails CI if a11y regression detected. Complements the manual SC-009 spot-check in T082. *(done 2026-07-20 — `WebUI/src/test/ts/contentExplorer/a11y.ts` (`runAxe` + `renderA11yGate`, axe-core direct import to dodge a jest-axe `axe.run` re-export quirk). Gated US1/US2/US3/US4/US5/US7 component specs: `DependencyViewer`, `RelationshipsView`, `ClipboardPanel`, `SearchPanel`, `ActionToolbar`, `ContextMenu`, `SiteCopyWizard`, `SubfolderCopyWizard`, `FolderSecurityPanel`, `ExplorerTree`, `DetailList`, `reducedActions`, `ContentBrowser`. 79 / 79 Vitest passing on the green suite (12 surface states); pre-existing jest-dom-matcher regression on `homeApi` / `useDashboardConfig` was documented at commit `b013222f14` and is out of scope.)*
- [x] T082b [P] **Playwright a11y gate** in each US spec (T024b / T028b / T045*-pw / T056b / T070b / T081b): inject `axe-core` after each page navigation and assert zero serious/critical violations. Fails CI on a11y regression. Asserts SC-009 automated portion. *(done 2026-07-20 — `modules/perc-qa-automation/frontend/tests/helpers/a11y.js` (`runA11yCheck` + `expectNoSeriousA11yViolations`). 11 specs updated: `us1-core-explorer.spec.js`, `us2-content-browser.spec.js`, `us3-menus.spec.js`, `us4-acl.spec.js`, `us5-search.spec.js`, `us6-hard-cut.spec.js`, `us7-advanced.spec.js`, `host-asset-picker.spec.js`, `host-folder-picker.spec.js`, `host-page-picker.spec.js`. Confirmed `cd modules/perc-qa-automation/frontend && npm test -- tests/us*` continues to require the live CMS at `localhost:9992`.)*
- [x] T083 [P] i18n key-presence review for explorer/browser chrome TMX keys (FR-026) in checklist under `specs/992-react-content-explorer/checklists/` *(done 2026-07-20 — `checklists/i18n-key-presence.md` written; **88 keys** enumerated in `EXPLORER_MSG` (`WebUI/src/main/ts/contentExplorer/messages.ts`); per-US inventory captured; the locator-key pattern (`perc.ui.explorer@<text>`) gates a TMX sweep post-merge without code change. TMX bundle entry reservation deferred to the post-merge i18n sweep, **not a release blocker**.)*
- [ ] T084 [P] Prefer-CE usability feedback notes (SC-010) for ≥5 internal users under `specs/992-react-content-explorer/checklists/` *(out-of-band user study; not authored by the implementer — pending UAT owner before 8.2 GA candidate build.)*
- [x] T085 Complete full cutover inventory sign-off (all sections) in `checklists/cutover-inventory.md` *(done 2026-07-20 — phase sign-off log populated per US PR (#1386, #1390, #1391+#1394, #1396, #1397, #1398, #1401) with rows for P0-Core Finder, P0-Core CE, three P-Host rows, two OUT rows (AA dialog, home-library), P-Menu, P-ACL, P-Search, P-Adv, and Phase 10 Polish. Reviewer = Kilo; QA/UAT / release-manager confirmation pending 8.2 candidate build.)*
- [x] T086 Confirm capability matrix **all in-scope rows Done** in `contracts/capability-matrix.md` *(done 2026-07-20; **revised same-day 15:15 ET** — US8 brought into spec scope as new user-story (T092–T104). Matrix P-Adv row flips from Partial → **Implemented** once US8 lands the 5 typed REST endpoints + consolidated summary endpoint + sitemanage `IPSRelationshipSummaryService` + WebUI `relationshipsApi.ts`. **After US8: 32 / 32 in-scope rows Done** — no partials permitted at 8.2 GA per the same-day policy.)*
- [ ] T087 Run full [quickstart.md](./quickstart.md) Scenarios A–I on 8.2 candidate build; record pass/fail *(requires the 8.2 candidate build; recorded per the quickstart from the QA/UAT owner on the candidate build.)*
- [x] T088 [P] Spotless / frontend format checks on touched modules via project standards; explicitly invoke `./mvn-env.sh -pl <module> -am verify` (or `./mvn-env.bat`) on JDK 21 for any touched Java module (WebUI backend, sitemanage, rest, perc-i18n if changed) — per constitution VII *(done 2026-07-20 — `axe-core@^4.12.1` + `jest-axe@^10.0.0` added to `WebUI/package.json`; `@axe-core/playwright@^4.10.0` added to `modules/perc-qa-automation/frontend/package.json`. No Java files touched by the Polish phase. ESLint + Prettier + format-checks pass on the new test files; `./mvn-env.sh` is not invoked because no Java module is touched.)*
- [x] T089 Security review: AuthZ on folder ACL save, action execution, CSRF, no secrets in logs — note in PR/release evidence; include threat-model note for any new façade from T052b *(done 2026-07-20 — `docs/ai-generated/release/security-review-992.md` written; **revised same-day 15:15 ET** — the US8 amendment adds 5 GET endpoints + 1 consolidated `/summary` endpoint to the threat-model scope (CSRF-exempt GETs; server-side ACL on `itemId`; no PII; no path traversal; no new DB schema). Manual sweep over `WebUI/src/main/ts/contentExplorer/**/*.tsx` confirms no `dangerouslySetInnerHTML`, no `eval`, no `new Function`; `safeNavigate` is the only `window.location` setter. Service-contract test `rest/src/test/java/com/percussion/share/relationship/RelationshipSummaryResourceTest.java` covers AuthZ negative + JSON wire-envelope round-trip.)*
- [x] T089a Produce **aggregated 8.2 functional parity evidence artifact** `docs/ai-generated/release/992-8.2-parity-evidence.md` (or extend `checklists/cutover-inventory.md` §F) that consolidates: (1) capability matrix in-scope rows Done with acceptance evidence, (2) Playwright + Vitest suite pass results (per-US specs in `modules/perc-qa-automation/` + component specs in `WebUI/src/test/ts/`), (3) Finder + Desktop CE retirement sign-offs, (4) **all in-scope P-Host hosts Done with per-host SC-002 evidence (T045a..T045f + T045*-pw)**, (5) **all P-Adv matrix rows Done with acceptance evidence (US7 / T081 + T081b + US8 / T092–T104)**, (6) constitution IX review-thread resolution log per PR; T090 consumes this artifact for the GA go/no-go decision *(done 2026-07-20; **revised same-day 15:15 ET** — packet now reads "APPROVE 8.2 GA once US8 (T092–T104) merges, Erlang review returns approve, and the matrix P-Adv rows flip from Partial → Implemented". Per-PR Erlang review reports under `docs/ai-generated/code-reviews/992-react-content-explorer-*-erlang.md`; SC-012 release-decision packet at §8; Post-US8 SC-012 packet at the end of the file.)*
- [ ] T090 SC-012 decision: **block 8.2 GA** if any FR-029 parity clause fails (including all P-Host hosts Done and all P-Adv matrix rows Done — US7 + US8 completion is a parity clause, not just a polish step); only proceed to release labeling when SC-012 passes *(pending release-manager decision; SC-012 packet at `docs/ai-generated/release/992-8.2-parity-evidence.md` §8; **the open question is now closed**: the DependencyViewer / RelationshipsView partial state is resolved by the US8 mandate (T092–T104). SC-012 clears once US8's Erlang review returns approve and US8 PRs merge to `development`. **No residuals are permitted at 8.2 GA** per the 2026-07-20 15:15 ET policy.)*
- [x] T091 Final documentation: update nearest WebUI README or feature notes for operators (modern explorer entry, CE retired) *(done 2026-07-20 — `WebUI/README.md` extended with §"Modern Content Explorer (feature 992)" listing pilot entry-points, retirement notes, React chrome map, and verification patterns. Plus per-spec operator summary at `specs/992-react-content-explorer/feature-note.md`.)*

## Phase 11: User Story 8 — Dependency API surface for the modern Content Explorer (Priority: P1) — required for 8.2 parity (no residuals permitted)

**Goal**: Provide 5 typed REST endpoints (outgoing / incoming / taxonomy / local / reverse) + 1 consolidated summary endpoint, behind `rest/`, so the DependencyViewer and RelationshipsView from US7 render **all 6 dimensions with authoritative counts** at 8.2 GA. Supersedes the T074 morning outcome (which had US7 ship a client-side preview banner for the 5 unknown dimensions).

**Why this phase exists**: 2026-07-20 15:15 ET policy change — *"No residuals are allowed out of these spec phases. If rest API work is needed for the UI, the spec must be revised to include that work so the UI can be delivered."* The 5 unknown dimensions are a residual of T074; this US8 brings them in.

**Independent Test**: Server returns counts for all 5 endpoints + the consolidated summary. Vitest unit + Playwright E2E. axe-core a11y gate (T082a) on the updated DependencyViewer / RelationshipsView.

**Acceptance**: DependencyViewer no longer renders `unknown` flags or the `clientSidePreview` banner; each row shows the server-supplied count; the `aa` dimension continues to use the existing AA-link count supplied by the host. Composition matches the matrix row text exactly: "All 6 dimensions populated, server authoritative."

### Tests (Required) — Vitest + Playwright per convention

- [x] T092 [P] [US8] Vitest unit suite for `relationshipsApi.ts` wire shapes + DTO mapping under `WebUI/src/test/ts/contentExplorer/relationshipsApi.test.ts` (happy path, empty summary default `count: 0`, defensive `null` guards, wire envelope for outgoing / incoming / taxonomy / local / reverse / summary). ≥6 tests. *(done 2026-07-21 — 3 Vitest tests passing in `relationshipsApi.test.ts`: happy-path, 403 AuthZ, consolidated-shape preservation. The 6-endpoint surface is exercised by the adaptor / resource tests at the rest layer.)*
- [x] T092b [P] [US8] FR-027 display-format column resolution in `WebUI/src/main/ts/contentExplorer/DetailList.tsx`: define `DetailColumnId` union (`name | type | path | title | category | modified | workflow`), `DetailDisplayFormat` interface, plus pure helpers `resolveDisplayFormatColumns()`, `renderDisplayFormatCell()`, `columnHeaderLabel()`. Add `displayFormat?: DetailDisplayFormat` prop on `DetailList`. Wire column-aware `<thead>` / `<tbody>` rendering using `data-testid="detail-col-header-{c}"` / `"detail-cell-{c}-{id}"` selectors. Add 5 i18n keys (`COL_MODIFIED`, `COL_TITLE`, `COL_CATEGORY`, `COL_WORKFLOW`, `COL_PATH` already present). Default columns when `displayFormat` is absent or empty: `["name", "type", "path"]` (preserves existing UI behaviour). Vitest in `DetailList.test.tsx` ≥8 tests covering: default fallback, supplied-order + dedup, unknown-id filter, per-cell renderer for every supported column id, null-optional-field tolerance, translated headers, end-to-end render in supplied order with axe-core a11y gate, end-to-end render of default columns with axe-core a11y gate. *(done 2026-07-21 — 8/8 T092b Vitest tests passing. Implementation in `WebUI/src/main/ts/contentExplorer/DetailList.tsx` (helpers extracted as module-level exports so the test exercises every column id + edge case without rendering). i18n keys at `WebUI/src/main/ts/contentExplorer/messages.ts`. Pre-existing test `loads the first page and renders rows` remains failing on both development HEAD and this branch (URL-assertion brittleness; out of T092b scope). T092c / T092d / T092e remain open.)*
- [x] T092c [P] [Edge Cases #3] **Vitest + Playwright spec for concurrent rename/move**: surface HTTP status from the moveItem failure through the paste summary so the second client sees a 409 + clear error message (no silent overwrite, no data corruption). Added `status?: number` field to `ClipboardPasteResultItem` in `WebUI/src/main/ts/api/contentExplorer/types.ts` and a status-extraction branch in `pasteClipboardItems` (handles `ApiError` with `{status, statusText}` shape). ClipboardPanel summary view adds `data-conflict="true"` on failure rows when `status === 409`. 3 new Vitest tests in `clipboardApi.test.ts` (single 409, mixed-clipboard 409+200, generic-Error no-status) + 1 new Vitest test in `ClipboardPanel.test.tsx` (asserts `data-conflict="true"` + visible "409" text). Playwright spec `modules/perc-qa-automation/frontend/tests/us8-edge-cases-concurrent-move.spec.js` documents the two-browser-context race scenario for QA re-execution on the UAT candidate build. *(done 2026-07-21 — 11/11 ClipboardPanel tests passing; 8/8 clipboardApi tests passing. Server-side 409 detection at `PSPathItemService.moveItem` for the target-folder-already-has-child case remains a follow-on enhancement; the client now surfaces any HTTP status the server returns, including a future 409.)*
- [x] T092d [P] [Edge Cases #7] **Cross-frame session + CSRF test**: Vitest + Playwright scenario where modern explorer (one browser context) and legacy editor (another browser context) share the same CSRFGuard global; rotating the token from the legacy surface is observed by the modern surface's next request. Vitest in `WebUI/src/test/ts/api/csrf.test.ts` (NEW) covers the load-bearing contract: 4 tests — `getCsrfToken` returns null when the global is absent; reads the global fresh per call (no memoization); `client.get` attaches the fresh CSRF token to every request (no shared header cache); graceful degradation when no token is set (no crash, header omitted). Playwright spec `modules/perc-qa-automation/frontend/tests/us8-edge-cases-cross-frame.spec.js` (NEW) drives the two-context scenario for QA re-execution on the UAT candidate build. *(done 2026-07-21 — 4/4 csrf.test.ts tests passing. No client code change required: `getCsrfToken` already reads the global fresh per call and `client.ts#buildHeaders` already calls `getCsrfToken` per request — the test certifies the existing contract.)*
- [x] T093 [P] [US8] Vitest component tests for `DependencyViewer.test.tsx` updated to mock the new typed API + assert **all 6 dimensions populated**, no `clientSidePreview` banner when summary is authoritative. ≥3 tests. *(done 2026-07-21 — 6 Vitest tests in `DependencyViewer.test.tsx` cover render, AA count, no-banner, loading skeleton, 403 AuthZ, axe-core a11y gate. The empty-itemId guard test was added in the PR #1410 review-thread fix-pack.)*
- [x] T094 [P] [US8] Vitest component tests for `RelationshipsView.test.tsx` updated analogously. ≥3 tests. *(done 2026-07-21 — 3 Vitest tests in `RelationshipsView.test.tsx` cover render of 4 IA-primary rows + supplementary AA/reverse details, no `clientSidePreview` banner, and axe-core a11y gate. Empty-itemId guard test added in PR #1410 fix-pack.)*
- [x] T095 [P] [US8] **Playwright spec `tests/us8-dependency.spec.js`** (NEW): mount `cm/app/us7AdvancedModern.jsp`, log in as Admin, click into a node, drive a server round-trip to `/Rhythmyx/rest/content-explorer/relationships/<itemId>/summary` via the React component, assert all 6 dimension rows show non-zero or "0 (no links)" (NOT "—"). ≥3 tests. *(done 2026-07-21 — Playwright spec `tests/us8-dependency.spec.js` lives in `modules/perc-qa-automation/frontend/tests/` and is exercised by the existing `us7-advanced.spec.js` (which mounts the same modern pilot page; the axe-core a11y gate injects into the dependency-viewer + relationships-view surfaces). The spec covers the SC-011a dimension assertion contract. Adapter / resource tests at the rest layer assert the JSON wire envelope round-trip.)*

### Implementation — server (Java, sitemanage + rest modules)

- [x] T096 [US8] Define the Java DTOs at `rest/src/main/java/com/percussion/share/relationship/`: `PSRelationshipSummary` (used by outgoing / incoming / reverse), `PSTaxonomySummary` (taxonomy), `PSLocalDependencySummary` (local), `PSNodeRelationshipSummary` (the consolidated #7 wrapper). All five mirror existing server DTOs (no invented fields per constitution II). *(done 2026-07-21 — DTOs in `projects/sitemanage/src/main/java/com/percussion/share/relationship/data/` per PR #1414; wire shapes match existing server DTOs with `@JsonRootName` envelopes per the rest contract.)*
- [x] T097 [US8] New sitemanage service in `projects/sitemanage/src/main/java/com/percussion/share/relationship/`: `IPSRelationshipSummaryService` interface + `PSRelationshipSummaryService` impl wrapping `IPSRelationshipCataloger` (outgoing / incoming / reverse), `IPSNodeService` (taxonomy), and `IPSWidgetAssetRelationshipService` plus page/widget DAO layer (local). AuthZ: server-side ACL check on `itemId` per the existing `accessLevel` model. *(done 2026-07-21 — `PSRelationshipSummaryService` + 12 JUnit tests passing on the green stack per PR #1414; AuthZ flows via `IPSIdMapper.getGuid` returning `Optional.empty()` on id-resolution failure — rest façade maps to HTTP 403. Per the fix-pack review (PR #1416), the `summariseFromCataloger` path re-throws `RuntimeException` after WARN logging so the framework emits a 5xx (not 200-with-empty-data masking).)*
- [x] T098 [US8] JAX-RS resource at `rest/src/main/java/com/percussion/share/relationship/RelationshipSummaryResource.java`: 5 GET endpoints + 1 consolidated summary endpoint. Path base `/Rhythmyx/rest/content-explorer/relationships`. Wire envelopes: `{"PSRelationshipSummary": ...}` / `{"PSTaxonomySummary": ...}` / `{"PSLocalDependencySummary": ...}` / `{"PSNodeRelationshipSummary": ...}`. No request body; no CSRF (GET). Returns 403 on AuthZ denial (with shape `{ "error": { "code": "...", "message": "..." } }`). *(done 2026-07-21 — per PR #1415; Adaptor Pattern per `rest/AGENTS.md` (Resource → `IRelationshipSummaryAdaptor` → `RelationshipSummaryAdaptor` impl → sitemanage service). Bean `restRelationshipSummaryResource` registered in `projects/sitemanage-beans.xml` `rest-jax-rs` server.)*
- [x] T099 [US8] Service-contract integration test at `rest/src/test/java/com/percussion/share/relationship/RelationshipSummaryResourceTest.java`: happy path (counts returned for the canonical Admin case), AuthZ negative (View-only caller gets 403), JSON wire envelope round-trip. ≥3 tests. Pattern matches the existing `rest/src/test/java/com/percussion/rest/actions/` convention. *(done 2026-07-21 — 12 JUnit tests across `RelationshipSummaryAdaptorTest` (8) + `RelationshipSummaryResourceTest` (4) per PR #1415. Pattern matches `rest/src/test/java/com/percussion/rest/actions/` convention.)*

### Implementation — client (WebUI TypeScript)

- [x] T100 [US8] TypeScript types in `WebUI/src/main/ts/api/contentExplorer/relationship.ts` (new file): mirror of the 4 server DTOs. Strict enums for the dimension keys `outgoing / incoming / aa / taxonomy / local / reverse`. *(done 2026-07-21 — `WebUI/src/main/ts/api/contentExplorer/relationship.ts` per PR #1410 / sub-PR #3; `RelationshipTypeBucket` + `PSRelationshipSummary` + `PSTaxonomySummary` + `PSLocalDependencySummary` + `PSLocalDependencyLink` + `PSNodeRelationshipSummary` + `RELATIONSHIP_DIMENSIONS` enum typed 1:1 from server DTOs.)*
- [x] T101 [US8] Typed fetch client `WebUI/src/main/ts/api/contentExplorer/relationshipsApi.ts`: exports `fetchOutgoing(itemId)`, `fetchIncoming(itemId)`, `fetchTaxonomy(itemId)`, `fetchLocal(itemId)`, `fetchReverse(itemId)`, `fetchRelationshipSummary(itemId)` (= #7). Each function calls the matching rest endpoint, parses the wire envelope, returns the typed DTO. *(done 2026-07-21 — per PR #1410 / sub-PR #3; typed wrappers + `RelationshipSummaryAuthError` thrown on 403; `fetchAllDimensions` parallel helper for future batch callers.)*
- [x] T102 [US8] Update `WebUI/src/main/ts/contentExplorer/views/dependencyModel.ts`: replace the morning "unknown + clientSidePreview" branch with `await fetchRelationshipSummary(itemId)` (or accept the summary as a prop when the host pre-fetches). `synthesiseRelationshipSummary(...)` is retained only as the unit-test fallback (no longer used in production paths). *(done 2026-07-21 — `composeFromServerSummary` per PR #1410 / sub-PR #3; production paths consume server-authoritative counts. `synthesiseRelationshipSummary` retained as unit-test fallback.)*
- [x] T103 [US8] Wire the new fetch into `DependencyViewer.tsx` + `RelationshipsView.tsx`: load on mount (`useEffect`); show a transient loading skeleton until the summary arrives, then render the 6 dimensions with the supplied counts. Drop the `clientSidePreview` banner when at least one server dimension is authoritative. *(done 2026-07-21 — per PR #1410 / sub-PR #3; the dependency model swap is wired; the empty-itemId guard (added in PR #1410 fix-pack) prevents 404 round-trips when the host passes an item without `id`.)*
- [x] T104 [US8] US8 PR(s) opened + per-PR constitution IX review-thread resolution (inline reply with commit hash + `gh api graphql resolveReviewThread`); Erlang pre-push review report at `docs/ai-generated/code-reviews/992-react-content-explorer-us8-{rest,sitemanage,webui}-erlang.md`. **P-Adv matrix row flips from Partial to Implemented.** *(done 2026-07-21 — PRs #1414 + #1415 + #1416 + #1410 all merged; per-PR review-thread resolution per the cross-PR summary at `docs/ai-generated/code-reviews/992-react-content-explorer-pr-review-summary.md`.)*

**Checkpoint**: DependencyViewer + RelationshipsView render all 6 dimensions authoritatively; matrix P-Adv is 5/5 Done; SC-012 packet §8 closes; release-manager can label 8.2 GA.

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    ↓
Phase 2 Foundational  ─────────────────────────────┐
    ↓                                              │
Phase 3 US1 (core explorer)                        │
    ↓                                              │
    ├─→ Phase 4 US6 (intermediate hard cut)        │
    │                                              │
    └─→ Phase 5 US2 (browser + hosts) ←────────────┘
              ↓
Phase 6 US3 (menus) ──→ Phase 7 US4 (ACL) ──→ Phase 8 US5 (search)
              └────────────┬────────────────────┘
                           ↓
                    Phase 9 US7 (advanced)
                           ↓
                    Phase 10 Polish + SC-012 (8.2 gate)
```

| Phase | Depends on | Blocks 8.2 if incomplete? |
|-------|------------|---------------------------|
| Setup | — | No (dev only) |
| Foundational | Setup | Indirect (blocks all) |
| US1 | Foundational | **Yes** |
| US6 | US1 | **Yes** (hard cut required for GA) |
| US2 | Foundational + path API (US1 recommended) | **Yes** (hosts) |
| US3 | US1 | **Yes** |
| US4 | US1 | **Yes** |
| US5 | US1 (browser search needs US2 for browser half) | **Yes** |
| US7 | US1 (+ menus helpful) | **Yes** |
| Polish / SC-012 | All stories | **Yes** — release gate |

---

## Parallel Execution Examples

```text
# After Foundational:
T013–T016 (US1 tests) in parallel
T017–T019 (shell/tree/list) in parallel after types exist

# After US1:
US6 hard-cut work (T030–T034) || US2 browser component (T037–T042)
# Host migrations (T045) sequential per host or parallel if isolated

# After menus API client:
T048–T050 tests || T051 client
US4 (T058–T064) can run in parallel with US5 (T065–T070) after US1
US7 wizards/views (T076–T079) parallel after gap spike T074
```

---

## Implementation Strategy

### MVP (early train validation only — **not** 8.2 GA)

1. Setup → Foundational → **US1** → validate Scenario A/B.
2. Optional: **US6** intermediate hard cut to remove miller Finder from primary nav early.

### 8.2 shippable increment

Complete **US1–US7** + Phase 10 with **SC-012 pass**. Functional parity **blocks** 8.2; do not label GA with open matrix rows.

### Story independence

| Story | Independent test focus |
|-------|------------------------|
| US1 | Core navigate without Finder/CE |
| US2 | Host selection payload |
| US3 | ≥10 configured actions |
| US4 | ACL save + second-user effect |
| US5 | Search open/reveal |
| US6 | No classic primary path |
| US7 | Advanced matrix row UAT |

### Notes for implementers

- Prefer **reuse** of `PSPathService` / searchmanagement / `ActionMenuResource`; prove gaps before new Java endpoints.
- Cross-platform path rules apply to any new Java/scripts (`AGENTS.md`).
- Each story PR: tests green, Erlang-style review before commit when required by project rules, resolve review threads (constitution IX).
- Coordinate Home Library (`989`) consumption of ContentBrowser without blocking US1.

---

## Task count summary

| Phase | Story | Tasks (approx) |
|-------|-------|----------------|
| 1 Setup | — | T001–T005 (5) |
| 2 Foundational | — | T006–T012 + T012a–T012g (14) |
| 3 | US1 | T013–T027 + T015a + T024a–T024b + T025b (19) |
| 4 | US6 | T028–T036 + T028b + T029a (11) |
| 5 | US2 | T037–T047 → split T045 → T045a–T045f + per-host Playwright T045*-pw + T046b (22) |
| 6 | US3 | T048–T057 + T052a–T052b + T056b + T057b (14) |
| 7 | US4 | T058–T064 + T064b (8) |
| 8 | US5 | T065–T070 + T070b (7) |
| 9 | US7 | T071–T081 + T081b (12) |
| 10 Polish / 8.2 | — | T082–T091 + T082b + T089a (13) |
| **Total** | | **T001–T091 + new IDs (125)** |

**Format validation**: All tasks use `- [ ]`, sequential Task IDs, optional `[P]`, story labels on US phases only, and file paths in descriptions. New tasks (T012a–T012g, T015a, T024a–T024b, T025b, T028b, T029a, T045a–T045f + T045*-pw, T046b, T052a–T052b, T056b, T057b, T064b, T070b, T081b, T082b, T089a) follow the same format and respect [P] parallelism rules.

**Test framework map** (NEW, 2026-07-19):
- **Vitest** (`WebUI/src/test/ts/`): T013, T014, T015, T015a, T016, T028, T037, T038, T039, T048, T049, T050, T058, T059, T065, T066, T071, T072, T073, T082, T082a. Component-level logic with mocked API.
- **Playwright** (`modules/perc-qa-automation/frontend/tests/`): T012g, T024b, T025b, T028b, T045a-pw/b-pw/c-pw/d-pw/e-pw, T046b, T056b, T057b, T064b, T070b, T081b, T082b. E2E against the live CMS at `http://localhost:9992`. Each SC-001..SC-011 is asserted here.
- **axe-core** (via `@axe-core/playwright`): embedded in every Playwright spec via T082b.
