# Tasks: React Content Editor (995)

**Prerequisites**: `spec.md`, `plan.md` · **Branch**: `spec-plan-995-996` (planning; implementation branches per workstream)
**Tests**: Constitution III + WebUI Playwright hard gate — Vitest + Playwright are first-class, not optional.

## Phase 1: Setup

- [ ] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `WebUI/` module rules (React-only, Playwright gate), `rest/`, `projects/sitemanage/`, `modules/perc-qa-automation/` — tracked by #6153
- [ ] T002 Confirm branch is based on current `main`; JDK 21 via `./mvnw -version`; WebUI toolchain builds (`npm run build:modern` clean) — tracked by #6153

## Phase 2: WP0 inventory (blocking)

- [ ] T003 Map FR-001–009 to in-tree files + existing tests (Vitest/Playwright); record per-FR status (covered / thin / missing) in `checklists/coverage.md` — tracked by #6153
- [ ] T004 Map FR-010–014 widgets (TipTap/Html, File, Image, Keyword, Community, PromoteForm) to their REST paths; record gaps vs spec "Later" list — tracked by #6153

## Phase 3: WP1 verification battery

### Tests

- [ ] T005 [P] Vitest for editor pure helpers lacking coverage (field mapping, label fallback, error mapping per `editorFieldErrors.ts`) — tracked by #6153
- [ ] T006 [P] Playwright spec: open edit/view modes, checkout indicator, scalar save round-trip (FR-001/002/003/005) — tracked by #6153
- [ ] T007 [P] Playwright spec: new-item create + template picker branch (FR-006/007) — tracked by #6153
- [ ] T008 [P] Playwright spec: rich/upload/keyword/community/promote controls smoke (FR-010/011/012/013), axe-scoped — tracked by #6153
- [ ] T009 [P] Playwright spec: denied/failed save messaging (FR-009) — tracked by #6153

### Implementation

- [ ] T010 Fix only what WP1 proves broken (real bugs); no re-verification rebuilds — tracked by #6153
- [ ] T011 Add `perc-i18n` TMX keys for any new editor strings (N/A with reason if fallback-only like the explorer surface) — tracked by #6104 (menu parity); workstream tracked by #6153

## Phase 4: WP3 closeout

- [ ] T012 Standalone `clean install` on every changed module; record BUILD SUCCESS — tracked by #6153
- [ ] T013 Erlang pre-commit review; fix hard-gate findings — tracked by #6153
- [ ] T014 Record closeout note linking evidence (battery results, gap fixes or no-gaps-found, #6110/#4530 disposition); tick boxes — tracked by #6153
- [ ] T015 Open implementation PR (or no-PR closeout if WP1 finds zero gaps — tasks + evidence only); pause for review/merge — tracked by #6153

## Dependencies

Setup → WP0 inventory (blocks everything) → WP1 battery → WP2 gap fixes (only failures) → WP3 closeout.

## Epic traceability (issue #6102)

Each spec bullet maps to exactly one owning epic — see `plan.md` § "Epic ownership map". Summary: bullets 1–9, 10b, 10c, 11 → #4532 (React content editor increments); bullet 10a (rich HTML / TipTap) → #5622 (TinyMCE replacement). Ambiguity decisions D1 (keyword/community → #4532) and D2 (promote form → #4532) are recorded in `plan.md`; #5986 duplicate-triage confusion is resolved (CLOSED 2026-11-11).
