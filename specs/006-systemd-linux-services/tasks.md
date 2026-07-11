# Tasks: Systemd Linux Service Scripts (Replace init.d)

**Input**: Design documents from `/specs/006-systemd-linux-services/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md
**Source issue**: intersoftdatalabs-in/percussioncms#962

**Tests**: REQUIRED for every behavioral code change (Constitution III — Test Discipline). Each user story MUST include test tasks. Prefer fail-then-pass (write/adjust tests first).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact mono-repo file paths in descriptions (module + `src/main` / `src/test`)

## Path Conventions

- **Mono-repo modules** (Percussion CMS): use real module roots, e.g.
  - Installer / distribution: `modules/perc-distribution-tree/`, `modules/perc-jetty/`
  - Legacy install tree (deletion target): `system/release/installer/Linux/`, `system/release/installer/unix/`
  - DTS distribution: `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/`
  - Test harness: `docker/systemd-test/`
- Shell scripts and unit templates are not Java; do NOT run `./mvn-env.sh` against them directly. Use `shellcheck` and the integration test harness instead.
- Build wiring changes still go through `./mvn-env.sh` for any touched module's `pom.xml`.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm ownership, AGENTS rules, and build baseline for touched modules

- [ ] T001 Read `AGENTS.md` (root) and `modules/perc-jetty/AGENTS.md` and apply Rule Discovery Protocol for `modules/perc-distribution-tree` and `system/`
- [ ] T002 Confirm branch JDK (21 on `development`) and verify `./mvn-env.sh -pl modules/perc-distribution-tree -am test` baseline passes
- [ ] T003 [P] Confirm `modules/perc-jetty/src/main/jetty/StartJetty.sh` and `StopJetty.sh` exist and are the runtime entry points we will reference from the unit template
- [ ] T004 [P] Confirm `shellcheck` is available in the dev environment for new shell scripts; add to CI gate if missing

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared prerequisites that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Create the new `docker/systemd-test/` harness directory and `docker/systemd-test/Dockerfile` (Ubuntu 22.04 with `/sbin/init` as entrypoint, cgroups mounted, JDK 21 installed) so the integration test in US1 can run end-to-end
- [ ] T006 [P] Create `docker/systemd-test/run-tests.sh` and `docker/systemd-test/README.md` documenting how to launch the harness
- [ ] T007 Enumerate every reference to the legacy init.d scripts across the repository (grep for `percussion-service.sh`, `InstallDaemon.sh`, `InstallPublisherDaemon.sh`, `InstallFTSDaemon.sh`, `DTSStagingService.sh`, `DTSProductionService.sh`, `chkconfig`, `update-rc.d`, `/etc/rc?.d`, `/etc/init.d`); record the list in `specs/006-systemd-linux-services/checklists/legacy-initd-references.md`
- [ ] T008 [P] Read the existing `modules/perc-jetty/src/main/jetty/service/install-jetty-service.sh` and `modules/perc-jetty/src/main/jetty/StartJetty.sh` and capture the full set of env vars the legacy installer writes to `/etc/default/<name>` (the source of truth for the env-file contract)
- [ ] T009 [P] Update `modules/perc-distribution-tree/src/main/resources/META-INF/MANIFEST.MF` (or equivalent assembly descriptor) to include the new `systemd/` and `env/` resource directories in the install tree
- [ ] T010 Create `modules/perc-distribution-tree/scripts/README.md` documenting the new `install-systemd-units.sh` and `uninstall-systemd-units.sh` scripts (per AGENTS rule: scripts need a README)
- [ ] T011 [P] Add a `shellcheck` step to the CI pipeline that runs against `modules/perc-distribution-tree/scripts/*.sh` and `modules/perc-jetty/src/main/jetty/service/*.sh` (modify `.github/workflows/` or equivalent)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Fresh Install on a systemd Linux Host (Priority: P1) 🎯 MVP

**Goal**: A clean CMS install on a systemd Linux host ends with the CMS running under `systemd` (managed by `percussioncms@default.service`), all logs in the journal, autostart on boot enabled.

**Independent Test**: Run `docker/systemd-test/run-tests.sh scenario-fresh-install` and confirm the harness asserts `systemctl is-active percussioncms@default` returns `active`, `curl` reaches the CMS port, and `journalctl -u percussioncms@default` shows startup logs.

### Tests for User Story 1 (REQUIRED) ⚠️

> **NOTE: Write or update these tests FIRST; ensure they FAIL before implementation, then PASS**

- [ ] T012 [P] [US1] Write a shell-level bats test `modules/perc-jetty/src/test/shell/install-systemd-units.bats` covering: fresh install (no existing init.d) creates `/etc/systemd/system/percussioncms@default.service`, `/etc/percussion/cms-default.env` (mode `0640`), runs `daemon-reload`/`enable`/`start`, and `systemctl is-active` returns `active`
- [ ] T013 [P] [US1] Add a negative-path bats test case covering FR-002's "report clear success/failure" contract: (a) env file with mode `0644` (group/world readable) → installer exits 2, unit file is NOT installed, `systemctl is-enabled percussioncms@default` returns `disabled`; (b) non-systemd PID 1 (simulated via `unshare --pid --fork` or a chroot) → installer exits 2 with the "systemd is not PID 1" message; (c) `JETTY_HOME` path missing → installer exits 2 with the missing-path message
- [ ] T014 [P] [US1] Write a JUnit 5 test `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/systemd/EnvFileSchemaTest.java` validating the env-file key set, mode `0640`, and rejection of `$(`, backticks, `export` per `specs/006-systemd-linux-services/contracts/env-file.md` (also serves as the SC-006 abuse-case test for env-file injection)
- [ ] T015 [P] [US1] Write a JUnit 5 test `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/systemd/UnitTemplateRenderTest.java` rendering the template with a sample env and asserting `systemd-analyze verify` (or static structural check) passes the contract from `specs/006-systemd-linux-services/contracts/unit-template.md`

### Implementation for User Story 1

- [ ] T016 [P] [US1] Create the unit template `modules/perc-distribution-tree/src/main/resources/systemd/percussioncms@.service.template` per `specs/006-systemd-linux-services/contracts/unit-template.md` (must declare `Type=simple`, `EnvironmentFile=/etc/percussion/cms-%i.env`, `Restart=on-failure`, `RestartSec=30s`, `StartLimitBurst=5`, `StartLimitIntervalSec=600s`, `NoNewPrivileges=true`, `ProtectSystem=strict`, and the rest of the hardening directives)
- [ ] T017 [P] [US1] Create the env-file template `modules/perc-distribution-tree/src/main/resources/env/percussioncms.env.template` with all keys from `specs/006-systemd-linux-services/contracts/env-file.md` (PERC_ROOT, JETTY_HOME, JETTY_BASE, JETTY_DEFAULTS, JETTY_RUN, JETTY_CONF, JETTY_START_LOG, JETTY_PID, JAVA_HOME, JAVA, JAVA_OPTIONS, JETTY_ARGS, JETTY_USER, INSTANCE_NAME)
- [ ] T018 [US1] Create `modules/perc-distribution-tree/scripts/install-systemd-units.sh` implementing the 8-phase flow per `specs/006-systemd-linux-services/contracts/installer-script.md` (probe → detect → validate → render → migrate (no-op on fresh install) → enable → start → report), with `--perc-root`, `--user`, `--instance`, `--dry-run`, `--force`, `--help` flags and exit codes 0/1/2/3
- [ ] T019 [US1] Create `modules/perc-distribution-tree/scripts/uninstall-systemd-units.sh` that does the reverse: `systemctl disable --now`, `rm` the unit and env file, `systemctl daemon-reload`, `reset-failed`
- [ ] T020 [US1] Wire the new scripts into the CMS installer pipeline (Maven assembly / `perc-ant` install XMLs) so the CMS installer invokes `install-systemd-units.sh` on a fresh install; update `modules/perc-distribution-tree/scripts/README.md` accordingly
- [ ] T021 [US1] Run `shellcheck modules/perc-distribution-tree/scripts/install-systemd-units.sh modules/perc-distribution-tree/scripts/uninstall-systemd-units.sh`; fix every reported issue
- [ ] T022 [US1] Run `./mvn-env.sh -pl modules/perc-distribution-tree -am test`; ensure the JUnit tests T014 / T015 pass
- [ ] T023 [US1] Run the bats test T012 inside `docker/systemd-test/`; confirm Scenario 1 from `specs/006-systemd-linux-services/quickstart.md` passes end-to-end

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently — the CMS is installable on a fresh systemd host with no init.d artifacts.

---

## Phase 4: User Story 2 - Existing Installation Upgrade From init.d to systemd (Priority: P2)

**Goal**: An existing init.d-based CMS install is migrated in place by the upgrade installer: legacy scripts/symlinks removed, systemd units installed and active, no operator intervention beyond running the installer.

**Independent Test**: Seed a legacy init.d install (Scenario 2 in `quickstart.md`), run the upgrade installer inside the `docker/systemd-test/` harness, assert legacy artifacts are gone, the systemd unit is active, and the CMS is reachable.

### Tests for User Story 2 (REQUIRED) ⚠️

- [ ] T024 [P] [US2] Add a bats test case to `modules/perc-jetty/src/test/shell/install-systemd-units.bats` covering: seeded `/etc/init.d/PercussionCMS` + `/etc/rc?.d/S99PercussionCMS` symlinks; after `install-systemd-units.sh`, the legacy script and symlinks are absent, `chkconfig --list | grep percussion` is empty (or the tool is missing), and the systemd unit is active
- [ ] T025 [P] [US2] Add a JUnit 5 test `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/systemd/LegacyDetectionTest.java` covering: detection of multiple init.d patterns (`percussion-service.sh`, `PercussionD`, `PercussionCMS`, `DTSStagingService`, `DTSProductionService`), rc?.d symlink enumeration, and `/etc/default/<name>` parsing
- [ ] T026 [P] [US2] Add a JUnit 5 test `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/systemd/IdempotencyTest.java` covering: re-running the installer (a) on a clean host (no-op, exit 0), (b) after manual unit deletion (recreates, starts, exit 0), (c) with conflicting init.d + systemd state (exit 3 unless `--force`)

### Implementation for User Story 2

- [ ] T027 [US2] Extend `modules/perc-distribution-tree/scripts/install-systemd-units.sh` with the upgrade-only migration phase 5 (legacy service stop, `chkconfig --del` / `update-rc.d -f remove`, symlink removal, legacy script removal) per Decision 4 in `specs/006-systemd-linux-services/research.md`
- [ ] T028 [US2] Extend the same script with per-instance detection so multiple init.d-prefixed CMS instances each map to a `percussioncms@<instance>.service` (FR-004a); default instance name is `default`, additional ones are `instance2`, `instance3`, …
- [ ] T029 [US2] Extend the same script with FR-011 recovery: detect partial-migration state (init.d removed but no systemd unit present) and re-create the unit + env file
- [ ] T030 [P] [US2] Physically delete the legacy init.d scripts per FR-007: `system/release/installer/Linux/percussion-service.sh`, `system/release/installer/Linux/InstallPublisherDaemon.sh`, `system/release/installer/Linux/InstallDaemon.sh`, `system/release/installer/Linux/InstallFTSDaemon.sh`, and their `system/release/installer/unix/` counterparts
- [ ] T031 [P] [US2] Physically delete the DTS init.d scripts: `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/DTSStagingService.sh` and `DTSProductionService.sh`
- [ ] T032 [US2] Update every installer manifest entry that referenced the deleted scripts (use the enumeration from T007); confirm `mvn -pl modules/perc-ant` validate-pom and any assembly XML still resolve
- [ ] T033 [US2] Add a hardening note to `install-systemd-units.sh`: when parsing legacy `/etc/default/<name>`, warn (not silently drop) on values matching secret patterns (`PASSWORD`, `SECRET`, `KEY`, `TOKEN`); do NOT copy such values into the new env file
- [ ] T034 [US2] Run `shellcheck` and `./mvn-env.sh -pl modules/perc-distribution-tree -am test`; ensure T025 / T026 pass
- [ ] T035 [US2] Run the bats test T024 inside `docker/systemd-test/`; confirm Scenario 2 from `quickstart.md` passes

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently — fresh installs and in-place upgrades both land on systemd.

---

## Phase 5: User Story 3 - Operator Lifecycle and Diagnostics (Priority: P3)

**Goal**: An operator uses only `systemctl` and `journalctl` to manage the CMS — start, stop, restart, status, autostart toggle, and crash-recovery behavior all work via the standard systemd interface.

**Independent Test**: Run Scenarios 3 and 6 from `quickstart.md` inside the `docker/systemd-test/` harness; assert each `systemctl` subcommand exits 0 and the post-conditions hold.

### Tests for User Story 3 (REQUIRED) ⚠️

- [ ] T036 [P] [US3] Add a bats test case to `modules/perc-jetty/src/test/shell/install-systemd-units.bats` covering: `systemctl stop` then `is-active` → `inactive`; `systemctl start` then `is-active` → `active`; `systemctl restart` exits 0; `disable`/`enable` round-trips `is-enabled`; `kill -9 <pid>` is followed by `is-active` → `active` within `RestartSec=30s`
- [ ] T037 [P] [US3] Add a bats test case for the crash-loop guard: after `StartLimitBurst=5` crashes within `StartLimitIntervalSec=600s`, the unit enters `failed` state; `systemctl reset-failed` allows a fresh start

### Implementation for User Story 3

- [ ] T038 [US3] Confirm (and tune if needed) the unit template's `TimeoutStartSec`, `TimeoutStopSec`, `RestartSec`, `StartLimitBurst`, `StartLimitIntervalSec` values match the spec; record final values in `specs/006-systemd-linux-services/contracts/unit-template.md` (Decision 1 / 3 in `research.md`)
- [ ] T039 [US3] Confirm the unit's `StandardOutput=journal` and `StandardError=journal` actually capture CMS output (not just Jetty's stdout): if CMS writes to a file only, add a `tail -F` wrapper or document the limitation in the operator guide
- [ ] T040 [P] [US3] Add a JUnit 5 test `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/systemd/RestartPolicyTest.java` asserting the rendered unit string contains `Restart=on-failure`, `RestartSec=30s`, `StartLimitBurst=5`, `StartLimitIntervalSec=600s` (per FR-006a)
- [ ] T041 [US3] Run the bats tests T036 / T037 inside `docker/systemd-test/`; confirm Scenarios 3 and 6 from `quickstart.md` pass
- [ ] T042 [US3] Run `./mvn-env.sh -pl modules/perc-distribution-tree -am test`; ensure T040 passes

**Checkpoint**: At this point, User Stories 1, 2, and 3 should all work independently.

---

## Phase 6: User Story 4 - Delivery Tier Suite (DTS) Services Move to systemd (Priority: P3)

**Goal**: The DTS staging and production services install as systemd units with a parallel upgrade path from the legacy `DTSStagingService.sh` / `DTSProductionService.sh` init.d scripts.

**Independent Test**: Run Scenario 5 from `quickstart.md` inside the `docker/systemd-test/` harness; assert both `percussiondts-staging@<instance>.service` and `percussiondts-production@<instance>.service` are active.

### Tests for User Story 4 (REQUIRED) ⚠️

- [ ] T043 [P] [US4] Add a bats test case to `modules/perc-jetty/src/test/shell/install-systemd-units.bats` covering: DTS install (template + env files for staging and production) results in two active units; legacy `DTSStagingService.sh` / `DTSProductionService.sh` (seeded) are removed after upgrade

### Implementation for User Story 4

- [ ] T044 [P] [US4] Create the DTS unit templates `modules/perc-distribution-tree/src/main/resources/systemd/percussiondts-staging@.service.template` and `percussiondts-production@.service.template` (parallel to the CMS template, same hardening and restart policy)
- [ ] T045 [P] [US4] Create the DTS env templates `modules/perc-distribution-tree/src/main/resources/env/percussiondts-staging.env.template` and `percussiondts-production.env.template`
- [ ] T046 [US4] Wire the DTS templates into the DTS installer pipeline (`deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/pom.xml` or its assembly); reuse the same `install-systemd-units.sh` from T018 with `--instance staging` / `--instance production`
- [ ] T047 [P] [US4] Extend the legacy-detection test T025 to recognize `DTSStagingService` / `DTSProductionService` (already covered, but confirm per-instance mapping produces two separate units)
- [ ] T048 [US4] Run the bats test T043 inside `docker/systemd-test/`; confirm Scenario 5 from `quickstart.md` passes

**Checkpoint**: All user stories are now independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T049 [P] Update `modules/perc-distribution-tree/README.md` to point at the new operator guide and link to `specs/006-systemd-linux-services/quickstart.md`
- [ ] T050 [P] Update `modules/perc-jetty/README.md` to note the deprecation of `install-jetty-service.sh` for the init.d path; keep the script as a no-op dispatcher (or delete per FR-007 if the new script fully replaces it)
- [ ] T051 [P] Create `modules/perc-distribution-tree/src/site/markdown/systemd-linux-services.md` (operator guide, supported-platform matrix, install/upgrade procedure, troubleshooting) and add a menu item in `modules/perc-distribution-tree/src/site/site.xml`
- [ ] T052 [P] Create `modules/perc-jetty/src/site/markdown/worklog/systemd-linux-services.md` describing the change for the next maintainer; add a worklog menu item in `modules/perc-jetty/src/site/site.xml`
- [ ] T053 [P] Add a `BREAKING_CHANGES.md` entry (or existing release-notes file) noting: "init.d support is removed in this release. Hosts running legacy init.d installs MUST run the upgrade installer in this release to convert to systemd; out-of-support otherwise." per Q1 resolution
- [ ] T054 Security review: confirm `EnvironmentFile=` is mode `0640`, owner `root:<group>`; confirm no secret material can land in `/etc/percussion/*.env`; confirm `ProtectSystem=strict` / `NoNewPrivileges=true` do not break Jetty (Scenarios 1–5 still pass after enabling them — they already do in T016)
- [ ] T055 [P] Add a regression test for the edge case in `data-model.md`: multi-host, multi-instance seed (two distinct `PERC_ROOT` paths) yields two `percussioncms@<instance>.service` units with independent env files
- [ ] T056 [P] Run Spotless (`./mvn-env.sh spotless:check`) against any module whose `pom.xml` was touched
- [ ] T057 [P] Verify `.specify/feature.json` still points at `specs/006-systemd-linux-services` and the checklist `specs/006-systemd-linux-services/checklists/requirements.md` is up to date with all resolutions
- [ ] T058 [P] Add a `dependabot.yml` entry or `CODEOWNERS` rule so the new `modules/perc-distribution-tree/scripts/*.sh` and `modules/perc-distribution-tree/src/main/resources/systemd/**` have an owning team
- [ ] T059 Final end-to-end run of all 7 scenarios from `specs/006-systemd-linux-services/quickstart.md` in `docker/systemd-test/`; attach the harness output to the PR

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P3)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1) Fresh Install**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2) Upgrade from init.d**: Depends on US1 (the systemd unit template, env-file template, and `install-systemd-units.sh` from US1 are extended in US2 with the migration phase and per-instance detection). US2 cannot ship without US1's unit + env files.
- **User Story 3 (P3) Operator Lifecycle**: Depends on US1 (the unit template carries the restart policy and journal directives US3 tests). US3 is mostly test work plus a few template tweaks.
- **User Story 4 (P3) DTS**: Depends on US1 (same `install-systemd-units.sh` is reused with `--instance staging|production`). The DTS-specific templates T044 / T045 are parallel to US1's templates.

### Within Each User Story

- Tests MUST be written/updated and FAIL before implementation, then PASS after
- Unit files and env-file templates come first; the installer script that wires them comes second; integration tests come last
- Deletion of legacy init.d scripts (US2) happens AFTER the new path is tested green, not before
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- T012 / T013 / T014 / T015 (US1 tests) can be authored in parallel
- T016 / T017 (US1 templates) can be authored in parallel
- T030 / T031 (US2 deletions) can be authored in parallel
- T044 / T045 (US4 DTS templates) can be authored in parallel
- T049 / T050 / T051 / T052 (US7 docs) can be authored in parallel
- Once Foundational phase completes, US1 can start; US2/US3/US4 should wait for US1's unit + env template to be frozen, then proceed in parallel by different developers (US2 = installer-logic, US3 = test work, US4 = DTS templates)

---

## Parallel Example: User Story 1

```bash
# Launch tests for User Story 1 together:
Task: "Write bats test in modules/perc-jetty/src/test/shell/install-systemd-units.bats"
Task: "Write EnvFileSchemaTest in modules/perc-distribution-tree/src/test/java/.../EnvFileSchemaTest.java"
Task: "Write UnitTemplateRenderTest in modules/perc-distribution-tree/src/test/java/.../UnitTemplateRenderTest.java"

# Independent production artifacts in parallel after tests are red:
Task: "Create unit template in modules/perc-distribution-tree/src/main/resources/systemd/percussioncms@.service.template"
Task: "Create env template in modules/perc-distribution-tree/src/main/resources/env/percussioncms.env.template"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently via `docker/systemd-test/`
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Add User Story 4 → Test independently → Deploy/Demo
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once US1's unit + env templates are frozen:
   - Developer A: User Story 2 (installer migration phase + legacy deletions)
   - Developer B: User Story 3 (operator lifecycle tests)
   - Developer C: User Story 4 (DTS templates + installer wiring)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Per Constitution IX (PR Review Comment Resolution): the PR author MUST follow root `AGENTS.md` "PR Review Comment Resolution" — inline reply + `resolveReviewThread` mutation — for every review thread before considering the PR merge-ready
- All deletion tasks (T030 / T031) MUST be paired with a `grep` verification step (T032) to catch every reference in the source tree before deletion
