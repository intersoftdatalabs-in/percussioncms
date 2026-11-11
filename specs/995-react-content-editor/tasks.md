# Tasks: React Content Editor (995)

**Prerequisites**: `spec.md`, `plan.md` · **Branch**: `spec-plan-995-996` (planning; implementation branches per workstream)
**Tests**: Constitution III + WebUI Playwright hard gate — Vitest + Playwright are first-class, not optional.

## Phase 1: Setup

- [ ] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `WebUI/` module rules (React-only, Playwright gate), `rest/`, `projects/sitemanage/`, `modules/perc-qa-automation/`
- [ ] T002 Confirm branch is based on current `main`; JDK 21 via `./mvnw -version`; WebUI toolchain builds (`npm run build:modern` clean)

## Phase 2: WP0 inventory (blocking)

- [ ] T003 Map FR-001–009 to in-tree files + existing tests (Vitest/Playwright); record per-FR status (covered / thin / missing) in `checklists/coverage.md`
- [ ] T004 Map FR-010–014 widgets (TipTap/Html, File, Image, Keyword, Community, PromoteForm) to their REST paths; record gaps vs spec "Later" list

## Phase 3: WP1 verification battery

### Tests

- [ ] T005 [P] Vitest for editor pure helpers lacking coverage (field mapping, label fallback, error mapping per `editorFieldErrors.ts`)
- [ ] T006 [P] Playwright spec: open edit/view modes, checkout indicator, scalar save round-trip (FR-001/002/003/005)
- [ ] T007 [P] Playwright spec: new-item create + template picker branch (FR-006/007)
- [ ] T008 [P] Playwright spec: rich/upload/keyword/community/promote controls smoke (FR-010/011/012/013), axe-scoped
- [ ] T009 [P] Playwright spec: denied/failed save messaging (FR-009)

### Implementation

- [ ] T010 Fix only what WP1 proves broken (real bugs); no re-verification rebuilds
- [ ] T011 Add `perc-i18n` TMX keys for any new editor strings (N/A with reason if fallback-only like the explorer surface)

## Phase 4: WP3 closeout

- [ ] T012 Standalone `clean install` on every changed module; record BUILD SUCCESS
- [ ] T013 Erlang pre-commit review; fix hard-gate findings
- [ ] T014 Record closeout note linking evidence (battery results, gap fixes or no-gaps-found, #6110/#4530 disposition); tick boxes
- [ ] T015 Open implementation PR (or no-PR closeout if WP1 finds zero gaps — tasks + evidence only); pause for review/merge

## Dependencies

Setup → WP0 inventory (blocks everything) → WP1 battery → WP2 gap fixes (only failures) → WP3 closeout.
