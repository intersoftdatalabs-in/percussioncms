# Quickstart validation: Unified React Content Explorer

**Feature**: `992-react-content-explorer`  
**Purpose**: Validation scenarios for implementers and the Playwright E2E suite. **Primary acceptance is automated via Playwright** (`modules/perc-qa-automation/`); this document is the manual fallback when the docker runtime is unavailable and the per-row evidence checklist for SC-001..SC-011 sign-off.

## Prerequisites

- Branch `992-react-content-explorer` (or merge target with this feature).
- **Product release target: 8.2** — full functional parity (SC-012) is required before labeling/shipping 8.2 GA.
- JDK 21 via `./mvn-env.sh` / `./mvn-env.bat`.
- **Docker dev runtime operational** (see [Docker dev runtime](#docker-dev-runtime) below). The Playwright suite runs against this CMS.
- Node/npm 22+ for Playwright (`modules/perc-qa-automation/frontend/package.json`).
- Contracts: [path-api.md](./contracts/path-api.md), [content-browser-host.md](./contracts/content-browser-host.md), [action-menu-api.md](./contracts/action-menu-api.md), [capability-matrix.md](./contracts/capability-matrix.md).

## Docker dev runtime

A `docker-compose.yml` stack brings up the `cms-dts` container against a host-side install at `/opt/Percussion`. Install is run once on host; container is service-only. Default DB is **Derby** (dev/test); MySQL mode is deferred per [issue #1388](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388).

```bash
# 1. Configure (one-time)
cp .env.compose.example .env.compose            # edit secrets if needed (defaults work for local dev)

# 2. Host-side install (one-time per branch; idempotent via marker file)
./scripts/install-cms-dev.py --skip-dts         # CMS-only install (DTS is out of dev scope)

# 3. Bring up the container
docker compose --env-file .env.compose -f docker-compose.yml up -d cms-dts

# 4. Verify
./docker/scripts/perc-devctl.py verify           # polls login + DTS endpoints, 30 s start_period + 60 retries

# 5. Admin creds live at /opt/Percussion/var/config/generated/passwords:
#    Admin=<pw>, Editor=<pw>, Contributor=<pw>
```

The CMS is reachable at `http://localhost:9992/Rhythmyx/login` with Basic auth via the `RX_USEBASICAUTH: true` header (matches the 8.2 CMS REST auth contract).

## Build / unit tests (dev)

```bash
# Vitest — component-level tests with mocked API
./mvn-env.sh -pl WebUI -am test
cd WebUI && npx vitest run src/test/ts/contentExplorer src/test/ts/contentBrowser

# Playwright — E2E against the live CMS (requires docker runtime)
cd modules/perc-qa-automation/frontend
npm ci
npx playwright install chromium
npm test                                            # runs all tests via Playwright Test Runner
```

**Expected**: Vitest + Playwright both green for implemented phases. Playwright specs marked `test.skip` (with `BUG:` note) document upstream REST bugs (e.g. [issue #1387](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387)); flipping `test.skip` → `test(...)` is the SC-008 evidence when the fix lands.

## Scenario A — Core navigate (P0-Core / SC-001)

**Automated via**: `tests/us1-core-explorer.spec.js` (Playwright).

1. Sign in as content user.
2. Open Web Management content exploration (editor/explorer entry after rewire).
3. Confirm **tree + detail list** (not miller columns as primary).
4. Expand folders; select folder; verify children list.
5. Open a page/asset; verify editor/preview path works.
6. Create folder; rename; move or copy; delete with confirm.
7. Force permission error or session timeout (or mock); verify clear message.

**Pass**: 100% of steps without miller-column Finder or Desktop CE.

## Scenario B — Large folder (SC-005)

**Automated via**: `tests/us1-perf-sc005.spec.js` (Playwright) + Vitest perf regression guard `WebUI/src/test/ts/contentExplorer/sc005-perf-regression.test.ts`.

**Fixture**:
- Single folder with **≥500 children** (mixed page/asset type). Create via `scripts/create-large-folder-fixture.sh` (Derby default).

**Steps**:
1. Sign in; navigate to fixture folder via modern explorer.
2. Select folder → list loads first page.
3. Scroll/page list.
4. Open one item.

**Pass criterion (SC-005)**:
- **p95** time from "select folder" → "selected item open in editor" **≤ 10 seconds** on a standard office network.
- Recorded in `checklists/sc005-perf-evidence.md`: run name, git SHA, fixture size, network profile, p50/p95/max times, pass/fail.
- Vitest perf regression guard: mocked `paginatedFolder` for a 500-child fixture renders first page and opens a selected item within a **tighter dev-machine budget (≤ 5 s)** — catches regressions pre-CI.

## Scenario C — Finder hard cut (SC-006)

**Automated via**: `tests/us6-hard-cut.spec.js` (Playwright).

1. Production-like build after US6 Finder phase.
2. Open primary content exploration.
3. Confirm zero miller-column Finder chrome; no production fallback toggle/URL to classic Finder.
4. Sign off [cutover-inventory.md](./checklists/cutover-inventory.md) rows for primary nav.

**Sign-off reviewers (per `cutover-inventory.md` §E)**: at minimum, **two roles** must sign each row:
- Engineering owner of the touched module (e.g. `WebUI` lead for Finder primary-nav; sitemanage lead for path API gaps).
- QA/UAT owner (runs Scenario A/B/C against the candidate build; **in this feature, automated via Playwright**).
- Optional: release manager (signs release-train readiness).

Evidence per row = sign-off name + date + linked PR / commit hash + Scenario result (Playwright report).

## Scenario D — Desktop CE retirement (SC-007)

**Automated via**: `tests/us6-hard-cut.spec.js` Playwright check asserts the modern web explorer is sufficient for ordinary admin.

1. Same core-navigate bar as Scenario A.
2. Complete ordinary content admin without launching Desktop CE.
3. Docs/distribution no longer require CE for those workflows.

## Scenario E — Content browser host (SC-002 / US2)

**Automated via**: `tests/host-asset-picker.spec.js`, `tests/host-page-picker.spec.js`, `tests/host-aa-contentbrowser-dialog.spec.js`, `tests/host-folder-picker.spec.js` (Playwright; one per in-scope host).

1. Open pilot host dialog that mounts `ContentBrowser`.
2. Navigate or search; select allowed item; confirm.
3. Verify host receives `SelectionResult` (id/path).
4. Try disallowed type; confirm blocked.
5. If multi-select host: select multiple; confirm set.

## Scenario F — ACL (SC-004 / US4) — post-cutover

**Automated via**: `tests/us4-acl.spec.js` (Playwright; second browser context simulates the second user session).

1. Admin: open folder properties/security; change ACL; save.
2. Second user session: refresh; verify access change.
3. Attempt self-lockout change; expect warning.

## Scenario G — Menus (SC-003 / US3) — post-cutover

**Automated via**: `tests/us3-menus.spec.js` (Playwright; one `test()` per action in the ≥10-action enumeration).

1. Authorized user: context menu on folder and on item.
2. Execute checklist of ≥10 high-value actions (matrix); all succeed.

## Scenario H — Advanced matrix (SC-011 / US7)

**Automated via**: `tests/us7-advanced.spec.js` (Playwright; one `test()` per P-Adv row).

1. Review capability matrix P-Adv rows.
2. For each in-scope row, run row-specific Playwright spec until **Done** (post-8.2 “scheduled” is not allowed).

## Scenario I — 8.2 release gate (SC-012)

**Automated via**: full Playwright suite pass + Vitest green + axe-core a11y gate. Manual aggregation in `docs/ai-generated/release/992-8.2-parity-evidence.md`.

1. Confirm Scenarios A–H pass on the 8.2 candidate build.
2. Confirm capability matrix in-scope rows are all Done.
3. Confirm no production classic Finder/CE fallback for in-scope surfaces.
4. **Do not** label or ship as 8.2 GA if any SC-012 clause fails.

## Accessibility (SC-009)

**Automated via axe-core** (T082b): injected into every Playwright spec; fails CI on serious/critical violations.

**Manual supplement**: keyboard-only navigation of the modern explorer for surface-only flows that Playwright can't drive (e.g. embedded iframe scenarios, native dialogs).

## Usability survey (SC-010) — intentionally manual

**Rubric**: each of ≥5 internal users familiar with both UIs rates **five** daily folder-navigation tasks on a 1–5 Likert scale for **modern explorer** vs **miller-column Finder**. Tasks:
1. Find a folder by name.
2. Open a specific item from a known parent.
3. Create a new folder and rename it.
4. Move an item between folders.
5. Recover from a permission-denied state.

**Pass**: majority of raters (≥3 of 5) score modern explorer **higher** than miller-column Finder on the sum across tasks. Recorded in `checklists/sc010-usability-notes.md` (T084) with raw scores per rater.

**SC-010 is the one criterion that is intentionally manual** — quantitative scores from internal users cannot be substituted by automated tests.

## i18n spot-check

**Automated via**: Playwright spec loads the explorer in non-default locale (es), asserts chrome resolves via TMX.

## Troubleshooting

| Symptom | Check |
|---------|--------|
| 404 on `/Rhythmyx/services/...` | Use `detectServicesRoot()` / context path (`api/paths.ts`) |
| CSRF failures | Token header on POST; shell includes CSRF like other modern pages |
| Empty tree | User community/permissions; path roots; network tab path API |
| Hard cut still shows Finder | `webmgt.jsp` includes and nav wiring; cutover inventory |
| Playwright `Module not found: playwright` | `cd modules/perc-qa-automation/frontend && npm ci` |
| Playwright timeout on login | Verify docker CMS is up: `docker compose ps`. Verify `/opt/Percussion/var/config/generated/passwords` exists with `Admin=...` line. |
| 500 on `/rest/folders/by-path/...` | Known upstream bug [issue #1387](https://github.com/intersoftdatalabs-in/percussioncms/issues/1387). Mark with `test.skip` + `BUG:` note; flip when fix lands. |
