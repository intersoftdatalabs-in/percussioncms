# Tasks: Cross-Platform Python Build Scripts

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/cli-schemas.md, quickstart.md

> **Strategy**: Per-directory PRs (FR-001a, Clarification Q1). Each PR removes only its in-scope `.sh`/`.bat` on landing. Foundation PR ships first; subsequent per-directory PRs gate on green CI from the foundation.

## Phase 1: Setup (Foundation PR — ships FIRST, blocks all per-directory PRs)

**Goal**: Stand up the pytest manifest, the cross-platform test runner, the path-filtered CI matrix workflow, and the US1 regression sentinel for `mvnw / mvnw.cmd`. This PR is the only PR that touches files under `scripts/` other than the per-script conversions in later phases.

**Independent Test**: A clean clone on Linux + Windows can `pip install -r scripts/requirements-dev.txt`, run `scripts/run-python-tests.{sh,cmd}`, see the US1 sentinel pass, and the new GH Actions workflow runs green on both runners (SC-003, SC-008).

- [X] T001 Read AGENTS hierarchy: root `/AGENTS.md` (cross-platform paths, scripts/ convention, Pre-PR Maven gate); `scripts/AGENTS.md` if present; module AGENTS for each scope touched later (R7 path-depth reference)
- [X] T002 Confirm Python 3.9+ on the working host (`python3 --version` on Linux/macOS, `python --version` on Windows); document in PR body
- [X] T003 Create `scripts/requirements-dev.txt` with pytest pinned (e.g. `pytest==8.3.*`) per FR-009a / R4; commit
- [X] T004 [P] Create `scripts/run-python-tests.sh` (Linux/macOS): `set -euo pipefail`; `python3 -m pip install -r scripts/requirements-dev.txt`; `python3 -m pytest scripts/ docker/scripts/ docker/entrypoint/ modules/perc-distribution-tree/scripts/ modules/ai-shared-develop/scripts/ modules/ai-shared-develop/src/main/resources/skills/ "$@"` (FR-009a)
- [X] T005 [P] Create `scripts/run-python-tests.cmd` (Windows): `@echo off`, install via `python -m pip install -r scripts\requirements-dev.txt`, run `python -m pytest` over the same set of in-scope dirs (FR-009a)
- [X] T006 Create `.github/workflows/python-build-scripts.yml` with `on: pull_request` + `push: branches: [development]`; `paths` filter union of in-scope script dirs + runner files + workflow file itself; matrix `ubuntu-latest` + `windows-latest`; `actions/setup-python@v5` with `python-version: '3.11'`, `cache: 'pip'`, `cache-dependency-path: scripts/requirements-dev.txt`; run the runner shim; NO `actions/setup-java` and NO Maven invocation (FR-012a, R5, Clarification Q4)
- [X] T007 [P] ~~Create `scripts/test_mvn_env_untouched.py`~~ (historical; that sentinel guarded the removed `mvn-env` wrappers). SC-004 now means: repo-root `mvnw` / `mvnw.cmd` remain present on `development`.
- [X] T008 Run `bash scripts/run-python-tests.sh` locally — expect exit 0 with the US1 sentinel passing and no other tests yet; push branch and observe `.github/workflows/python-build-scripts.yml` green on both runners (SC-003)
- [X] T009 Run Erlang review per root AGENTS pre-commit gate; open foundation PR; address feedback inline + resolve review threads per AGENTS.md IX; wait for human approval and merge before starting any per-directory phase

## Phase 2: User Story 2 - CI/release verify scripts run identically on every OS (Priority: P1) — Scope 1 `scripts/`

**Goal**: Convert every in-scope `.sh`/`.bat` under `scripts/` (excluding `release-audit/` which is its own sub-phase and `erlang-harvest-review-patterns.{sh,bat}` which is US3 in Phase 3) to Python with colocated pytest; remove the originals; update `scripts/README.md`.

**Independent Test (per `quickstart.md` Scenario B)**: Each converted script's `--help` exits 0; the verify scripts pass on the existing fixtures; `python3 -m pytest scripts/` exits 0 on Linux; `python -m pytest scripts\` exits 0 on Windows; `find scripts/ -type f \( -name '*.sh' -o -name '*.bat' \)` returns empty (SC-005, SC-002).

### Tests + Implementation (per script — colocated test runs in same PR)

- [X] T010 [P] [US2] Create `scripts/install-cms-dev.py` + `scripts/test_install_cms_dev.py` per `contracts/cli-schemas.md` Scope 1 entry (FR-002/FR-009); include `## Behavioral Notes` in module docstring (FR-009b, R2)
- [X] T011 [P] [US2] Create `scripts/authenticate-sigstore.py` + `scripts/test_authenticate_sigstore.py` per contracts
- [X] T012 [P] [US2] Create `scripts/gh-preflight.py` + `scripts/test_gh_preflight.py` per contracts
- [X] T013 [P] [US2] Create `scripts/hot-deploy-local.py` + `scripts/test_hot_deploy_local.py` per contracts
- [X] T014 [P] [US2] Create `scripts/resolve-conflicts.py` + `scripts/test_resolve_conflicts.py` per contracts
- [X] T015 [P] [US2] Create `scripts/fetch-gh-code-scanning-alerts.py` + `scripts/test_fetch_gh_code_scanning_alerts.py` per contracts (gated `@pytest.mark.network` for the live fetch path; offline unit test exercises the JSON parser with a fixture)
- [X] T016 [P] [US2] Create `scripts/generate-umbrella-issues.py` + `scripts/test_generate_umbrella_issues.py` per contracts
- [X] T017 [P] [US2] Create `scripts/stage-triage-cluster.py` + `scripts/test_stage_triage_cluster.py` per contracts
- [X] T018 [P] [US2] Create `scripts/filter-stale-alerts.py` + `scripts/test_filter_stale_alerts.py` per contracts
- [X] T019 [P] [US2] Create `scripts/verify-triage-inventory.py` + `scripts/test_verify_triage_inventory.py` per contracts; reuse `scripts/test-fixtures/triage-{good,bad}.md` (FR-010)
- [X] T020 [P] [US2] Create `scripts/verify-valid-fixes.py` + `scripts/test_verify_valid_fixes.py` per contracts
- [X] T021 [P] [US2] Create `scripts/verify-suppressions.py` + `scripts/test_verify_suppressions.py` per contracts
- [X] T022 [P] [US2] Create `scripts/verify-distribution-archive.py` + `scripts/test_verify_distribution_archive.py` per contracts
- [X] T023 [P] [US2] Create `scripts/verify-pr-review-resolution.py` + `scripts/test_verify_pr_review_resolution.py` per contracts (gated `@pytest.mark.network` for the live `gh` call)
- [X] T024 [P] [US2] Create `scripts/verify-no-finder-jsp-references.py` + `scripts/test_verify_no_finder_jsp_references.py` per contracts
- [X] T025 [P] [US2] Create `scripts/verify-no-jqplot-vendor-refs.py` + `scripts/test_verify_no_jqplot_vendor_refs.py` per contracts
- [X] T026 [P] [US2] Create `scripts/verify-codeql-analyzer-of-record.py` + `scripts/test_verify_codeql_analyzer_of_record.py` per contracts
- [X] T027 [P] [US2] Create `scripts/create-large-folder-fixture.py` + `scripts/test_create_large_folder_fixture.py` per contracts (gated `@pytest.mark.network` for the live CMS API path; offline test covers arg parsing and fixture dir creation)
- [X] T028 [P] [US2] Create `scripts/test-verify-triage-inventory.py` (Python port of the bash self-test) per contracts
- [X] T029 [US2] Convert `scripts/release-audit/release-audit.sh` + `scripts/release-audit/lib/{common,inventory,verdicts,backlog,report,port}.sh` + `scripts/release-audit/tests/test_*.sh` to a Python package (`scripts/release-audit/*.py` + `scripts/release-audit/tests/test_*.py`); bash `source lib/*.sh` becomes Python module imports (R8)
- [X] T030 [US2] Delete `scripts/*.sh` and `scripts/*.bat` for all scripts converted in T010–T028 (FR-004); leave `scripts/erlang-harvest-review-patterns.{sh,bat}` for Phase 3
- [X] T031 [US2] Update `scripts/README.md`: rewrite each catalog entry to reference the new `.py` (FR-011, FR-014); add the in-scope/out-of-scope section linking to the spec (FR-014); delete legacy "Windows users run the `.cmd` counterpart" notes (FR-011)
- [X] T032 [US2] Run `python3 -m pytest scripts/ -v` locally (SC-002) and `python -m pytest scripts\ -v` on Windows; both must exit 0
- [X] T033 [US2] Open PR titled `build(scripts): migrate verify/audit/dev scripts to cross-platform Python`; link to spec 994 / FR-013 in-scope enumeration
- [X] T034 [US2] Run Erlang review (root AGENTS pre-PR gate); address feedback inline + resolve review threads per AGENTS.md IX; verify CI green on both runners (SC-003); wait for human approval and merge

## Phase 3: User Story 3 - Erlang review pattern harvesting works cross-platform (Priority: P2) — Scope 1 `scripts/` subset

**Goal**: The `erlang-harvest-review-patterns.py` script already exists; the only remaining work is removing the now-redundant `.sh` and `.bat` wrappers and confirming pytest discovers the existing `test_erlang_harvest_review_patterns.py`.

**Independent Test (per `quickstart.md` Scenario B.3)**: `python3 scripts/erlang-harvest-review-patterns.py --help` exits 0; `python3 -m pytest scripts/test_erlang_harvest_review_patterns.py -v` exits 0; `find scripts/ -type f \( -name '*.sh' -o -name '*.bat' \)` returns empty (SC-002, SC-005).

- [X] T035 [US3] Verify `scripts/erlang-harvest-review-patterns.py` already exists (introduced 2026); if any new CLI args have been added since, update the contract in `contracts/cli-schemas.md` (FR-002)
- [X] T036 [P] [US3] Verify `scripts/test_erlang_harvest_review_patterns.py` runs via `python3 -m pytest` (it currently runs via raw `python3`; the pytest adaptation is documented in `scripts/README.md` and reuses the existing `--fixture` flag — adjust only the runner)
- [X] T037 [US3] Delete `scripts/erlang-harvest-review-patterns.sh` and `scripts/erlang-harvest-review-patterns.bat` (FR-004)
- [X] T038 [US3] Update `scripts/README.md` `erlang-harvest-review-patterns` entry to drop the `.sh`/`.bat` references and the "Cross-platform: Python core; Unix wrapper `.sh` and Windows wrapper `.bat`" line (FR-011)
- [X] T039 [US3] Run `bash scripts/run-python-tests.sh` locally — expect exit 0 with erlang-harvest tests included
- [X] T040 [US3] Open PR titled `build(scripts): remove erlang-harvest .sh/.bat wrappers (Python is the entry point)`; reference US3 in PR body
- [X] T041 [US3] Run Erlang review; resolve review threads; verify CI green on both runners; wait for human approval and merge

## Phase 4: User Story 4 - AI skill scripts and docker dev tooling work cross-platform (Priority: P2) — Scopes 2 (`docker/`) and 4 root (`modules/ai-shared-develop/scripts/`)

**Goal**: Convert docker dev tooling and AI dev tooling (non-skill) to Python; remove `.sh` originals; update `docker/README.md` and `modules/ai-shared-develop/AGENTS.md`.

**Independent Test (per `quickstart.md` Scenarios C + E.1)**: `python3 docker/scripts/perc-devctl.py --help` lists all subcommands; `python3 -m pytest docker/scripts/ modules/ai-shared-develop/scripts/` exits 0 on Linux + Windows; `find docker/scripts/ docker/entrypoint/ modules/ai-shared-develop/scripts/ -type f -name '*.sh'` returns empty (SC-002).

- [X] T042 [P] [US4] Create `docker/scripts/hot-deploy-jar.py` + `docker/scripts/test_hot_deploy_jar.py` per contracts
- [X] T043 [P] [US4] Create `docker/scripts/perc-devctl.py` + `docker/scripts/test_perc_devctl.py` per contracts; preserve all bash subcommands (`install`, `up`, `down`, `status`, `verify`, `it-verify`, `deploy-jar`, `verify-fix`, `logs-path`, `inspect-install`, `show-generated-passwords`); document the bash trap → `try`/`finally` deviation in `## Behavioral Notes` (FR-009b, R2)
- [X] T044 [P] [US4] Create `docker/entrypoint/install-update.py` + `docker/entrypoint/test_install_update.py` per contracts
- [X] T045 [P] [US4] Create `modules/ai-shared-develop/scripts/sign-ai-resources.py` + `modules/ai-shared-develop/scripts/test_sign_ai_resources.py` per contracts
- [X] T046 [P] [US4] Create `modules/ai-shared-develop/scripts/verify-signatures-hook.py` + `modules/ai-shared-develop/scripts/test_verify_signatures_hook.py` per contracts
- [X] T047 [P] [US4] Create `modules/ai-shared-develop/scripts/build-integrity-check.py` + `modules/ai-shared-develop/scripts/test_build_integrity_check.py` per contracts
- [X] T048 [US4] Delete `docker/scripts/*.sh`, `docker/entrypoint/*.sh`, `modules/ai-shared-develop/scripts/*.sh` (FR-004)
- [X] T049 [P] [US4] Update `docker/README.md` to reference the new `.py` entry points and drop `.sh` references (FR-011)
- [X] T050 [P] [US4] Update `modules/ai-shared-develop/AGENTS.md` to reference the new `.py` entry points (FR-011)
- [X] T051 [US4] Run `python3 -m pytest docker/scripts/ modules/ai-shared-develop/scripts/ -v` locally; run on Windows too
- [X] T052 [US4] Open PR titled `build(docker,ai-shared): migrate docker + AI dev scripts to cross-platform Python`
- [X] T053 [US4] Run Erlang review; resolve review threads; verify CI green on both runners; wait for human approval and merge

## Phase 5: User Story 4 (cont.) - AI skill helper scripts (Priority: P2) — Scope 4 nested (`modules/ai-shared-develop/src/main/resources/skills/*/scripts/`)

**Goal**: Convert the per-skill `.sh` helpers under the AI skill bundle to Python so any agent invoking a skill helper works cross-platform.

**Independent Test (per `quickstart.md` Scenario E.2 + E.3)**: Every skill helper's `--help` exits 0; `python3 -m pytest modules/ai-shared-develop/src/main/resources/skills/ -v` exits 0 on Linux + Windows; the parent module's `mvnw clean install` of `modules/ai-shared-develop` stays green (SC-007).

- [X] T054 [P] [US4] Create `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/api-client.py` + `test_api_client.py` per contracts
- [X] T055 [P] [US4] Create `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/download-latest.py` + `test_download_latest.py` per contracts
- [X] T056 [P] [US4] Create `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/install-cms.py` + `test_install_cms.py` per contracts
- [X] T057 [P] [US4] Create `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/install-dts.py` + `test_install_dts.py` per contracts
- [X] T058 [P] [US4] Create `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/start-cms.py` + `test_start_cms.py` per contracts
- [X] T059 [P] [US4] Create `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/start-dts.py` + `test_start_dts.py` per contracts
- [X] T060 [P] [US4] Create `modules/ai-shared-develop/src/main/resources/skills/javadoc/scripts/generate-javadoc-stubs.py` + `test_generate_javadoc_stubs.py` per contracts
- [X] T061 [US4] Update each affected skill's `SKILL.md` to reference the new `.py` entry points and drop `.sh` references (FR-011)
- [X] T062 [US4] Delete the `.sh` files under `modules/ai-shared-develop/src/main/resources/skills/*/scripts/` (FR-004)
- [X] T063 [US4] Run `python3 -m pytest modules/ai-shared-develop/src/main/resources/skills/ -v` locally; run on Windows too
- [X] T064 [US4] Verify `cd modules/ai-shared-develop && ../../mvnw clean install` succeeds with no new warnings (SC-007)
- [X] T065 [US4] Open PR titled `build(ai-shared/skills): migrate skill helper scripts to cross-platform Python`
- [X] T066 [US4] Run Erlang review; resolve review threads; verify CI green on both runners; wait for human approval and merge

## Phase 6: `modules/perc-distribution-tree/` build verification + APIUpdate helpers (Scope 3)

**Goal**: Convert the build verification helpers and the developer-convenience `APIUpdate-*.bat` + `UpdateTinyMCE.bat` to Python; remove originals; update `modules/perc-distribution-tree/AGENTS.md` and any module-level script README.

**Independent Test**: `python3 modules/perc-distribution-tree/scripts/verify-jdbc-drivers.py --help` exits 0; `python3 modules/perc-distribution-tree/scripts/api-update.py --help` exits 0 and lists `--module {webui,rest,sitemanage,jars}`; `python3 -m pytest modules/perc-distribution-tree/scripts/ -v` exits 0; `cd modules/perc-distribution-tree && ../../mvnw clean install` succeeds with no new warnings (SC-007).

- [X] T067 [P] Create `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.py` + `test_verify_jdbc_drivers.py` per contracts
- [X] T068 [P] Create `modules/perc-distribution-tree/scripts/check-no-glob-deletes.py` + `test_check_no_glob_deletes.py` per contracts
- [X] T069 Create `modules/perc-distribution-tree/scripts/api-update.py` (consolidated helper for `--module {webui,rest,sitemanage,jars}`) + `test_api_update.py` per contracts; document the Windows `start /WAIT cmd /C ...` → `subprocess.run([...], shell=False)` deviation in `## Behavioral Notes` (FR-009b, R2); the live Maven invocation is gated behind a `--dry-run` flag for tests
- [X] T070 [P] Create `modules/perc-distribution-tree/scripts/update-tinymce.py` + `test_update_tinymce.py` per contracts
- [X] T071 Delete `modules/perc-distribution-tree/scripts/*.sh` and `modules/perc-distribution-tree/scripts/*.bat` (FR-004)
- [X] T072 Delete `modules/perc-distribution-tree/APIUpdate-WEBUI.bat`, `APIUpdate-REST.bat`, `APIUpdate-SiteManage.bat`, `APIUpdateJars.bat`, `UpdateTinyMCE.bat` (FR-004)
- [X] T073 [P] Update `modules/perc-distribution-tree/AGENTS.md` and `modules/perc-distribution-tree/scripts/README.md` (if exists; create if absent) to reference the new `.py` entry points (FR-011)
- [X] T074 Run `python3 -m pytest modules/perc-distribution-tree/scripts/ -v` locally and on Windows
- [X] T075 Verify `cd modules/perc-distribution-tree && ../../mvnw clean install` succeeds (SC-007); run Erlang review on the diff
- [X] T076 Open PR titled `build(perc-distribution-tree): migrate build verification + APIUpdate helpers to cross-platform Python`
- [X] T077 Resolve review threads; verify CI green on both runners; wait for human approval and merge

## Phase 7: Polish & Cross-Cutting Concerns

**Goal**: Make the historical-scripts decision (R3), update cross-cutting documentation, run the final SC verification (Scenario F from `quickstart.md`), and close the spec.

- [X] T078 [P] Resolve the R3 decision for `docs/ai-generated/tasks/#000-webui-src-layout/*.sh`: confirm with maintainer whether to delete (preferred) or carve-out from spec scope; apply the chosen outcome (likely delete; record the decision in PR body)
- [X] T079 [P] Update `scripts/README.md` with the in-scope/out-of-scope section linking to spec 994 (FR-014) — already partially done in T031; verify completeness after all phases have landed
- [X] T080 [P] Update root `AGENTS.md` only if any reference to in-scope scripts slipped through during the per-directory PRs (do NOT touch the `mvnw / mvnw.cmd` lines per Clarification Q2)
- [X] T081 Run the full Scenario F end-to-end verification from `quickstart.md` on Linux: SC-001 zero survivors, SC-002 pytest green, SC-003 CI green, SC-004 Maven wrapper unchanged, SC-005 verify parity, SC-006 zero doc refs, SC-007 Maven regression-free, SC-008 requirements-dev.txt + runner idempotent
- [X] T082 [P] Open a final docs/cleanup PR titled `docs(994): close spec — all per-directory PRs merged; final SC-001..SC-008 verification recorded`; include the Scenario F output in the PR body
- [X] T083 Resolve review threads; verify CI green on both runners; merge; mark spec 994 complete in `specs/994-python-build-scripts/tasks.md` (all boxes ticked)
- [X] T084 (Post-merge) Run `python3 scripts/save_kilo_memory.py` (or invoke `kilo_memory_save` action=remember) with key `spec_994_python_build_scripts_status` capturing: total PRs merged, total Python scripts landed, total `.sh`/`.bat` removed, pytest count, CI matrix green — for continuity into future sessions

## Dependencies & Execution Order

- **Phase 1 (Setup / Foundation PR)**: No dependencies. **Must merge first.** Blocks Phases 2-7 (provides the CI gate, pytest manifest, runner, and US1 sentinel).
- **Phase 2 [US2] scripts/**: Depends on Phase 1. Independent of Phases 3-7 in principle, but recommended sequential to keep PR review queue manageable.
- **Phase 3 [US3] erlang-harvest wrappers**: Depends on Phase 1. **Touches the same directory as Phase 2** — must wait for Phase 2 to merge so the per-PR diff for Phase 3 is purely the wrapper removal (avoids touching `scripts/README.md` twice).
- **Phase 4 [US4] docker + ai-shared/scripts/**: Depends on Phase 1. Independent of Phases 2/3/5/6 in principle.
- **Phase 5 [US4 cont.] skill helpers**: Depends on Phase 1. **Touches `modules/ai-shared-develop/`** (same module as Phase 4) — recommend sequential after Phase 4 so the per-PR diff for Phase 5 is purely skill helpers.
- **Phase 6 modules/perc-distribution-tree/**: Depends on Phase 1. Independent of Phases 2/3/4/5/7.
- **Phase 7 (Polish)**: Depends on Phases 2, 3, 4, 5, 6 all merged. Final verification PR.

### Recommended merge order (minimizes per-PR conflicts)

1. Phase 1 (foundation)
2. Phase 2 (scripts/, US2 — biggest)
3. Phase 3 (erlang-harvest wrapper removal, US3 — small follow-up to Phase 2)
4. Phase 6 (perc-distribution-tree/ — independent module)
5. Phase 4 (docker + ai-shared/scripts/ — independent dirs)
6. Phase 5 (ai-shared/skills/ — follow-up to Phase 4 in same module)
7. Phase 7 (polish + final SC verification)

Phases 2, 4, 6 are mutually independent at the git level and could be parallelized across review streams; the sequential order above is a recommendation, not a hard constraint.

## Parallel Execution Examples

```bash
# Foundation PR (Phase 1): T003, T004, T005, T007 are parallelizable across files
# T003 + T004 + T005 + T006 + T007 + T008 + T009 — all in the foundation PR

# Within Phase 2: each script + its test is independent — fan out across reviewers
# T010..T028 can be drafted concurrently by different agents; pytest collects them all

# Within Phase 4: docker scripts and ai-shared scripts are in different dirs
# T042..T047 can land in the same PR (single PR per phase); individual file edits are parallel-safe
```

## Implementation Strategy

- **MVP First (foundation)**: Phase 1 alone proves the test harness and CI gate work end-to-end, including the US1 regression sentinel for `mvnw / mvnw.cmd` (SC-004). Merging Phase 1 first means every subsequent PR is automatically gated by both Linux + Windows pytest runs and by the US1 no-touch check — no chance of accidentally regressing the existing Maven wrapper.
- **Incremental Delivery**: After Phase 1 lands, each per-directory PR adds a meaningful slice (US2 → US3 → US4 → build helpers) and is independently testable via its corresponding quickstart scenario.
- **Independent Story Tests**:
  - US1 → historical `test_mvn_env_untouched.py` removed with `mvn-env`; SC-004 = `mvnw`/`mvnw.cmd` presence
  - US2 → quickstart Scenario B (every Phase 2 pytest case + verify parity diff)
  - US3 → quickstart Scenario B.3 (`python3 -m pytest scripts/test_erlang_harvest_review_patterns.py`)
  - US4 → quickstart Scenarios C + E (`python3 -m pytest docker/scripts/ modules/ai-shared-develop/`)
  - Final → quickstart Scenario F (SC-001..SC-008 sweep)
- **No Maven in this work**: per Clarification Q4, neither pytest nor the new GH Actions workflow invokes Maven. The SC-007 Maven regression check in quickstart Scenario F is a manual local run, not part of CI (SC-007 is a pre-PR developer gate, not an automated gate).

