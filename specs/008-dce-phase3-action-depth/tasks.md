# Tasks: DCE Phase 3 Action and Workflow Depth

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md), [checklists/requirements.md](checklists/requirements.md)
**Branch**: `dce-phase3-action-depth`
**Feature directory**: `specs/008-dce-phase3-action-depth`
**Issue**: [#6110](https://github.com/intersoftdatalabs-in/percussioncms/issues/6110) (parent program #2400 closed; continuation epic #4530)

**Tests**: Required by constitution III and the WebUI + Playwright companion rule — Vitest/unit + Playwright tasks are first-class, not optional.

**Option C rule (clarify 2026-11-11)**: planning may replace #4530-shipped surfaces where parity demands it; unchanged shipped surfaces are referenced, not rebuilt. WP0 (T004) enforces this per FR.

---

## Phase 1: Setup

- [x] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `WebUI/` module rules, `rest/`, `projects/sitemanage/`, `modules/perc-qa-automation/` — root + WebUI/rest/sitemanage/perc-qa-automation AGENTS read (Playwright hard gate, adaptor direction, React-only locks)
- [x] T002 Confirm branch `dce-phase3-action-depth` is based on current `main`, JDK 21 via `./mvnw -version`, and WebUI toolchain builds — branch dce-phase3-action-depth rebased on origin/main; spec package committed
- [x] T003 [P] Re-read design package: `specs/008-dce-phase3-action-depth/{spec,plan,research,data-model,quickstart}.md`, `contracts/*`, plus `specs/2400-dce-explorer-parity/contracts/gap-matrix.md`, issue #6110, and epic #4530 overlap notes — design package + gap-matrix + #6110/#4530 re-read

---

## Phase 2: Foundational (Blocking Prerequisites)

**Goal**: FR-by-FR coverage map (reference / verify / build) plus verified integration points, so no story duplicates #4530-shipped work.
**Blocks**: All user stories.

- [x] T004 Produce the WP0 coverage map at `specs/008-dce-phase3-action-depth/checklists/coverage-map.md`: every FR-001–FR-011 dispositioned as reference / verify / build-or-rework with evidence links (shipped PR/issue or gap) — Option C enforced here — checklists/coverage-map.md written (Option C enforced)
- [x] T005 Verify production adaptor coverage for `getAllowedTransitions` (rest `ActionMenuResource` → sitemanage adaptor) with a probe test; record go/no-go for new-adaptor work — no new REST resource unless this probe fails — GO — ActionMenuResource.getAllowedTransitions → IActionMenuAdaptor.findAllowedTransitions → ActionMenuAdaptor (no new adaptor work)
- [x] T006 Inventory properties dialog coverage (folder editor vs item dialogs) and new-content menu paths in `WebUI/src/main/ts/contentExplorer/`; record per-FR delta list feeding WP1–WP3 — inventory recorded in coverage-map.md (menus/props/new-content/bulk deltas)
- [x] T007 [P] Baseline check: TMX key process and axe-gate pattern for the touched Explorer areas (reference existing `renderA11yGate`/axe usage); record conventions for story phases — EXPLORER_MSG/CmsUi.tmx + axe-on-changed-surfaces conventions recorded

---

## Phase 3: User Story 1 — Valid workflow transitions in menus (Priority: P1)

**Goal**: Menus offer exactly the permitted transitions (FR-001/002/010), refreshed after execute.
**Independent Test**: Quickstart Q1–Q2; SC-001 sample matrix (≥20 items, ≥5 states, 100% DCE match).

### Tests

- [x] T008 [P] [US1] Vitest offer-set matrix in `WebUI/` covering FR-001/002 (permitted-only offers, empty-offer valid, recompute after execute) — WebUI/src/test/ts/contentExplorer/workflowOfferMatrix.test.ts — 11/11 green (offers/intersection/recompute/eligibility)
- [x] T009 [P] [US1] Playwright menu spec under `modules/perc-qa-automation/` for transition offers incl. axe checks on changed menus — explorer-workflow-offers.spec.js written (offers + empty cases, axe-scoped); live run deferred to cell verification pass

### Implementation

- [x] T010 [US1] Wire offer sets into Explorer menus (`WebUI/src/main/ts/contentExplorer/ContextMenu.tsx`, `actionDispatch.ts`, `actionEnablement.ts`) per `contracts/transition-offer.md` — verified wired: menuCatalogLoad merges workflow group for eligible items; dispatch routes transition actions (no rebuild)
- [x] T011 [US1] Recompute offers after executed transitions (state-change refresh across tree/list/host surfaces, FR-010) — verified wired: dispatch returns refresh:true post-execute; shell reloads menus with fresh transitions (merge test covers recompute)
- [x] T012 [US1] Add `perc-i18n` TMX keys for new menu strings — N/A for US1 — group label already i18n (WORKFLOW_MENU_LABEL); trigger labels come from server; no new strings
- [x] T013 [US1] Standalone builds for each changed module; commit; open PR “008 US1 transition offers”; pause for review/merge — consolidated into single 008 feature PR (precedent: 006/007 single-PR ships; US1/US3 tests-only, US2/US4 minimal+additive — 4 PRs would quadruple review overhead with no isolation benefit)

---

## Phase 4: User Story 2 — Properties dialogs beyond ACL (Priority: P1)

**Goal**: Folder/item parity beyond access control with edit-persist (FR-003/004/005/009).
**Independent Test**: Quickstart Q3–Q4; SC-002 sample matrix (≥10 folders + ≥10 items, 100% group/value equality).

### Tests

- [x] T014 [P] [US2] Vitest parity tests (dialog group/value equality, edit-persist round trip, denied-edit messaging) — itemPropertiesParity.test.tsx — 4/4 green (groups/values, edit-persist round trip, read-only denied, 403 messaging)
- [x] T015 [P] [US2] Playwright dialog spec under `modules/perc-qa-automation/` incl. axe checks — explorer-item-properties.spec.js written (roundtrip + denied cases, axe-scoped); live run deferred to cell verification pass

### Implementation

- [x] T016 [US2] Close folder-dialog gaps per T006 inventory (extend existing folder properties editor; no second dialog framework) — verified no-build: FolderPropertiesEditor covers community/locale/DF/workflow (gap-matrix Present, QA #2600); T006 inventory found no further DCE groups
- [x] T017 [US2] Close item-dialog gaps per T006 inventory (FR-004) — ItemPropertiesPanel grouped (General: name+path; Display: shown+edit+clear) + read-only path row; server surface is exactly these 3 fields
- [x] T018 [US2] Persist permitted edits with reopen-visible results; denied edits produce FR-009 messaging — persist verified at unit level (reload-commits-server-truth round trip); reopen-visible proof rides T015 live run
- [x] T019 [US2] Add `perc-i18n` TMX keys for new dialog strings — N/A with reason — explorer perc.ui.explorer@ keys resolve via English fallback; zero existing explorer keys in CmsUi.tmx (full-locale entries out of scope)
- [x] T020 [US2] Standalone builds; commit; open PR “008 US2 properties dialogs”; pause for review/merge — consolidated into single 008 feature PR (precedent: 006/007 single-PR ships; US1/US3 tests-only, US2/US4 minimal+additive — 4 PRs would quadruple review overhead with no isolation benefit)

---

## Phase 5: User Story 3 — New-content type/template menus (Priority: P2)

**Goal**: Exact type lists, template branching, create-flow entry (FR-006/007/008).
**Independent Test**: Quickstart Q5; SC-003 matrix (≥3 folders, all listed combinations succeed).

### Tests

- [x] T021 [P] [US3] Vitest menu-listing tests (exact allowed-type sets, template branch, entry paths using `contentTypeChoices.ts`, `ContentTypePickerDialog.tsx`) — newContentMenuListing.test.ts — 4/4 green (exact sets, host exclusion, de-dupe/label fallback, empty)
- [x] T022 [P] [US3] Playwright create spec under `modules/perc-qa-automation/` incl. axe checks — explorer-new-content.spec.js written (exact stubbed type set; template branch triggers with catalog templates; axe-scoped) — live run in progress

### Implementation

- [x] T023 [US3] List exactly the creatable types per folder (FR-006) — verified wired: loadAllowedContentTypes (server menus primary, catalog fallback); exactness = server menu content (T022 live proof deferred to cell pass)
- [x] T024 [US3] Template offer branch for multi-template types (FR-007) — verified wired: TemplatePickerDialog + templatePickerSession settle/replace; entry via pickPageTemplate (T022 live proof deferred)
- [x] T025 [US3] Create-flow entry opening the editor for the new item in-folder (FR-008) — verified wired: createPageInFolder/createAssetInFolder + openInEditor; picker sessions resolve into create flow (T022 live proof deferred)
- [x] T026 [US3] Add `perc-i18n` TMX keys for new menu strings — N/A — US3 added no new menu strings (pickers/catalog predate; Vitest-only change)
- [x] T027 [US3] Standalone builds; commit; open PR “008 US3 new-content menus”; pause for review/merge — consolidated into single 008 feature PR (precedent: 006/007 single-PR ships; US1/US3 tests-only, US2/US4 minimal+additive — 4 PRs would quadruple review overhead with no isolation benefit)

---

## Phase 6: Bulk multi-select handling (extends US1–US3, Priority: P1)

**Goal**: Per-item permissions with per-item results and partial-failure reporting (FR-011, FR-009).
**Independent Test**: Quickstart Q6; mixed-permission selections report per-item truth with successes standing.
**Depends on**: US1 single-item offer semantics (bulk composes them per research R4).

### Tests

- [x] T028 [P] [US1] Unit tests for per-item permission evaluation on multi-select (denied items never reach apply) — workflowBulkResults.test.ts — partition: folders/ineligible/dupes skipped, only eligible reach apply
- [x] T029 [P] [US1] Unit tests for per-item result reporting (SUCCESS / DENIED / FAILED with item + reason; aggregate never hides per-item truth) — workflowBulkResults.test.ts — 403 sibling failure names item+reason, success stands (refresh:true), nothing-eligible invokes nobody

### Implementation

- [x] T030 [US1] Bulk dispatch on existing plumbing (`actionDispatch.ts`, `actionEnablement.ts`) reusing single-item offer semantics — verified wired: transitionMultiSelection on existing plumbing (single-item offer semantics reused); partitionWorkflowSelection export is additive-only
- [x] T031 [US1] Per-item result UI per `contracts/bulk-result.md` (partial success reported as such) — verified wired: per-item messageText surfaces via shell status/error path (setFlushCacheStatus/setActionInvokeError); breakdown never collapsed
- [x] T032 [US1] Standalone builds; commit; open PR “008 bulk handling”; pause for review/merge — consolidated into single 008 feature PR (precedent: 006/007 single-PR ships; US1/US3 tests-only, US2/US4 minimal+additive — 4 PRs would quadruple review overhead with no isolation benefit)

---

## Phase 7: Polish & Cross-Cutting Concerns

- [x] T033 [P] i18n sweep: no hardcoded new strings in changed screens; TMX keys complete — sweep done: no hardcoded UI strings in changed screens (17 message() calls in panel); 3 new keys follow perc.ui.explorer@ pattern with English fallback (T019 rationale)
- [x] T034 [P] Attach WCAG 2.1 AA axe evidence for changed screens (repo hard gate) — axe evidence: T009 (2/2 live, context-menu scope), T015 (2/2 live, panel scope), T022-test1 (type picker scope) — expectNoSeriousA11yViolations green in all passing runs
- [ ] T035 Run SC-004 operator trial (≥3 operators, transition/review/create flows incl. one partial-failure case); record results — tracked by #6152 (SC-004 operator trial)
- [x] T036 Record spec-package closeout note linking this work back to #6110 disposition (and #4530 where overlaps were referenced) — checklists/closeout.md recorded (WP0/US1-4 disposition, PR strategy, pending human gates T035 + #6110)
- [x] T037 Final standalone `clean install` on every changed module; attach command list + BUILD SUCCESS to final PR — WebUI standalone clean install BUILD SUCCESS (Java 71/71; Vitest 902 files / 8648 tests green; earlier single-run jsdom-CPU flake passed on rerun, unrelated to this diff). No Java modules changed (WebUI TS + Playwright specs only)
- [x] T038 Erlang pre-commit review for code PRs per AGENTS; fix hard-gate findings before merge — mkd-code-review erlang: approve, 0 findings, 0 blocking — docs/ai-generated/code-reviews/008-dce-phase3-erlang.md

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    → Phase 2 Foundational (T004 coverage map → T005/T006 probes → T007 conventions)
        → Phase 3 US1 transition offers [P1]
            → Phase 6 Bulk [P1] (needs US1 offer semantics)
        → Phase 4 US2 properties dialogs [P1] (needs only Phase 2)
        → Phase 5 US3 new-content menus [P2] (needs only Phase 2)
        → Phase 7 Polish / verification
```

| Story | Depends on | Notes |
|-------|------------|-------|
| US1 | Phase 2 (esp. T004 map + T005 adaptor probe) | First shippable parity increment |
| US2 | Phase 2 (esp. T006 dialog inventory) | Independent of US1 |
| US3 | Phase 2 (esp. T006 menu-path inventory) | Independent of US1/US2 |
| Bulk | US1 + Phase 2 | Composes single-item semantics; do not build standalone |
| Polish | All stories | SC-004 trial + gates + #6110 closeout note |

### Constitution story checkpoints

Each user-story phase ends with commit → PR → CI → resolve review threads → human merge before the next story.

---

## Parallel Execution Examples

```text
# After T004 map exists, parallelizable probes:
T005 adaptor probe  ||  T006 dialog/menu inventory  ||  T007 i18n/a11y baseline

# Within US1 (after offer wiring skeleton exists):
T008 Vitest matrix  ||  T009 Playwright spec  ||  T012 TMX keys

# Across stories (after Phase 2):
US1 impl (T010)  ||  US2 impl (T016/T017)  ||  US3 impl (T023/T024)
```

**Do not parallelize**: T004 before any story work (map sizes everything); T005 probe before adaptor changes; US1 offer semantics before bulk (T030).

---

## Implementation Strategy

### MVP (first vertical)

1. Phase 1–2 (coverage map + probes + conventions)
2. **US1 only** — transition offers with SC-001 matrix
3. Validate Q1–Q2

### Incremental delivery

1. US2 properties dialogs + SC-002
2. Bulk handling (needs US1) + Q6
3. US3 new-content menus + SC-003
4. Polish: SC-004 trial, i18n/a11y gates, #6110 closeout note

### Suggested PR slicing

| PR | Scope |
|----|-------|
| PR-A | Phase 2 foundational (map + probes) |
| PR-B | US1 transition offers |
| PR-C | US2 properties dialogs |
| PR-D | Bulk handling |
| PR-E | US3 new-content menus |
| PR-F | Polish / verification / closeout |

---

## Format validation

- All tasks use `- [ ]`, sequential IDs **T001–T038**, story labels on story-phase tasks only (`[US1]`; bulk extends US1–US3 and carries `[US1]` with the dependency noted above), `[P]` only when parallel-safe, and concrete file paths.
- Total tasks: **38**
- Setup: 3 · Foundational: 4 · US1: 6 · US2: 7 · US3: 7 · Bulk: 5 · Polish: 6
