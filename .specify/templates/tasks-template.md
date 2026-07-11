---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: REQUIRED for every behavioral code change (Constitution III — Test Discipline).
Each user story MUST include test tasks. Prefer fail-then-pass (write/adjust tests first).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact mono-repo file paths in descriptions (module + `src/main` / `src/test`)

## Path Conventions

- **Mono-repo modules** (Percussion CMS): use real module roots, e.g.
  - Core: `system/services/src/main/java/...`, `system/**/src/test/java/...`
  - REST: `rest/src/main/java/...`, `rest/src/test/java/...`
  - UI backend: `projects/sitemanage/src/main/java/...`
  - UI front end: `WebUI/`
  - DTS: `deliverytiersuite/delivery-tier-suite/<service>/...`
  - Shared: `modules/utils/`, `modules/perc-security-utils/`, etc.
- Paths in sample tasks below are illustrative — replace with plan.md structure
- Run builds/tests with `./mvn-env.sh` (or `mvn-env.bat`) against the branch JDK

<!-- 
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.
  
  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/
  
  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment
  
  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm ownership, AGENTS rules, and build baseline for touched modules

- [ ] T001 Identify owning module path(s) and read AGENTS hierarchy (root + module)
- [ ] T002 Confirm branch JDK and verify `./mvn-env.sh -pl <module> -am test` (or compile) baseline
- [ ] T003 [P] Note Spotless / formatting requirements for each touched module

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared prerequisites that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

Examples of foundational tasks (adjust to the feature; delete irrelevant ones):

- [ ] T004 Map existing services/adaptors/APIs that will be extended (cite class paths)
- [ ] T005 [P] Assess schema/package/install impact (TableFactory, `.ppkg`, distribution tree)
- [ ] T006 [P] Assess security surface (authZ, XML, upload, redirects, logging)
- [ ] T007 Define contract deltas (REST/OpenAPI, SOAP, internal sitemanage) if any
- [ ] T008 Plan error handling and log messages consistent with module patterns
- [ ] T009 Capture Complexity Tracking justifications for any constitution exceptions

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 (REQUIRED) ⚠️

> **NOTE: Write or update these tests FIRST; ensure they FAIL before implementation, then PASS**

- [ ] T010 [P] [US1] Unit test for [behavior] in `<module>/src/test/java/.../Test[Name].java`
- [ ] T011 [P] [US1] Contract/integration test when API or CMS↔DTS boundary changes

### Implementation for User Story 1

- [ ] T012 [P] [US1] Implement/adjust domain or service code in `<module>/src/main/java/...`
- [ ] T013 [US1] Wire resource/adaptor/UI as needed (e.g., `rest/` adaptor pattern)
- [ ] T014 [US1] Validation, error handling, and safe logging
- [ ] T015 [US1] Docs/i18n updates for user-visible changes
- [ ] T016 [US1] Run `./mvn-env.sh -pl <module> -am test` (and Spotless if required)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 (REQUIRED) ⚠️

- [ ] T018 [P] [US2] Unit test for [behavior] in `<module>/src/test/java/.../Test[Name].java`
- [ ] T019 [P] [US2] Contract/integration test when API or CMS↔DTS boundary changes

### Implementation for User Story 2

- [ ] T020 [P] [US2] Implement/adjust code in `<module>/src/main/java/...`
- [ ] T021 [US2] Wire resource/adaptor/UI as needed
- [ ] T022 [US2] Integrate with User Story 1 components only if required (keep independently testable)
- [ ] T023 [US2] Run `./mvn-env.sh -pl <module> -am test` (and Spotless if required)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 (REQUIRED) ⚠️

- [ ] T024 [P] [US3] Unit test for [behavior] in `<module>/src/test/java/.../Test[Name].java`
- [ ] T025 [P] [US3] Contract/integration test when API or CMS↔DTS boundary changes

### Implementation for User Story 3

- [ ] T026 [P] [US3] Implement/adjust code in `<module>/src/main/java/...`
- [ ] T027 [US3] Wire resource/adaptor/UI as needed
- [ ] T028 [US3] Run `./mvn-env.sh -pl <module> -am test` (and Spotless if required)

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Module README / Maven site / package notes updates
- [ ] TXXX Limited cleanup only in files already touched (no drive-by refactors)
- [ ] TXXX [P] Gap-fill unit/integration tests for edge cases found during implementation
- [ ] TXXX Security review of authZ, XML, upload, redirect, and logging changes
- [ ] TXXX Spotless check on touched modules
- [ ] TXXX Validate quickstart.md / install impact if packaging changed

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests MUST be written/updated and FAIL before implementation, then PASS after
- Prefer service/domain changes before REST resources or UI wiring
- Adaptors before (or with) JAX-RS resources when changing `rest/`
- Core implementation before cross-module integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Independent classes within a story marked [P] can be implemented in parallel
- Different user stories can be worked on in parallel by different team members when module ownership does not conflict

---

## Parallel Example: User Story 1

```bash
# Launch tests for User Story 1 together:
Task: "Unit test for [behavior] in system/.../src/test/java/.../TestFoo.java"
Task: "Contract test for [REST resource] in rest/src/test/java/.../FooResourceTest.java"

# Independent production classes in parallel after tests are red:
Task: "Implement service change in system/services/src/main/java/.../PSFooService.java"
Task: "Implement adaptor in rest/src/main/java/.../PSFooAdaptor.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
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
