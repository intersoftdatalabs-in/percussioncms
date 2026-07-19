# Quickstart validation: Unified React Content Explorer

**Feature**: `992-react-content-explorer`  
**Purpose**: Runnable validation scenarios for implementers and UAT—not a full test suite.

## Prerequisites

- Branch `992-react-content-explorer` (or merge target with this feature).
- **Product release target: 8.2** — full functional parity (SC-012) is required before labeling/shipping 8.2 GA.
- JDK 21 via `./mvn-env.sh` / `./mvn-env.bat`.
- Running CMS instance with at least one site, nested folders, and a user with content rights; admin user for ACL phase.
- Node/npm as required by WebUI Vite pipeline (see `WebUI/AGENTS.md` / `WebUI/README.md`).
- Contracts: [path-api.md](./contracts/path-api.md), [content-browser-host.md](./contracts/content-browser-host.md), [capability-matrix.md](./contracts/capability-matrix.md).

## Build / unit tests (dev)

```bash
# From repo root — adjust module goals per WebUI README if needed
./mvn-env.sh -pl WebUI -am test
# Frontend-focused (example; use project-standard WebUI frontend test goal)
cd WebUI && npm test -- --run src/test/ts/contentExplorer src/test/ts/contentBrowser
```

**Expected**: Vitest coverage for tree navigation, list pagination hooks, reduced actions, and browser selection filters is green for implemented phases.

## Scenario A — Core navigate (P0-Core / SC-001)

1. Sign in as content user.
2. Open Web Management content exploration (editor/explorer entry after rewire).
3. Confirm **tree + detail list** (not miller columns as primary).
4. Expand folders; select folder; verify children list.
5. Open a page/asset; verify editor/preview path works.
6. Create folder; rename; move or copy; delete with confirm.
7. Force permission error or session timeout (or mock); verify clear message.

**Pass**: 100% of steps without miller-column Finder or Desktop CE.

## Scenario B — Large folder (SC-005)

**Fixture**:
- Single folder with **≥500 children** (mixed page/asset type). Create once per test environment via scripted content seed (`scripts/create-large-folder-fixture.sh` — added in `tasks.md` Phase 2).
- Standard office network: wired or 802.11ac, <50 ms RTT to CMS host, no proxy throttle. Record network profile in test evidence.

**Steps**:
1. Sign in; navigate to fixture folder via modern explorer.
2. Select folder → list loads first page.
3. Scroll/page list.
4. Open one item.

**Pass criterion (SC-005)**:
- **p95** time from "select folder" → "selected item open in editor" **≤ 10 seconds** on the standard office network above.
- Recorded in `checklists/sc005-perf-evidence.md` (created in `tasks.md` Phase 2): run name, git SHA, fixture size, network profile, p50/p95/max times, pass/fail.
- Implementer Vitest gate: a separate Vitest perf test asserts that a mocked `paginatedFolder` for a 500-child fixture renders the first page and opens a selected item within the same 10 s budget on the dev machine, to catch regressions pre-CI.

## Scenario C — Finder hard cut (SC-006)

1. Production-like build after US6 Finder phase.
2. Open primary content exploration.
3. Confirm zero miller-column Finder chrome; no production fallback toggle/URL to classic Finder.
4. Sign off [cutover-inventory.md](./checklists/cutover-inventory.md) rows for primary nav.

**Sign-off reviewers (per `cutover-inventory.md` §E)**: at minimum, **two roles** must sign each row:
- Engineering owner of the touched module (e.g. `WebUI` lead for Finder primary-nav; sitemanage lead for path API gaps).
- QA/UAT owner (runs Scenario A/B/C against the candidate build).
- Optional: release manager (signs release-train readiness).

Evidence per row = sign-off name + date + linked PR / commit hash + Scenario result.

## Scenario D — Desktop CE retirement (SC-007)

1. Same core-navigate bar as Scenario A.
2. Complete ordinary content admin without launching Desktop CE.
3. Docs/distribution no longer require CE for those workflows.

## Scenario E — Content browser host (SC-002 / US2)

1. Open pilot host dialog that mounts `ContentBrowser`.
2. Navigate or search; select allowed item; confirm.
3. Verify host receives `SelectionResult` (id/path).
4. Try disallowed type; confirm blocked.
5. If multi-select host: select multiple; confirm set.

## Scenario F — ACL (SC-004 / US4) — post-cutover

1. Admin: open folder properties/security; change ACL; save.
2. Second user session: refresh; verify access change.
3. Attempt self-lockout change; expect warning.

## Scenario G — Menus (SC-003 / US3) — post-cutover

1. Authorized user: context menu on folder and on item.
2. Execute checklist of ≥10 high-value actions (matrix); all succeed.

## Scenario H — Advanced matrix (SC-011 / US7)

1. Review capability matrix P-Adv rows.
2. For each in-scope row, run row-specific UAT until **Done** (post-8.2 “scheduled” is not allowed).

## Scenario I — 8.2 release gate (SC-012)

1. Confirm Scenarios A–H pass on the 8.2 candidate build.
2. Confirm capability matrix in-scope rows are all Done.
3. Confirm no production classic Finder/CE fallback for in-scope surfaces.
4. **Do not** label or ship as 8.2 GA if any SC-012 clause fails.

## Accessibility spot-check (SC-009)

- Keyboard: tree expand/select, list focus, reduced actions or context menu, browser dialog confirm/cancel.

## Usability survey (SC-010)

**Rubric**: each of ≥5 internal users familiar with both UIs rates **five** daily folder-navigation tasks on a 1–5 Likert scale for **modern explorer** vs **miller-column Finder**. Tasks:
1. Find a folder by name.
2. Open a specific item from a known parent.
3. Create a new folder and rename it.
4. Move an item between folders.
5. Recover from a permission-denied state.

**Pass**: majority of raters (≥3 of 5) score modern explorer **higher** than miller-column Finder on the sum across tasks. Recorded in `checklists/sc010-usability-notes.md` (created in `tasks.md` T084) with raw scores per rater.

## i18n spot-check

- Non-default locale session: primary chrome resolves via TMX (not English-only hardcode).

## Troubleshooting

| Symptom | Check |
|---------|--------|
| 404 on `/Rhythmyx/services/...` | Use `detectServicesRoot()` / context path (`api/paths.ts`) |
| CSRF failures | Token header on POST; shell includes CSRF like other modern pages |
| Empty tree | User community/permissions; path roots; network tab path API |
| Hard cut still shows Finder | `webmgt.jsp` includes and nav wiring; cutover inventory |
