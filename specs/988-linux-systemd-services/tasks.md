# Tasks: Linux systemd Service Management

**Prerequisites**: plan.md, spec.md, research.md, contracts/  
**Branch**: `988-linux-systemd-services`  
**Issue**: #962

## Phase 1: Setup
- [x] T001 Identify owning module path(s) and read AGENTS hierarchy (root + `modules/perc-jetty/AGENTS.md`)
- [x] T002 Confirm branch JDK and verify structural test harness on `modules/perc-jetty` (packaging=pom surefire binding — may already exist from GH-939)

## Phase 2: Foundational (Blocking Prerequisites)
- [x] T003 Map `install-jetty-service.sh`, `rxjetty.sh`, assembly copy of `jetty/service/`, PID/`/etc/default` layout
- [x] T004 Assess security surface (root install, run-as user, no secrets in unit; journal may capture env — avoid logging passwords)

## Phase 3: User Story 1 - Native systemd install (Priority: P1)
**Goal**: Ship unit template; installer installs/enables systemd unit on systemd hosts  
**Independent Test**: Structural unit contract tests + dry-run/script assertions; manual `systemctl` smoke on Linux  

### Tests (Required)
- [x] T005 [P] [US1] Unit test `SystemdUnitTemplateTest` asserting contract keys in `contracts/systemd-unit-contract.md`
- [x] T006 [P] [US1] Unit test installer selection helpers / expected snippets (systemd vs init.d, no dual chkconfig on systemd path)

### Implementation
- [x] T007 [US1] Add systemd unit template under `modules/perc-jetty/src/main/jetty/service/` (e.g. `percussion-cms.service.in`)
- [x] T008 [US1] Update `install-jetty-service.sh` to detect systemd, install unit + EnvironmentFile + init.d start helper without SysV enable, `daemon-reload` + `enable`
- [x] T009 [US1] Update `modules/perc-jetty/README.md` (+ AGENTS note) for systemd install usage
- [ ] T010 [US1] Commit changes and submit PR for US1, pause for review/merge before US2

## Phase 4: User Story 2 - Slow start / journal (Priority: P1)
**Goal**: Timeout and journal behavior fix false-fail on long upgrades  
**Independent Test**: Contract asserts TimeoutStartSec ≥ 900 and journal directives; manual slow-start if available  

### Tests
- [x] T011 [P] [US2] Extend structural tests for `TimeoutStartSec`, `StandardOutput/Error=journal`, PIDFile consistency

### Implementation
- [x] T012 [US2] Set default `TimeoutStartSec=1800`, journal stdout/stderr, document drop-in override
- [x] T013 [US2] Align PIDFile path with `/etc/default` `JETTY_PID` and installer `JETTY_RUN` layout
- [ ] T014 [US2] Commit and PR for US2; merge before US3 *(combined with US1 in initial PR)*

## Phase 5: User Story 3 - Uninstall / migrate / init.d fallback (Priority: P2)
**Goal**: Clean uninstall; init.d fallback; no dual-start  
**Independent Test**: Uninstall removes unit; `--initd` forces SysV  

### Tests
- [x] T015 [P] [US3] Tests for uninstall paths and `--initd` / no dual-register rules

### Implementation
- [x] T016 [US3] systemd uninstall (`disable`, remove unit, daemon-reload); remove unit on uninstall
- [x] T017 [US3] Keep/force init.d path when no systemd or `--initd`
- [x] T018 [US3] Migration notes (init.d → systemd) in README
- [ ] T019 [US3] Commit and PR for US3 *(combined with US1/US2 in initial PR)*

## Phase 6: Polish & Cross-Cutting
- [x] T020 [P] Verify Windows `install-jetty-service.bat` untouched / still present
- [x] T021 [P] Update `quickstart.md` with final command names
- [x] T022 Spotless/format N/A for shell; ensure scripts use portable bash and LF
- [ ] T023 Link issue #962 in PR descriptions; close on final merge

## Dependencies & Execution Order
- Setup → Foundational → US1 → US2 → US3 → Polish  
- US2 may be combined with US1 in one PR if small (TimeoutStartSec + journal live in the same unit template)

## Implementation Strategy
- **MVP**: T001–T010 (native unit + install + tests + docs)  
- **Then**: timeout/journal polish if not in MVP unit  
- **Then**: uninstall/migration  
