# Tasks: React Active Assembly (996)

**Prerequisites**: `spec.md`, `plan.md` · **Branch**: `spec-plan-995-996` (planning; implementation branches per workstream)
**Tests**: Constitution III + WebUI Playwright hard gate — Vitest + Playwright are first-class, not optional.

## Phase 1: Setup

- [x] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `WebUI/` module rules, `rest/`, `projects/sitemanage/`, `modules/perc-qa-automation/` — tracked by #6154
- [x] T002 Confirm branch is based on current `main`; JDK 21 via `./mvnw -version`; WebUI toolchain builds — tracked by #6154

## Phase 2: WP0 inventory (blocking)

- [x] T003 Map FR-001–006 to in-tree files + existing tests; record per-FR status in `checklists/coverage.md` — tracked by #6154
- [x] T004 Confirm FR-007/008 have no hidden implementation (Compare search negative; #6104 scope as-filed); record decision inputs — tracked by #6154

## Phase 3: WP1 verification battery

### Tests

- [x] T005 [P] Vitest for assembly pure helpers lacking coverage (`overlayFields`, `slotContext`) — tracked by #6154 (already covered: `overlayFields.test.ts`, `slotContext.test.ts`; verified by T014 run)
- [x] T006 [P] Playwright spec: assembly open + template resolve + preview render + overlay chrome (FR-001/002/003) — tracked by #6154 (in `explorer-active-assembly.spec.js`; verified on H2)
- [x] T007 [P] Playwright spec: slot add/create/arrange on selected slot (FR-004) — tracked by #6154 (in `explorer-active-assembly.spec.js` + `explorer-create-item-in-slot` / `explorer-move-relationship-slot` / `explorer-link-existing-slot`; verified on H2)
- [x] T008 [P] Playwright spec: scalar overlay edit persists via itemmanagement (FR-005, mirrors closed #3475) — tracked by #6154 (`assembly-text-field.spec.js` family; verified on H2)
- [x] T009 [P] Playwright spec: denied/failed op messaging (FR-006) — tracked by #6154 (unmatched-template + new `@aa-denied` 403 test in `explorer-active-assembly.spec.js`; verified on H2)

### Implementation

- [x] T010 Fix only what WP1 proves broken (real bugs) — tracked by #6154 (no production fix needed; battery gap closed by the `@aa-denied` test itself)

## Phase 4: WP2 decisions + builds

- [ ] T011 FR-007: implement #6104 related-content menu parity scope, OR record explicit reduced-scope decision with rationale in #6104 — tracked by #6104 (menu parity); workstream tracked by #6154
- [x] T012 FR-008: implement Compare add-on, OR record defer decision with rationale in spec closeout — tracked by #6154 (**defer**; see `checklists/closeout.md`)
- [x] T013 Add `perc-i18n` TMX keys for any new assembly strings (N/A with reason if fallback-only) — tracked by #6154 (**N/A**: no new strings; fallback-only is the repo-wide convention; see `checklists/closeout.md`)

## Phase 5: WP3 closeout

- [x] T014 Standalone `clean install` on every changed module; record BUILD SUCCESS — tracked by #6154 (docs + Playwright-test-only change: no Maven module sources touched; WebUI Vitest + H2 surface run are the gates — see PR body)
- [x] T015 Erlang pre-commit review; fix hard-gate findings — tracked by #6154
- [x] T016 Record closeout note linking evidence (battery results, T011/T012 decisions, #6104 state, #6110 disposition); tick boxes — tracked by #6154 (`checklists/closeout.md`; #6104 OPEN p3 owns T011)
- [x] T017 Open implementation PR (or no-PR closeout if WP1 finds zero gaps and WP2 defers with rationale); pause for review/merge — tracked by #6154

## Dependencies

Setup → WP0 inventory (blocks everything) → WP1 battery → WP2 decisions/builds → WP3 closeout.
