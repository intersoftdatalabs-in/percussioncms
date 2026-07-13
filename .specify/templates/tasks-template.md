# Tasks: [FEATURE NAME]

**Prerequisites**: plan.md, spec.md

## Phase 1: Setup
- [ ] T001 Identify owning module path(s) and read AGENTS hierarchy (root + module)
- [ ] T002 Confirm branch JDK and verify `./mvn-env.sh -pl <module> -am test` baseline

## Phase 2: Foundational (Blocking Prerequisites)
- [ ] T003 Map existing services/adaptors/APIs that will be extended
- [ ] T004 Assess security surface (authZ, XML, upload, redirects, logging)

## Phase 3: User Story 1 - [Title] (Priority: P1)
**Goal**: [Brief description]
**Independent Test**: [How to verify]

### Tests (Required)
- [ ] T005 [P] [US1] Unit test in `<module>/src/test/java/...`

### Implementation
- [ ] T006 [P] [US1] Implement/adjust domain or service code in `<module>/src/main/java/...`
- [ ] T007 [US1] Wire resource/adaptor/UI as needed
- [ ] T008 [US1] Commit changes and submit PR for review, pausing downstream tasks
- [ ] T009 [US1] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [ ] T010 [US1] Verify human approval and merge of PR before starting next story

## Phase 4: User Story 2 - [Title] (Priority: P2)
**Goal**: [Brief description]
**Independent Test**: [How to verify]

### Tests
- [ ] T011 [P] [US2] Unit test in `<module>/src/test/java/...`

### Implementation
- [ ] T012 [P] [US2] Implement/adjust code in `<module>/src/main/java/...`
- [ ] T013 [US2] Wire resource/adaptor/UI as needed
- [ ] T014 [US2] Commit changes and submit PR for review, pausing downstream tasks
- [ ] T015 [US2] Monitor Kilo Code check, address feedback, and resolve comments (pause on check failure)
- [ ] T016 [US2] Verify human approval and merge of PR before proceeding

## Phase N: Polish & Cross-Cutting Concerns
- [ ] T017 [P] Module README / Maven site / package notes updates
- [ ] T018 Security review of authZ, XML, upload, redirect, and logging changes
- [ ] T019 Spotless check on touched modules


## Dependencies & Execution Order
- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup. Blocks all stories.
- **User Stories (Phase 3+)**: Depend on Foundational. Run in priority order (P1 -> P2 -> P3) or parallel.
- **Polish (Phase N)**: Depends on all user stories.

## Parallel Execution Examples
```bash
# Independent tasks can run in parallel:
Task: "Unit test for [behavior] in system/.../src/test/java/.../TestFoo.java"
Task: "Implement service change in system/services/src/main/java/.../PSFooService.java"
```

## Implementation Strategy
- **MVP First**: Setup -> Foundational -> User Story 1 -> Validate.
- **Incremental Delivery**: Add each story sequentially, test independently, commit.
