# Tasks: React Active Assembly (996)

**Prerequisites**: `spec.md`, `plan.md` · **Branch**: `spec-plan-995-996` (planning; implementation branches per workstream)
**Tests**: Constitution III + WebUI Playwright hard gate — Vitest + Playwright are first-class, not optional.

## Phase 1: Setup

- [ ] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `WebUI/` module rules, `rest/`, `projects/sitemanage/`, `modules/perc-qa-automation/`
- [ ] T002 Confirm branch is based on current `main`; JDK 21 via `./mvnw -version`; WebUI toolchain builds

## Phase 2: WP0 inventory (blocking)

- [ ] T003 Map FR-001–006 to in-tree files + existing tests; record per-FR status in `checklists/coverage.md`
- [ ] T004 Confirm FR-007/008 have no hidden implementation (Compare search negative; #6104 scope as-filed); record decision inputs

## Phase 3: WP1 verification battery

### Tests

- [ ] T005 [P] Vitest for assembly pure helpers lacking coverage (`overlayFields`, `slotContext`)
- [ ] T006 [P] Playwright spec: assembly open + template resolve + preview render + overlay chrome (FR-001/002/003)
- [ ] T007 [P] Playwright spec: slot add/create/arrange on selected slot (FR-004)
- [ ] T008 [P] Playwright spec: scalar overlay edit persists via itemmanagement (FR-005, mirrors closed #3475)
- [ ] T009 [P] Playwright spec: denied/failed op messaging (FR-006)

### Implementation

- [ ] T010 Fix only what WP1 proves broken (real bugs)

## Phase 4: WP2 decisions + builds

- [ ] T011 FR-007: implement #6104 related-content menu parity scope, OR record explicit reduced-scope decision with rationale in #6104
- [ ] T012 FR-008: implement Compare add-on, OR record defer decision with rationale in spec closeout
- [ ] T013 Add `perc-i18n` TMX keys for any new assembly strings (N/A with reason if fallback-only)

## Phase 5: WP3 closeout

- [ ] T014 Standalone `clean install` on every changed module; record BUILD SUCCESS
- [ ] T015 Erlang pre-commit review; fix hard-gate findings
- [ ] T016 Record closeout note linking evidence (battery results, T011/T012 decisions, #6104 state, #6110 disposition); tick boxes
- [ ] T017 Open implementation PR (or no-PR closeout if WP1 finds zero gaps and WP2 defers with rationale); pause for review/merge

## Dependencies

Setup → WP0 inventory (blocks everything) → WP1 battery → WP2 decisions/builds → WP3 closeout.
