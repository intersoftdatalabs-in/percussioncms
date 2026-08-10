# perc-qa-automation

This module provides QA automation testing capabilities for Percussion CMS using Playwright.

## Purpose

The `perc-qa-automation` module is used for authoring and executing automated browser-based tests against a running Percussion CMS instance. It enables QA engineers and developers to write end-to-end tests that verify the functionality of the CMS.

## Prerequisites

- A running Percussion CMS instance
- Maven 3.x
- Java 8+

## Configuration

Two modes share the same helpers (`frontend/tests/helpers/auth.js` +
`resolve-cms-env.js`). Full product rules:
[workbench-rest-and-qa-modes.md](../../docs/developer-module/workbench-rest-and-qa-modes.md).

|              Mode              |                           CMS target                            |            Host install?            |
|--------------------------------|-----------------------------------------------------------------|-------------------------------------|
| **QA mode** (agents / gate)    | `TEST_CMS_URL` from `perc-devctl qa-up` (or freeport host port) | **No**                              |
| **Dev mode** (human fast loop) | Local install + `DEV_PERCUSSION_*` auto-discovery               | Yes (optional if URL/passwords set) |

### Quick Start — QA mode (no host install)

```bash
# From repo root — bring up H2 Docker cell (prints TEST_CMS_URL + host port)
python docker/scripts/perc-devctl.py qa-up

# Playwright against the stack only — do not set DEV_PERCUSSION_INSTALL
# Prefer the printed TEST_CMS_URL (do not hardcode :9993 — freeport #2005/#2014).
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm test -- tests/install.spec.js

# Tear down when done
python docker/scripts/perc-devctl.py qa-down
```

### Golden unattended smoke (#2065 / #1928 slice B)

**One** reference Playwright path for agents: Admin **login + modern Content Explorer**
(product screen). Env-only (`TEST_CMS_URL` + admin creds); **no** `DEV_PERCUSSION_INSTALL`.
Not the full suite.

**Prereq (once / after installer changes):** package the customer CMS assembly:

```bash
cd modules/perc-distribution-tree && ../../mvnw package -DskipTests
# Windows: cd modules\perc-distribution-tree && ..\..\mvnw.cmd package -DskipTests
```

**Unix (bash) one-shot** from repo root:

```bash
python docker/scripts/perc-devctl.py qa-up
# Capture TEST_CMS_URL=… QA_CMS_HOST_PORT=… ADMIN_PASSWORD=… from stdout (freeport).

cd modules/perc-qa-automation/frontend
npm ci
npx playwright install chromium

export TEST_CMS_URL="${TEST_CMS_URL:-http://127.0.0.1:${QA_CMS_HOST_PORT}}"
export ADMIN_USERNAME=Admin
export ADMIN_PASSWORD   # set from qa-up stdout — never commit
export TEST_DB_TYPE=h2
export TEST_PRODUCT=cms

npm run test:golden
# equivalent:
# npm run test:surface -- --path tests/golden-unattended-smoke.spec.js
# npx playwright test tests/golden-unattended-smoke.spec.js --grep @golden

# Optional extended set (#2490): golden + @folder-recycle multi-path (not full suite)
# npm run test:golden-extended
# npm run test:golden-extended:list   # no live CMS

cd ../../..
python docker/scripts/perc-devctl.py qa-down
```

**Windows (cmd) one-shot** from repo root:

```bat
python docker\scripts\perc-devctl.py qa-up
REM Capture TEST_CMS_URL / QA_CMS_HOST_PORT / ADMIN_PASSWORD from stdout (do not hardcode 9993)

cd modules\perc-qa-automation\frontend
call npm ci
call npx playwright install chromium

set TEST_CMS_URL=http://127.0.0.1:%QA_CMS_HOST_PORT%
set ADMIN_USERNAME=Admin
set ADMIN_PASSWORD=<from-qa-up>
set TEST_DB_TYPE=h2
set TEST_PRODUCT=cms

call npm run test:golden
REM Optional extended: call npm run test:golden-extended

cd ..\..\..
python docker\scripts\perc-devctl.py qa-down
```

| Item | Value |
|------|--------|
| Spec (baseline) | `frontend/tests/golden-unattended-smoke.spec.js` |
| npm (baseline) | `npm run test:golden` |
| Tags (baseline) | `@smoke` / `@golden` (also via `npm run test:surface -- --tag golden`) |
| Specs (extended) | baseline + `folder-recycle-smoke.spec.js` (`@folder-recycle`) + `profile-shell.spec.js` (`@profile`) |
| npm (extended) | `npm run test:golden-extended` / `npm run test:golden-extended:list` |
| Inventory helper | `frontend/tests/helpers/golden-unattended-smoke-set.js` (#2490 / #2498) |
| Failure artifacts | `modules/perc-qa-automation/frontend/test-results/` |
| HTML report | `modules/perc-qa-automation/frontend/playwright-report/` |
| Attach runbook | [playwright-failure-artifacts.md](../../docs/developer-module/playwright-failure-artifacts.md) |
| Stack lifecycle | [workbench-rest-and-qa-modes.md](../../docs/developer-module/workbench-rest-and-qa-modes.md) §2 |

#### Extended golden multi-path (`@folder-recycle` + `@profile`)

**Decision: include** `@folder-recycle` (#2490 / parent #2423) and `@profile`
(#2498 / parent #2374) in the **optional extended** golden/unattended multi-path
set — not in the minimal default and **not** the full Playwright suite.

| Tier | npm | Specs | When to use |
|------|-----|-------|-------------|
| **Baseline** (default) | `npm run test:golden` | `golden-unattended-smoke.spec.js` only | Fastest unattended login + Explorer gate |
| **Extended** | `npm run test:golden-extended` | baseline + `folder-recycle-smoke.spec.js` + `profile-shell.spec.js` | Overnight recycle REST + profile hub entry without full suite |

```bash
# List only (no live CMS) — proves surface wiring
cd modules/perc-qa-automation/frontend
npm run test:golden-extended:list
# or surface-filter form:
npm run test:surface:list -- --path tests/folder-recycle-smoke.spec.js
npm run test:surface:list -- --tag folder-recycle
npm run test:surface:list -- --path tests/profile-shell.spec.js
npm run test:surface:list -- --tag profile

# Live after qa-up
TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… npm run test:golden-extended
```

Canonical inventory (unit-tested lockstep with package.json):
`frontend/tests/helpers/golden-unattended-smoke-set.js`. Live H2 proof residual
for folder-recycle: #2488. Finder UI companion is separate
(#2489 / `@finder-recycle-restore`). Profile path-only residual was #2425;
this entry keeps `@profile` on the overnight extended matrix (#2498).

**Hard bans:** do not commit passwords; do not hardcode host port `:9993` as the only URL
(use freeport `TEST_CMS_URL` from `qa-up`). Do **not** replace `test:golden` with the
full suite; use path/tag/`test:golden-extended` only.

#### Stale matrix image — fail-fast on `--skip-image-build` (#2484)

`perc-devctl qa-up` waits on Docker `Health.Status=healthy` for the matrix
cell (#2481). When `qa-up --skip-image-build` reuses a cached
`percussion-matrix-cell:local` image whose `HEALTHCHECK` block is absent
(a pre-#2481 bake, or one built without the `rhythmyx_healthcheck.py`
script), the smoke would otherwise spin the full
`--probe-timeout` window (default 900 s) and then report a confusing
`docker_health_timeout health=none`.

The Python entrypoint now detects that condition up front: when
`--skip-image-build` is set, `qa-up` inspects the local image and emits
`RESULT:FAIL STEP:qa-up DETAIL:matrix_image_stale` with a one-line rebuild
hint before waiting on the smoke. Operators and agents should:

1. Drop `--skip-image-build` (default) — the smoke will rebuild the image
   from `docker/matrix/Dockerfile` automatically.
2. Or rebuild the matrix image directly (faster when only the
   `docker/scripts/*` health scripts changed):
   `docker build -t percussion-matrix-cell:local -f docker/matrix/Dockerfile docker/`
   (Windows: same command via PowerShell or `cmd /c`).

Full context: `docker/README.md` → *Docker `Health.Status` (in-image
HEALTHCHECK)* and `docs/developer-module/workbench-rest-and-qa-modes.md`
§ *H2 Docker one-shot*.

### Demo-sites Sample Site residual (#1750 / #2194)

Regression coverage that **Corporate Investments** and **Enterprise Investments**
appear under **Sites** after a demo-sites install (REST `path/folder/Sites` + Explorer UI).
Peers of `tests/bugs/bug-1622-explorer-root-folders.spec.js`.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/bugs/bug-1750-demo-sites-sample-site.spec.js` |
| Helpers / unit | `frontend/tests/helpers/demo-sites.js`, `npm run test:unit` |
| Product fix | Installer seed flag propagation (#2192) must be in the image under test |
| Soft skip | Without sample data, tests `test.skip` with `BUG:` + issue URL (skip-with-BUG) |
| Hard fail | Set `EXPECT_DEMO_SITES=1` (alias `TEST_EXPECT_DEMO_SITES`) so empty Sites fails |

```bash
# After #2192 is in the installer/image: silent H2 with sample sites, then CMS up.
# java -jar <installer>.jar --install-dir=<path> --silent --db.type=h2 --demo-sites

cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-install> \
  EXPECT_DEMO_SITES=1 \
  npm test -- tests/bugs/bug-1750-demo-sites-sample-site.spec.js

# Default qa-up without demo-sites: soft skip when Sites empty (not a suite red)
TEST_CMS_URL=… ADMIN_PASSWORD=… npm test -- tests/bugs/bug-1750-demo-sites-sample-site.spec.js
```

Evidence / install flags: `docs/ai-generated/issue-2191-demo-sites-empty-sites-repro.md`,
module `perc-distribution-tree` AGENTS.md § Installing Sample Sites.

### Developer entry + critical catalogs smoke gate (#2188 / epic #2089)

Inventory of Developer SPA entry + critical catalog specs that must be **green** or
**skip-with-BUG** (durable issue URL) on H2 qa-up. Canonical list:
`frontend/tests/helpers/developer-smoke-set.js`. Doc:
[playwright-smoke-gate-2188.md](../../docs/developer-module/playwright-smoke-gate-2188.md).

```bash
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… npm run test:developer-smoke
# list only (no CMS): npm run test:developer-smoke:list
```

| Item | Value |
|------|--------|
| Specs | golden + login + `developer-catalog-smoke` + `developer-template-source-viewer` |
| npm | `npm run test:developer-smoke` |
| Tags | `@smoke` (residuals use `test.skip` + `BUG:` + issue URL) |
| Open residuals | #2186 content-types selectors; #2189 template list DTO |

**Admin creds (QA):** default username `Admin` (`ADMIN_USERNAME`). Password comes
from `qa-up` output, process env, or `docker exec` into the QA cell — **never
commit secrets**. No passwords are stored in this repo.

**URL resolution precedence** (highest first; pure helper + unit tests):

1. `TEST_CMS_URL` (aliases: `CMS_BASE_URL`, `QA_CMS_URL`)
2. `http://127.0.0.1:<port>` from `QA_CMS_HOST_PORT` or `CMS_HOST_PORT`
3. `DEV_PERCUSSION_URL`
4. Discover from `DEV_PERCUSSION_INSTALL` (dev mode only)
5. Documented fallback `http://localhost:9992` (dev default; install matrix
   specs may fall back to preferred QA pin `http://localhost:9993` when free)

### Quick Start — Dev mode (human)

1. Copy the example environment file:

   ```bash
   cp .env.example .env
   ```
2. Edit `.env` and set `DEV_PERCUSSION_INSTALL` to your local CMS installation path:

   ```
   DEV_PERCUSSION_INSTALL=/path/to/your/cms-install
   ```
3. That's it! The module will auto-discover:
   - **CMS URL** - from `jetty/base/etc/installation.properties`
   - **DTS URL** - from `Deployment/Server/conf/perc/perc-catalina.properties`
   - **User passwords** - from `var/config/generated/passwords`

### Environment Variables

|               Variable               |     Required     |                    Description                     |
|--------------------------------------|------------------|----------------------------------------------------|
| `TEST_CMS_URL`                       | QA mode          | CMS base URL from `qa-up` / matrix pin (preferred) |
| `CMS_BASE_URL` / `QA_CMS_URL`        | No               | Documented aliases for `TEST_CMS_URL`              |
| `QA_CMS_HOST_PORT` / `CMS_HOST_PORT` | No               | Freeport host port when URL not set (#2005)        |
| `DEV_PERCUSSION_INSTALL`             | Dev mode         | Path to local CMS installation                     |
| `DEV_PERCUSSION_URL`                 | No               | CMS URL (auto-calculated from installation)        |
| `DEV_PERCUSSION_DTS_INSTALL`         | No               | Path to DTS installation (if separate from CMS)    |
| `DEV_PERCUSSION_DTS_URL`             | No               | DTS URL (auto-calculated from DTS installation)    |
| `ADMIN_USERNAME`                     | No               | Admin username (default: `Admin`)                  |
| `ADMIN_PASSWORD`                     | QA if no install | Admin password (env / qa-up / install discovery)   |
| `EDITOR_USERNAME`                    | No               | Editor username (default: `Editor`)                |
| `EDITOR_PASSWORD`                    | No               | Editor password (auto-read from installation)      |
| `CONTRIBUTOR_USERNAME`               | No               | Contributor username (default: `Contributor`)      |
| `CONTRIBUTOR_PASSWORD`               | No               | Contributor password (auto-read from installation) |

### Auto-Configuration (dev mode)

On first run with `DEV_PERCUSSION_INSTALL`, the test helpers will automatically:

1. Read the CMS port from `jetty/base/etc/installation.properties` → `jetty.http.port`
2. Read the DTS port from `{DEV_PERCUSSION_DTS_INSTALL}/Deployment/Server/conf/perc/perc-catalina.properties` → `http.port`
3. Read all user passwords from `var/config/generated/passwords`
4. Save these values to `.env` for faster subsequent runs

QA mode skips install discovery when `TEST_CMS_URL` (or host port env) is set.

### Failure artifacts

On failure, Playwright writes under `frontend/` (paths relative to that directory):

|               Artifact               |     Default path     |
|--------------------------------------|----------------------|
| Screenshots / traces / error context | `test-results/`      |
| HTML report                          | `playwright-report/` |

How agents attach these to PRs/issues: see
[playwright-failure-artifacts.md](../../docs/developer-module/playwright-failure-artifacts.md)
(#2066) when present; otherwise collect the paths above and upload as PR comment
attachments or gist links.

### Optional CI (GitHub Actions) — #1930

Non-required workflow: [`.github/workflows/h2-qa-playwright.yml`](../../.github/workflows/h2-qa-playwright.yml)
(**H2 QA Playwright (optional)**).

| Mode | When | Runs |
|------|------|------|
| **dry-run** / path-filtered PR | Default dispatch + QA-related PR paths | Freeport + `qa-*` dry-run + `test:unit` + surface list (no live CMS) |
| **live** | Manual `workflow_dispatch` only | Package → `perc-devctl qa-up` → surface Playwright → upload `test-results/` + `playwright-report/` → `qa-down` |

- Trigger / env / artifact download / Windows-local parity:
  [workbench-rest-and-qa-modes.md](../../docs/developer-module/workbench-rest-and-qa-modes.md)
  → **Optional CI**.
- Do **not** make this workflow a required branch check without a product decision.
- Secrets: optional repo secret `QA_ADMIN_PASSWORD`; prefer password emitted by `qa-up` (masked). Never commit passwords.

## Building

To build the module and install Node.js dependencies:

```bash
mvn clean install
```

This will:
1. Install Node.js and npm via frontend-maven-plugin
2. Run `npm ci` to install Playwright dependencies
3. Compile the module

## Running Tests

**Always run from `modules/perc-qa-automation/frontend`** — Playwright resolves `playwright.config.js` relative to the current working directory. Running from the parent module dir or elsewhere will report "No tests found" because `testDir: './tests'` resolves incorrectly.

### First-time setup

```bash
cd modules/perc-qa-automation/frontend
npm ci                                              # install @playwright/test + dotenv
npx playwright install chromium                    # one-time browser download
# Optional: copy .env.example to .env (auto-discovered from
# DEV_PERCUSSION_INSTALL on first run if missing)
```

### Unit / config tests (no live CMS)

```bash
cd modules/perc-qa-automation/frontend
npm run test:unit                                   # node:test for URL/creds precedence
```

### Full suite

```bash
cd modules/perc-qa-automation/frontend
npm test                                            # alias for `npx playwright test`
```

### Specific test file

```bash
cd modules/perc-qa-automation/frontend
npx playwright test tests/login.spec.js
```

### Surface filter (PR / agent subset) — path, grep, tag

Unattended and PR-focused runs should **not** default to the full suite. Use native
Playwright filters for the **surface under test**. Helper + npm scripts wrap the same
CLI (no custom test runner).

|           Filter            |                Native Playwright                 |                        npm / helper                        |
|-----------------------------|--------------------------------------------------|------------------------------------------------------------|
| **Path**                    | `npx playwright test tests/login.spec.js`        | `npm run test:surface -- --path tests/login.spec.js`       |
| **Grep (title)**            | `npx playwright test --grep "Admin login"`       | `npm run test:surface -- --grep "Admin login"`             |
| **Tag**                     | `npx playwright test --grep @smoke`              | `npm run test:surface -- --tag smoke`                      |
| **List only** (no live CMS) | `npx playwright test --list tests/login.spec.js` | `npm run test:surface:list -- --path tests/login.spec.js`  |
| **Print command**           | —                                                | `npm run test:surface:print -- --path tests/login.spec.js` |

```bash
cd modules/perc-qa-automation/frontend

# Unit tests for the surface-filter arg builder (no live CMS, no Docker)
npm run test:unit

# List matches only — safe without a CMS
npm run test:surface:list -- --path tests/login.spec.js
npm run test:surface:list -- --grep "Content Explorer"
npm run test:surface:print -- --tag smoke

# Env form (same filters; useful for agents / CI)
SURFACE_PATH=tests/login.spec.js npm run test:surface:list
SURFACE_PATHS=tests/login.spec.js,tests/logout.spec.js npm run test:surface:print
SURFACE_GREP="Admin login" npm run test:surface:list
SURFACE_TAG=smoke npm run test:surface:print

# Live QA mode against H2 Docker (slices 1–2 entrypoint — no host install)
# Prefer TEST_CMS_URL from perc-devctl qa-up (do not hardcode :9993 — freeport #2005/#2014).
# python docker/scripts/perc-devctl.py qa-up   # from repo root
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/login.spec.js

# Tear down when done
# python docker/scripts/perc-devctl.py qa-down
```

#### Home gadget locale residual (#1876 / parent #1852)

Optional Playwright bug-regression for residual `perc.ui.dashboard.modern@` /
`welcome@` / `activity@` body and modal keys after a non-English login
(prefer `de-de`/`de`, else `hi-in`/`hi`, else `es`). Asserts Add Gadget chrome
and a sample of Welcome/Activity strings are **not** English fallback.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/bugs/bug-1876-home-gadget-locale.spec.js` |
| Tags | `@locale` `@home` `@dashboard` |
| Unit (no CMS) | `npm run test:unit` (includes `pick-locale-tag.test.js`) |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/bugs/bug-1876-home-gadget-locale.spec.js

# List only (no live CMS)
npm run test:surface:list -- --path tests/bugs/bug-1876-home-gadget-locale.spec.js
npm run test:surface:list -- --tag locale
```

Does **not** expand Spanish/#961 residual matrix scope (tracked separately).

#### Folder + recycle REST smoke (#2464 / parent #2423 / #2490)

Surface-filtered regression after the `folderHelper` → `recycleService` Spring
cycle break. Proves pathmanagement is up (hard fail if Rhythmyx context is dead),
then create folder under Assets → soft-delete (recycle) → restore by guid when
available, else purge via empty Recycling. Optional Admin login check when
context is healthy.

**Golden/unattended placement (#2490):** included in **`npm run test:golden-extended`**
(multi-path with baseline golden). **Not** in default `npm run test:golden`.
Still available as a standalone surface path/tag below.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/folder-recycle-smoke.spec.js` |
| Tags | `@folder-recycle` `@smoke` |
| Unit (no CMS) | `npm run test:unit` (includes `folder-recycle-smoke.test.js`, `golden-unattended-smoke-set.test.js`) |
| Helper | `frontend/tests/helpers/folder-recycle-smoke.js` |
| Extended golden | `npm run test:golden-extended` / `test:golden-extended:list` |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/folder-recycle-smoke.spec.js

# Tag form
npm run test:surface -- --tag folder-recycle

# Extended golden multi-path (baseline + this surface) — #2490
npm run test:golden-extended
npm run test:golden-extended:list

# List only (no live CMS)
npm run test:surface:list -- --path tests/folder-recycle-smoke.spec.js
npm run test:surface:list -- --tag folder-recycle
```

**Hard fail contract:** if pathmanagement returns connection error / 5xx (or body
mentions `BeanCurrentlyInCreationException` / `folderHelper`), the smoke fails
with an explicit message citing #2464 / #2423 — do not treat as soft skip.

#### Classic Finder UI recycle / restore companion (#2489 / residual #2541 / parent #2423)

Surface-filtered UI companion for the folder+recycle REST smoke (#2464). Exercises
classic Finder chrome: soft-delete (recycle) a seeded Assets folder, then
**restore** via `#perc-finder-restore-item` when path/selection allows, else
**Empty Recycling** via Actions menu (`data-testid="perc-finder-empty-recycling"`,
#2207 peer). Hard fails when pathmanagement context or Admin login is down.

**#2541 selection reliability:** happy-path recycle uses ordered strategies so
`#perc-finder-delete` enables without REST soft-delete fallback:

1. Path-bar navigate to `/Assets/{name}` (fires path_changed depth &gt; 2)
2. Click `#perc-finder-listing-{guid}` when seed guid is known
3. Miller column: `.mcol-listing[title=…]` / `.perc-finder-item-name` parent
4. List view: `.perc-datatable-row` exact name

REST `deleteFolder` remains only as last-resort residual-shell recovery (annotated
warning). Residual product chrome gaps: no `data-testid` on miller listings or
list rows; delete/restore enablement is class-based (`ui-enabled` /
`ui-disabled`) only — Empty Recycling is the main `data-testid` control.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/finder-recycle-restore-ui.spec.js` |
| Tags | `@finder-recycle-restore` `@folder-recycle` `@smoke` |
| Unit (no CMS) | `npm run test:unit` (includes `finder-recycle-restore-ui.test.js`) |
| Helper | `frontend/tests/helpers/finder-recycle-restore-ui.js` |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/finder-recycle-restore-ui.spec.js

# Tag form
npm run test:surface -- --tag finder-recycle-restore

# List only (no live CMS)
npm run test:surface:list -- --path tests/finder-recycle-restore-ui.spec.js
npm run test:surface:list -- --tag finder-recycle-restore
```

**Hard fail contract:** pathmanagement probe uses the same context-down message
class as #2464; Admin login that remains on `/Rhythmyx/login` fails with an
explicit #2489 / #2423 message — do not soft-skip.

#### Modern Content Explorer UI recycle / restore companion (#2542 / parent #2423)

Surface-filtered UI companion for the folder+recycle REST smoke (#2464), peer of
classic Finder #2489. Exercises modern React explorer chrome
(`data-testid="content-explorer-shell"`, tree, detail list, reduced-actions
`action-delete`): soft-delete (recycle) a seeded Assets folder, then **restore**
when a server/context-menu restore action is present, else **empty Recycling**
via REST cleanup (classic Finder remains the UI empty peer). Hard fails when
pathmanagement context or Admin login is down. Does **not** replace #2489.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/explorer-recycle-restore-ui.spec.js` |
| Tags | `@explorer-recycle-restore` `@folder-recycle` `@smoke` |
| Unit (no CMS) | `npm run test:unit` (includes `explorer-recycle-restore-ui.test.js`) |
| Helper | `frontend/tests/helpers/explorer-recycle-restore-ui.js` |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/explorer-recycle-restore-ui.spec.js

# Tag form
npm run test:surface -- --tag explorer-recycle-restore

# List only (no live CMS)
npm run test:surface:list -- --path tests/explorer-recycle-restore-ui.spec.js
npm run test:surface:list -- --tag explorer-recycle-restore
```

**Hard fail contract:** pathmanagement probe uses the same context-down message
class as #2464; Admin login that remains on `/Rhythmyx/login` fails with an
explicit #2542 / #2423 message — do not soft-skip.

#### Explorer translations / P-Trans UI (#2430 / parent #2411)

Surface-filtered companion for content-item locale variants + create-variant in
the modern React Content Explorer. Consumes public REST
`GET|POST /rest/content-explorer/translations` (slice B / PR #2601). Asserts
shell toggle chrome, select-item hint, and optional panel load when a content
row is selected. In-flight queue is product OUT (panel note only).

| Item | Value |
|------|--------|
| Spec | `frontend/tests/explorer-translations.spec.js` |
| Tags | `@explorer-translations` `@p-trans` `@smoke` |
| Vitest peer | `WebUI` `translationsApi` / `TranslationsPanel` / shell tests |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/explorer-translations.spec.js

# Tag form
npm run test:surface -- --tag explorer-translations

# List only (no live CMS)
npm run test:surface:list -- --path tests/explorer-translations.spec.js
npm run test:surface:list -- --tag explorer-translations
```

#### Explorer IA relationships (#2769 / parent #2400)

Surface-filtered companion for mounting `RelationshipsView` from product
Explorer chrome (View → IA Relationships). Consumes public REST
`GET /rest/content-explorer/relationships/{itemId}/summary`. Asserts shell
toggle chrome, select-item hint, and optional panel load (4 primary IA rows)
when a list row with an id is selected. Dependency viewer is a sibling slice
(#2768) — not this surface.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/explorer-relationships.spec.js` |
| Tags | `@explorer-relationships` `@p-adv` `@smoke` |
| Vitest peer | `WebUI` `RelationshipsView` / shell / `ExplorerMenuBar` / `menuBarModel` |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/explorer-relationships.spec.js

# Tag form
npm run test:surface -- --tag explorer-relationships

# List only (no live CMS)
npm run test:surface:list -- --path tests/explorer-relationships.spec.js
npm run test:surface:list -- --tag explorer-relationships
```

#### Explorer dependency viewer (#2768 / parent #2400)

View → Dependencies shell chrome + optional DependencyViewer mount for a
selected content item (reuses relationship summary REST). Soft-skip deep
relationship count assertions when the QA fixture has no selectable row.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/explorer-dependencies.spec.js` |
| Tags | `@explorer-dependencies` `@p-adv` `@smoke` |
| Vitest peer | `WebUI` `DependencyViewer` / shell / `ExplorerMenuBar` / `menuBarModel` |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/explorer-dependencies.spec.js

# Tag form
npm run test:surface -- --tag explorer-dependencies

# List only (no live CMS)
npm run test:surface:list -- --path tests/explorer-dependencies.spec.js
npm run test:surface:list -- --tag explorer-dependencies
```

#### Profile shell + axe WCAG + locale title (#2393 / #2425 / #2427 / #2497 / #2498 / #2499 / #2501 / parent #2374)

Smoke opens the **My profile** hub via deep link and user-menu entry (Admin,
Editor, Contributor); English smoke keeps `/my profile/i` on
`perc-profile-title`. Locale residual (**#2499**): login with **de** or **es**
(prefer `de-de`/`de`, then `es`) and assert the title via TMX
`perc.ui.profile.modern@My profile` (`Mein Profil` / `mi perfil`) — not
English-only. Axe-core gates assert **zero serious/critical** WCAG 2.1 A/AA
violations on `[data-testid="perc-profile-shell"]` for **Admin, Editor,
and Contributor** (helper: `tests/helpers/a11y.js`). #2501 extends the #2427
Admin-only axe residual to non-admin roles (deep link + My profile menu).
Title map helper: `tests/helpers/profile-shell-title.js`
(unit: `npm run test:unit` → `profile-shell-title.test.js`).

**Unattended matrix:** `@profile` is on the **extended** golden multi-path set
(`npm run test:golden-extended`) so overnight/CI reference gates do not miss
profile entry regressions when operators only run golden-extended (#2498).
Baseline `test:golden` stays login + Explorer only.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/profile-shell.spec.js` |
| Tags | `@profile` `@smoke` |
| Golden inventory id | `profile-shell` (tier `extended`) |
| Locale residual | `#2499` — `--grep "locale"` |
| Title helper | `tests/helpers/profile-shell-title.js` |
| Axe helper | `tests/helpers/a11y.js` → `expectNoSeriousA11yViolations` |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  EDITOR_USERNAME=Editor EDITOR_PASSWORD=<from-qa-up> \
  CONTRIBUTOR_USERNAME=Contributor CONTRIBUTOR_PASSWORD=<from-qa-up> \
  npm run test:surface -- --path tests/profile-shell.spec.js

# Extended golden multi-path (baseline + folder-recycle + profile)
npm run test:golden-extended
npm run test:golden-extended:list

# Locale title residual only (#2499)
npm run test:surface -- --path tests/profile-shell.spec.js --grep "locale"

# Axe gates only (Admin + Editor + Contributor)
npm run test:surface -- --path tests/profile-shell.spec.js --grep "axe-core"

# Non-admin axe residual (#2501)
npm run test:surface -- --path tests/profile-shell.spec.js --grep "#2501"

# List only (no live CMS)
npm run test:surface:list -- --path tests/profile-shell.spec.js
npm run test:surface:list -- --tag profile
```

#### Explorer shell chrome composition (#2850 / parent #2407 / #2400)

Surface-filtered companion for product Explorer shell composition: DCE menu bar,
display-format selector, server action toolbar region, and **Search panel**
toggle (View → Search and Content → Search). Hard-asserts free-text SearchPanel
chrome when open; soft-asserts optional toolbar item children when the H2 action
catalog is empty. Soft-skip **only** when a live QA CMS is unavailable (document
in the PR / issue) — not when chrome is missing on a healthy CMS.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/explorer-shell-chrome.spec.js` |
| Tags | `@explorer-shell-chrome` `@explorer` `@smoke` `@search` |
| Unit (no CMS) | `npm run test:unit` (includes `explorer-shell-chrome.test.js`) |
| Helper | `frontend/tests/helpers/explorer-shell-chrome.js` |
| Product peer | `WebUI/.../ContentExplorerShell.tsx` + `ExplorerMenuBar` + Vitest |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/explorer-shell-chrome.spec.js

# Tag form
npm run test:surface -- --tag explorer-shell-chrome

# List only (no live CMS)
npm run test:surface:list -- --path tests/explorer-shell-chrome.spec.js
npm run test:surface:list -- --tag explorer-shell-chrome
```

#### Explorer saved-search picker (#2507 / parent #2409 / #2400 slice D)

Surface-filtered live-CMS companion to WebUI SearchPanel saved-search picker
(#2506). Opens modern Content Explorer (`spa.jsp?entry=explorer`) → toggles
SearchPanel → asserts catalog chrome (picker / empty / error) → when the
fixture has a runnable design search, selects it and asserts post-execute
results list, empty, or error region wiring.

**Soft-skip:** if `GET /services/searches` has no non-custom-URL design search,
the execute-path test soft-skips after catalog UI assertions (documented for
minimal H2 fixtures). Shell open + catalog settle remain hard.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/explorer-saved-search.spec.js` |
| Tags | `@saved-search` `@explorer-saved-search` `@explorer` |
| Unit (no CMS) | `npm run test:unit` (includes `explorer-saved-search.test.js`) |
| Helper | `frontend/tests/helpers/explorer-saved-search.js` |
| Product peer | `WebUI/.../SearchPanel.tsx` + Vitest `SearchPanel.test.tsx` |

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/explorer-saved-search.spec.js

# Tag form
npm run test:surface -- --tag saved-search
npm run test:surface -- --tag explorer-saved-search

# List only (no live CMS)
npm run test:surface:list -- --path tests/explorer-saved-search.spec.js
npm run test:surface:list -- --tag saved-search
```

#### Profile shell keyboard section-nav / focus (#2502 / residual #2427)

Beyond axe: keyboard path Tab → `perc-profile-nav-*` → Enter focuses and
scrolls `perc-profile-section-*` (`tabIndex={-1}` hash targets); asserts
focus-visible rings stay usable on nav links after activation.

| Item | Value |
|------|--------|
| Spec | `frontend/tests/profile-shell-keyboard.spec.js` |
| Tags | `@profile` `@a11y` `@keyboard` |
| Product focus ring | `WebUI/.../ProfileShell.module.css` `.sectionNavLink:focus-visible` + `.section:focus-visible` |

```bash
# After qa-up — path-filtered only
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/profile-shell-keyboard.spec.js

# With axe/smoke peers
npm run test:surface -- --path tests/profile-shell.spec.js \
  --path tests/profile-shell-keyboard.spec.js

# List only (no live CMS)
npm run test:surface:list -- --path tests/profile-shell-keyboard.spec.js
npm run test:surface:list -- --tag keyboard
```

#### Profile form axe WCAG (account / preferences / avatar) (#2503 / residual #2427 / parent #2374)

When profile **form** slices land, extend the shell axe pattern
(`expectNoSeriousA11yViolations`) to each **form root** testid — not only the
hub chrome (`perc-profile-shell`).

| Surface | Spec | Form root scope | Product issue |
|---------|------|-----------------|---------------|
| Account edit | `tests/profile-account.spec.js` | `[data-testid="perc-profile-account"]` | #2395 |
| Preferences | `tests/profile-preferences.spec.js` | `[data-testid="perc-profile-preferences"]` | #2396 |
| Avatar / Gravatar | `tests/profile-avatar.spec.js` | `[data-testid="perc-profile-avatar"]` | #2397 |
| Password (local auth) | *not on main yet* | — | #2394 (still open) — residual when form lands |

Account also scans after a client email-validation error so `aria-invalid` /
error live region stay free of serious/critical issues. Password form axe is
intentionally out of scope until #2394 merges.

```bash
# After qa-up — path-filtered only (do not run full suite)
cd modules/perc-qa-automation/frontend
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/profile-account.spec.js --grep "axe-core"

TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/profile-preferences.spec.js --grep "axe-core"

TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- --path tests/profile-avatar.spec.js --grep "axe-core"

# Multi-path landed form surfaces (axe only)
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm run test:surface -- \
    --path tests/profile-account.spec.js \
    --path tests/profile-preferences.spec.js \
    --path tests/profile-avatar.spec.js \
    --grep "axe-core"

# List only (no live CMS)
npm run test:surface:list -- --path tests/profile-account.spec.js
npm run test:surface:list -- --path tests/profile-preferences.spec.js
npm run test:surface:list -- --path tests/profile-avatar.spec.js
```

**`run-surface` refuses the full suite** unless you pass `--allow-full` (agents must
not use that by default).

**Tags (optional):** annotate specs with Playwright `tag` so `--tag` / `--grep @name`
selects them:

```javascript
test("admin can open explorer", { tag: ["@smoke", "@explorer"] }, async ({ page }) => {
  // ...
});
```

Until a tag is present, prefer **path** and **title --grep**.

**Failure artifacts** (after a live run): under `frontend/test-results/` (screenshots,
traces, error context) and `frontend/playwright-report/` (HTML). How agents attach
these to PRs/issues: [playwright-failure-artifacts.md](../../docs/developer-module/playwright-failure-artifacts.md)
(#2066) when present; otherwise upload paths as PR comment attachments or gist links.

**Agent QA mode path (product docs):** see
[workbench-rest-and-qa-modes.md](../../docs/developer-module/workbench-rest-and-qa-modes.md)
→ surface filter + proposed agent rules. Root/module `AGENTS.md` rule-file updates
require human review before commit (root AGENTS hard gate).

### Maven

`mvn test` from `modules/perc-qa-automation` runs Java Surefire (no tests in this module). Use `npm test` to run Playwright. The `frontend-maven-plugin` only installs Node + `npm ci` — it does NOT invoke Playwright at the Maven `test` phase; the Playwright run is an explicit `npm test` step after the Maven build.

### Install matrix smoke (opt-in Maven profile)

Layer 1 CMS/DTS install matrix (silent install → start → login/health probe → destroy)
lives in `docker/scripts/matrix-install-smoke.py`. It is **not** part of the default
reactor / `mvn test` (Docker + multi-minute installs).

Enable with profile **`matrix-smoke`** from this module (use the repo-root `Maven wrapper`
wrapper so JDK is correct):

```bash
# From modules/perc-qa-automation
# CMS + H2 (default)
../../mvnw -Pmatrix-smoke test

# CMS + PostgreSQL (requires Docker compose profile postgres; host port 5433 by default)
../../mvnw -Pmatrix-smoke -Dmatrix.db=postgresql test

# Multiple backends (sequential cells)
../../mvnw -Pmatrix-smoke -Dmatrix.db=h2,postgresql,mysql,sqlserver test

# Product list
../../mvnw -Pmatrix-smoke -Dmatrix.product=cms,dts -Dmatrix.db=h2 test
```

|         Property          | Default |                     Description                      |
|---------------------------|---------|------------------------------------------------------|
| `matrix.product`          | `cms`   | Comma list: `cms`, `dts`                             |
| `matrix.db`               | `h2`    | Comma list: `h2`, `postgresql`, `mysql`, `sqlserver` |
| `matrix.probe.timeout`    | `1200`  | Seconds to wait for login/health HTTP                |
| `matrix.skip.image.build` | `true`  | Passed as `--skip-image-build` when true             |

**Prerequisites**

- Docker + Docker Compose
- Built customer installer jar: `modules/perc-distribution-tree/target/perc-distribution-tree.jar`
  (and DTS jar for `dts` product)
- For external DBs: compose profiles (`postgres`, `mysql`, `sqlserver`) — see root `docker-compose.yml`

**Manual harness (same script)**

```bash
# From repo root
python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --probe-timeout 1200

# Leave cell up for Playwright Layer 2 (install.spec.js)
python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep
# Use the pinned CMS_HOST_PORT (or TEST_CMS_URL from perc-devctl qa-up). Prefer
# preferred 9993 only when free — multi-worktree freeport may allocate another port.
TEST_CMS_URL=http://127.0.0.1:${CMS_HOST_PORT:-9993} TEST_DB_TYPE=postgresql \
  npm test --prefix frontend -- tests/install.spec.js
```

Results: `docker/logs/matrix-results-*.json` and `RESULT:OK` / `RESULT:FAIL` on stdout.

## Test Configuration

Tests are configured in `frontend/playwright.config.js`. Key settings:

- `testDir`: `./tests` (resolved relative to the config file's directory — always run Playwright from `frontend/`)
- `timeout`: 30 seconds per test
- `headless`: Tests run in headless mode by default

## Adding New Tests

1. Create new test files in `frontend/tests/`
2. Use Playwright's test syntax
3. Update `frontend/playwright.config.js` if needed

## Authentication Helpers

The module provides reusable login functions in `frontend/tests/helpers/auth.js`:

```javascript
const { loginAsAdmin, loginAsEditor, loginAsContributor, BASE_URL } = require('./tests/helpers/auth');

// Login as Admin
await loginAsAdmin(page);

// Login as Editor (for testing editor workflows)
await loginAsEditor(page);

// Login as Contributor (for testing contributor workflows)
await loginAsContributor(page);
```

## Example Test

```javascript
const { test, expect } = require('@playwright/test');
const { loginAsAdmin, BASE_URL } = require('./tests/helpers/auth');

test('login test', async ({ page }) => {
  await loginAsAdmin(page);
  
  // Verify successful login
  await expect(page).toHaveURL(/dash/);
});
```

## Using Environment Variables in Tests

```javascript
require('dotenv').config({ path: require('path').resolve(__dirname, '../../../.env') });

test('use configuration', async ({ page }) => {
  // Prefer BASE_URL from auth helpers (respects TEST_CMS_URL precedence).
  const { BASE_URL } = require('./tests/helpers/auth');
  console.log('CMS URL:', BASE_URL);
  console.log('DTS URL:', process.env.DEV_PERCUSSION_DTS_URL);

  await page.goto(BASE_URL);
});
```

## Failure artifacts (night-issue / PR attach)

On a failed Playwright run, collect outputs under **`frontend/`** (always run tests from that directory):

|           Directory           |                                             Role                                              |
|-------------------------------|-----------------------------------------------------------------------------------------------|
| `frontend/test-results/`      | Default Playwright `outputDir` (`error-context.md`, traces, failure screenshots when enabled) |
| `frontend/playwright-report/` | HTML report when `--reporter=…,html` is used                                                  |
| `frontend/tests/screenshots/` | Optional manual failure screenshots (module convention)                                       |

These paths are **gitignored** — do not commit them. For overnight agents attaching failures to a PR or issue (inline summary, small zip/gist, size limits, cross-platform notes), follow:

**[docs/developer-module/playwright-failure-artifacts.md](../../docs/developer-module/playwright-failure-artifacts.md)**

QA-mode stack lifecycle (`qa-up` / `TEST_CMS_URL` / `qa-down`) is documented in [workbench-rest-and-qa-modes.md](../../docs/developer-module/workbench-rest-and-qa-modes.md). Full CI artifact upload is a separate track (#1930), not this module’s default `npm test` path.

