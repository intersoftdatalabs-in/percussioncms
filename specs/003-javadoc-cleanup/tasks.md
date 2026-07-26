# Tasks: Javadoc Cleanup for Content Explorer Module

**Input**: Design documents from `/specs/003-javadoc-cleanup/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/README.md, quickstart.md, baseline-raw.txt
**Feature Branch**: `003-javadoc-cleanup` | **Module**: `modules/DesktopContentExplorer/` (`com.percussion:perc-content-explorer`)

**Tests**: NOT generated for this feature. FR-009 of `spec.md` explicitly excludes new/modified tests ("the feature is documentation-only"), and Constitution III permits omitting behavioral tests for doc-comment changes. Verification is performed by the javadoc build itself plus the existing module test suite (must keep passing). The template's "Tests REQUIRED" rule is intentionally overridden by the spec for this docs-only change.

**Organization**: Tasks are grouped by user story so each story can be implemented and verified independently. All edits live ONLY in `modules/DesktopContentExplorer/src/main/java/` (comment/HTML/Javadoc-tag repairs). No pom, README, resources, test sources, or other module is touched (FR-004, FR-005, FR-008, FR-009).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story this task belongs to (US1, US2, US3)
- Exact mono-repo file paths are included in every task
- All builds/tests use `./mvn-env.sh` (JDK 21); never plain `mvn`

> **Fixing strategy (from research.md)** — applied in priority order on every touched file:
> 1. Write real Javadoc for missing comments on public classes/methods
> 2. Repair `{@link}` references (typos, dead links, wrong overload)
> 3. Fix `@param` name typos to match the actual signature
> 4. Repair HTML (`<code>` close, escape `<` `>` `&`)
> 5. Delete stale references pointing at deleted/renamed classes → prose mention
> 6. Last-resort `@SuppressWarnings("javadoc")` with a justification comment for private/generated/internal symbols ONLY

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm ownership, AGENTS governance, and the pre-cleanup javadoc baseline for the module

- [ ] T001 Identify owning module `modules/DesktopContentExplorer/` and apply the AGENTS hierarchy — run the Rule Discovery Protocol: confirm NO `AGENTS.md`/`AGENTS.local.md` exists under `modules/DesktopContentExplorer/`, so root `AGENTS.md` is the operative governance (per plan.md Technical Context)
- [ ] T002 Verify JDK 21 via `./mvn-env.sh` and reproduce the baseline: run `./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests 2> specs/003-javadoc-cleanup/baseline-raw.txt` and confirm it captures the documented `44 errors` / `100 warnings` tool summary (FR-006, SC-001)
- [ ] T003 Record the baseline metrics (errors/warnings counts, capture date, JDK 21, `maven-javadoc-plugin` 3.12.0) into `specs/003-javadoc-cleanup/checklists/requirements.md` as the referenced pre-cleanup artifact (FR-006, SC-001)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared prerequisites that MUST be complete before ANY user-story edits begin

- [ ] T004 Confirm the inherited javadoc plugin configuration in `pom.xml:2636-2653` (`maven-javadoc-plugin` 3.12.0, `doclint=all`, `failOnError=false`, `failOnWarnings=false`) and document that NO per-module override may be introduced (FR-005; contracts/README.md "Forbidden changes")
- [ ] T005 Build the per-file work list from `specs/003-javadoc-cleanup/baseline-raw.txt` (35 implicated `com/percussion/**` files) and record the issue-category → fix-mechanism playbook derived from `research.md` (categories: broken `{@link}`, no comment, no `@param`, no `@return`, no `@throws`, malformed HTML, unknown tag, param-name-not-found, exception-not-thrown, default-ctor)
- [ ] T006 Define the `@SuppressWarnings("javadoc")` justification convention: each suppression MUST carry a brief inline comment naming the symbol and reason, and be limited to private/generated/internal symbols or where the module already uses the pattern; every instance is logged for the PostCleanupReport (FR-003, research.md heuristics 5–6)

**Checkpoint**: Foundation ready — user-story implementation can now begin

---

## Phase 3: User Story 1 - CI Build Succeeds for Content Explorer Module (Priority: P1) 🎯 MVP

**Goal**: Eliminate all javadoc ERRORS and repair the structurally malformed Javadoc/HTML in the module so the javadoc step no longer fails or emits error-level noise attributable to `modules/DesktopContentExplorer`.

**Independent Test**: Run `./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests` and confirm the tool's final summary reads `0 errors` (FR-001, SC-002). Warning count drops substantially (cumulative ≥80% reduction confirmed after US2/US3).

### Implementation for User Story 1

> Each task repairs the structural javadoc issues (broken `{@link}` errors, malformed/bad HTML, unknown tags, `@param name not found`, `exception not thrown`, `use of default constructor`, `empty comment`) in its listed files. Do NOT change signatures/visibility/behavior (FR-004).

- [ ] T007 [P] [US1] Fix broken `{@link}` reference ERRORS in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerMenu.java`, `.../cx/PSContentExplorerStatusDialog.java`, `.../cx/PSExecutableSearch.java`, `.../cx/PSMainDisplayPanel.java` (resolve targets to correct FQN/overload, or replace stale links with prose per research.md heuristic 2/5)
- [ ] T008 [P] [US1] Fix broken `{@link}` reference ERRORS in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSMenuManager.java`, `.../cx/PSWizardDialog.java`, `.../wizard/PSWizardDialog.java`, `.../wizard/PSWizardPanel.java`
- [ ] T009 [US1] Re-run `./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests` and confirm summary reads `0 errors`; iterate on any remaining cross-module `{@link}` resolution until all 44 errors are cleared (FR-001, SC-002)
- [ ] T010 [P] [US1] Repair malformed/bad HTML (close `<code>`, escape `<` `>` `&`, fix nested-tag / invalid-HTML / semicolon-missing / empty-comment) in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSColumnWidthsOption.java`, `.../cx/PSContentExplorerApplet.java`, `.../cx/PSContentExplorerUtils.java`, `.../cx/PSDisplayFormatOption.java`, `.../cx/PSDisplayFormatTableModel.java`, `.../cx/PSExpandedOption.java`, `.../cx/PSImageIconLoader.java`, `.../cx/PSItemAssemblyManager.java`, `.../cx/PSOptionManager.java`, `.../cx/PSProcessMonitor.java`, `.../cx/PSSearchDialog.java`
- [ ] T011 [P] [US1] Fix unknown Javadoc/HTML tags, `@param name not found` typos (correct name to match signature), `exception not thrown` declarations (remove or correct `@throws`), and `use of default constructor` warnings in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java`, `.../cx/PSDisplayFormatOption.java`, `.../cx/PSExpandedOption.java`, `.../cx/PSContentExplorerApplet.java`, `.../cx/PSContentExplorerUtils.java`
- [ ] T012 [P] [US1] Repair malformed/bad HTML and broken `{@link}` in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSCESessionManager.java`, `.../cx/PSContentExplorerStatusDialog.java`, `.../cx/PSExecutableSearch.java`, `.../cx/PSMainDisplayPanel.java`, `.../cx/PSSearchViewActionManager.java`, `.../cx/PSSearchDialog.java`
- [ ] T013 [P] [US1] Repair malformed/bad HTML, unknown tags, and `@param` typos in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/javafx/BrowserProps.java`, `.../cx/javafx/PSBrowserUtils.java`, `.../cx/javafx/PSCallBack.java`, `.../cx/javafx/PSFileSaver.java`, `.../cx/guitools/UTMnemonicLabel.java`, `.../cx/catalogers/PSCommunityCataloger.java`, `.../cx/wizards/PSCopySiteNamePage.java`
- [ ] T014 [P] [US1] Repair malformed/bad HTML and broken `{@link}` in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/JSClipDataBridge.java`, `.../cx/JSClipEventBridge.java`, `.../cx/PSACLNewUserDialog.java`, `.../cx/PSAjaxSwingWrapperLocator.java`, `.../cx/PSFolderGeneralPanel.java`, `.../com/percussion/ServerConnection.java`, `.../wizard/PSWizardDialog.java`, `.../wizard/PSWizardPanel.java`
- [ ] T015 [US1] Re-run `./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests` and confirm `0 errors` and a reduced warning summary versus `baseline-raw.txt`; capture the partial count toward SC-001

**Checkpoint**: User Story 1 done when javadoc summary reads `0 errors`

---

## Phase 4: User Story 2 - Public/Internal API Surfaces Have Useful Javadoc (Priority: P2)

**Goal**: Write substantive Javadoc descriptions for the symbols the tool flags as missing documentation, so generated docs/IDE hover help are trustworthy instead of suppressed.

**Independent Test**: Regenerate javadoc and inspect that classes/methods previously emitting `no comment` / `no main description` / `no @param` / `no @return` / `no @throws` warnings now carry a meaningful description; those warnings no longer appear for the same symbols (FR-003, spec US2 acceptance scenarios).

### Implementation for User Story 2

> Root-cause fixes only (FR-003): write real descriptions; do NOT widen visibility or invent public Javadoc on package-private/internal symbols (spec Edge Cases).

- [ ] T016 [P] [US2] Add substantive class/method Javadoc (main description + `@param` + `@return` + `@throws` where applicable) to clear `no comment` / `no main description` warnings in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/catalogers/PSCommunityCataloger.java`, `.../cx/JSClipDataBridge.java`, `.../cx/JSClipEventBridge.java`, `.../cx/PSCESessionManager.java`
- [ ] T017 [P] [US2] Add substantive Javadoc (main description, `@param`, `@return`, `@throws` descriptions) to clear `no comment` / `no @param` / `no @return` / `no description for @throws` warnings in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSActionManager.java` (largest file; address per reported line range)
- [ ] T018 [P] [US2] Add `@param`, `@return`, and `@throws` descriptions to clear `no @param` / `no @return` / `no main description` warnings in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/javafx/BrowserProps.java`, `.../cx/javafx/PSCallBack.java`, `.../cx/javafx/PSFileSaver.java`, `.../cx/PSACLNewUserDialog.java`, `.../cx/PSFolderGeneralPanel.java`, `.../cx/PSOptionManager.java`, `.../cx/PSProcessMonitor.java`, `.../cx/wizards/PSCopySiteNamePage.java`
- [ ] T019 [P] [US2] Add missing `@throws` tags with descriptions (e.g., `PSContentExplorerException`, `PSCmsException`, `PSException`, `IOException`, `SAXException`, `ParserConfigurationException`, `MalformedURLException`) to clear `no @throws for ...` warnings in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerApplet.java`, `.../cx/PSSearchViewActionManager.java`, `.../cx/PSActionManager.java`
- [ ] T020 [P] [US2] Add substantive Javadoc to remaining `no comment` / `no @param` / `no @return` / `no main description` symbols in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/PSContentExplorerStatusDialog.java`, `.../cx/PSSearchDialog.java`, `.../cx/PSContentExplorerMenu.java`, `.../cx/PSMainDisplayPanel.java`, `.../cx/PSMenuManager.java`, `.../cx/PSWizardDialog.java`, `.../wizard/PSWizardPanel.java`, `.../com/percussion/ServerConnection.java`
- [ ] T021 [US2] Re-run `./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests`; confirm the `no comment` / `no @param` / `no @return` / `no @throws` warning classes are resolved for the symbols addressed above

**Checkpoint**: User Stories 1 AND 2 done — errors gone, descriptions present

---

## Phase 5: User Story 3 - Localized Builds & IDE Inspections Are Quiet (Priority: P3)

**Goal**: Finish the residual warning reduction, replace unresolvable cross-module `{@link}` references with prose, apply last-resort justified suppressions, and capture the post-cleanup verification artifacts so local/IDE javadoc runs are quiet.

**Independent Test**: On a clean checkout run a focused javadoc pass on `modules/DesktopContentExplorer` and confirm the warning count is reduced by ≥80% versus `baseline-raw.txt` (SC-001); `post-cleanup.txt` captured and `git diff` shows comment/whitespace-only changes (SC-004).

### Implementation for User Story 3

- [ ] T022 [P] [US3] For cross-module `{@link}` references pointing into `system/`, `rest/`, etc. that cannot be resolved, replace with a prose mention plus a `// TODO: re-link after consolidating docs in SPEC-NNN` comment (no invented APIs, research.md "scope discipline") in the affected `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/**` files
- [ ] T023 [P] [US3] Apply last-resort `@SuppressWarnings("javadoc")` WITH an inline justification comment (per T006 convention) only to private/generated/internal symbols still emitting warnings in `modules/DesktopContentExplorer/src/main/java/com/percussion/cx/**` and `.../wizard/**`
- [ ] T024 [US3] Re-run `./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests 2> specs/003-javadoc-cleanup/post-cleanup.txt` and confirm summary shows `0 errors` and warnings reduced ≥80% versus `baseline-raw.txt` (FR-002, SC-001, SC-002)
- [ ] T025 [US3] Update `specs/003-javadoc-cleanup/checklists/requirements.md` (and create `specs/003-javadoc-cleanup/post-cleanup.txt` delta) recording the post-cleanup counts and, for any remaining warnings, a justified reason per warning (FR-002, FR-007)
- [ ] T026 [US3] Run the full module build `./mvn-env.sh -pl modules/DesktopContentExplorer -am verify -DskipTests` and confirm it exits 0 with no javadoc-related failures (SC-003)

**Checkpoint**: All user stories done — warnings reduced ≥80%, artifacts captured

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification, signature-integrity check, and optional doc updates

- [ ] T027 [P] Validate SC-004: run `git diff --stat -- ':!*.md' modules/DesktopContentExplorer/` and confirm only comment/whitespace edits in tracked `.java` files — no signature, visibility, or behavior changes (FR-004)
- [ ] T028 [P] Run the existing module test suite `./mvn-env.sh -pl modules/DesktopContentExplorer test` and confirm all pre-existing tests still pass unchanged (FR-009; no test code modified)
- [ ] T029 Update `modules/DesktopContentExplorer/README.md` with javadoc conventions ONLY if the module already documents them; otherwise do NOT create a new doc file (FR-008, Constitution VIII / YAGNI)
- [ ] T030 [P] Spot-check that no `@SuppressWarnings("javadoc")` lacks its required justification comment (T006 convention) and that no suppression was applied to a public symbol (FR-003, FR-004)
- [ ] T031 Final confirmation: `./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests` prints `0 errors` and warnings reduced ≥80% vs `baseline-raw.txt`; record final delta in `specs/003-javadoc-cleanup/checklists/requirements.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational — establishes `0 errors` (MVP)
- **US2 (Phase 4)**: Depends on Foundational; builds on US1's structural fixes (add descriptions)
- **US3 (Phase 5)**: Depends on US1+US2 — residual warnings, suppressions, artifacts
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: After Foundational — no dependency on other stories; independently testable via `0 errors` summary
- **US2 (P2)**: After Foundational — adds descriptions to US1's now-structurally-clean files; independently testable via resolved `no comment`/`no @param` warnings
- **US3 (P3)**: After US1+US2 — final warning reduction + artifacts; independently testable via ≥80% reduction vs baseline

### Within Each User Story

- Edit only `modules/DesktopContentExplorer/src/main/java/**` comments/HTML/tags
- No signature/visibility/behavior change (FR-004); no new deps/plugins (FR-005)
- Run `./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests` after each cluster to keep the build green
- Commit after each task or logical group

### Parallel Opportunities

- All Setup tasks (T001–T003) can run in parallel
- All Foundational tasks (T004–T006) can run in parallel within Phase 2
- Within US1: T007/T008 (error files), T010/T011/T012/T013/T014 (disjoint file clusters) are `[P]` and can run in parallel
- Within US2: T016/T017/T018/T019/T020 address disjoint file sets and are `[P]`
- Within US3: T022/T023 are `[P]` (different files); T024–T026 are sequential verification
- Polish T027/T028/T030 are `[P]`; T029/T031 sequential
- Different user stories can be worked by different developers once Foundational is done (module ownership does not conflict)

---

## Parallel Example: User Story 1

```bash
# Independent file clusters in parallel (after T001–T006):
Task: "Fix broken {@link} errors in cx/PSContentExplorerMenu.java, PSContentExplorerStatusDialog.java, PSExecutableSearch.java, PSMainDisplayPanel.java"
Task: "Fix broken {@link} errors in cx/PSMenuManager.java, cx/PSWizardDialog.java, wizard/PSWizardDialog.java, wizard/PSWizardPanel.java"
Task: "Repair malformed HTML in cx/PSColumnWidthsOption.java, PSContentExplorerApplet.java, PSContentExplorerUtils.java, ..."
# After edits: single verification gate
Task: "Re-run ./mvn-env.sh -pl modules/DesktopContentExplorer javadoc:javadoc -DskipTests → confirm 0 errors"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T003)
2. Complete Phase 2: Foundational (T004–T006) — CRITICAL, blocks all stories
3. Complete Phase 3: User Story 1 (T007–T015) → `0 errors`
4. **STOP and VALIDATE**: `javadoc:javadoc` summary reads `0 errors`
5. This alone unblocks CI (SC-002)

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → `0 errors` (MVP, CI unblocked) → validate
3. US2 → meaningful descriptions added → validate
4. US3 → ≥80% warning reduction + artifacts captured → validate (SC-001)
5. Polish → signature check + tests pass → done

### Parallel Team Strategy

With multiple developers:
1. Team completes Setup + Foundational together
2. Once Foundational done:
- Developer A: US1 file clusters (T007–T015)
- Developer B: US2 description tasks (T016–T021)
- Developer C: US3 residual + artifacts (T022–T026)
3. Polish (T027–T031) runs after all stories merge

---

## Notes

- **No test tasks** — FR-009 (docs-only feature) overrides the template's "Tests REQUIRED" rule; verification is the javadoc build + existing module tests (T028).
- Every file path is under `modules/DesktopContentExplorer/src/main/java/` — no other module, the parent POM, or test sources are touched.
- `[P]` tasks = disjoint files, no dependencies; run them in parallel to speed the cleanup.
- Each `@SuppressWarnings("javadoc")` MUST carry a justification comment (T006/T023/T030).
- Stop at any checkpoint to validate the story independently before proceeding.

