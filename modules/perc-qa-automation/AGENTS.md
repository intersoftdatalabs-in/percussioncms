This project follows the Universal Code v1.0.0 - read ../../docs/policies/UC-EMBED-v1.0.0.md (vendored; upstream https://github.com/monkeyking-hq/universal-code)

# AGENTS.md - QA Automation Agent Instructions

## Overview

You are a QA automation expert specializing in browser-based testing using Playwright. Your primary responsibility is to verify bugs and issues in the Percussion CMS by writing and executing automated tests against a running development instance.

### Partnership with WebUI (HARD GATE for product UI)

**WebUI agents must create or update Playwright specs in this module whenever they change a product UI screen** (user-visible chrome, flows, navigation, i18n, dialogs, etc.). See [`WebUI/AGENTS.md`](../../WebUI/AGENTS.md) → **Playwright (HARD GATE)**.

- Prefer stable `data-testid` selectors already present (or added) in React sources under `WebUI/src/main/ts/`.
- Put screen/feature coverage next to existing specs (`login.spec.js`, `us*.spec.js`, …) or under `tests/bugs/bug-<id>.spec.js` for regressions.
- Vitest in WebUI is **not** a substitute for this module’s live-CMS Playwright coverage.
- When reviewing a WebUI PR that touches screens: fail the review if no matching Playwright create/update is present (unless the change is documented as non-UI behavior only).

## Current Capabilities

- **Login**: Successfully authenticate to the Percussion CMS admin interface with multiple user roles
- **Navigation**: Navigate through the CMS UI elements
- **Verification**: Verify that specific UI elements, workflows, or behaviors are working as expected

## Environment Setup

### QA mode (agents — no host install) HARD GATE path

Unattended / overnight runs must **not** require `DEV_PERCUSSION_INSTALL`. Use
env pointing at the H2 Docker stack from `perc-devctl qa-up`:

```bash
# After: python docker/scripts/perc-devctl.py qa-up
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up-or-docker-exec> \
  npm test -- tests/install.spec.js
```

- **URL precedence:** `TEST_CMS_URL` (aliases `CMS_BASE_URL`, `QA_CMS_URL`) >
  `QA_CMS_HOST_PORT` / `CMS_HOST_PORT` → `http://127.0.0.1:<port>` >
  `DEV_PERCUSSION_URL` > install discovery > fallback.
- **Do not hardcode `:9993`** as the only host port (freeport multi-worktree; issues #2005/#2014). Prefer the `TEST_CMS_URL` printed by `qa-up`.
- **Admin creds:** `ADMIN_USERNAME=Admin` (default); password from env / `qa-up`
  output / `docker exec` — **never commit secrets**.
- Pure resolver + unit tests: `frontend/tests/helpers/resolve-cms-env.js`,
  `npm run test:unit` (no live CMS).
- Failure artifact dirs: `frontend/test-results/`, `frontend/playwright-report/`
  (attach runbook: `docs/developer-module/playwright-failure-artifacts.md`).

### QA mode surface filter (HARD GATE for unattended UI gates)

Do **not** run the full Playwright suite as the default overnight or agent gate.
Run a **subset** for the surface under test (path / title grep / tag).

From `modules/perc-qa-automation/frontend`:

```bash
# List / print only — no live CMS required
npm run test:unit
npm run test:surface:list -- --path tests/login.spec.js
npm run test:surface:print -- --tag smoke

# Live QA mode after perc-devctl qa-up (use printed TEST_CMS_URL — freeport contract)
TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
  ADMIN_USERNAME=Admin ADMIN_PASSWORD=<from-qa-up> \
  npm run test:surface -- --path tests/login.spec.js

# Alternatives
npm run test:surface -- --grep "Admin login"
npm run test:surface -- --tag golden
```

- `run-surface` **refuses the full suite** unless `--allow-full` is explicit.
- Prefer path/grep/tag for PR surfaces; golden smoke: `tests/golden-unattended-smoke.spec.js`.
- Product direction of record: `docs/developer-module/workbench-rest-and-qa-modes.md`
  (§ Playwright surface filter + unattended H2 QA). Module README → **Surface filter**.
- Tear-down: `python docker/scripts/perc-devctl.py qa-down` when the agent started the stack.

### Dev mode environment (human fast loop)

The module uses auto-discovery. You can set a `.env` file in the module root
(`modules/perc-qa-automation/.env`):

```bash
# Path to your local CMS installation
DEV_PERCUSSION_INSTALL=/path/to/your/cms-install

# Optional: CMS URL (auto-calculated from installation)
DEV_PERCUSSION_URL=http://localhost:9992

# Optional: DTS Installation and URL (auto-calculated from installation)
DEV_PERCUSSION_DTS_INSTALL=/path/to/your/dts-install
DEV_PERCUSSION_DTS_URL=http://localhost:9980

# User credentials (auto-read from installation if not set)
ADMIN_USERNAME=Admin
# ADMIN_PASSWORD=... (auto-discovered)
EDITOR_USERNAME=Editor
# EDITOR_PASSWORD=... (auto-discovered)
CONTRIBUTOR_USERNAME=Contributor
# CONTRIBUTOR_PASSWORD=... (auto-discovered)
```

### Auto-Configuration (dev mode)

When `DEV_PERCUSSION_INSTALL` is set and QA URL env is not, helpers will:

1. Read the CMS URL from `jetty/base/etc/installation.properties` → `jetty.http.port`
2. Read the DTS URL from `{DEV_PERCUSSION_DTS_INSTALL}/Deployment/Server/conf/perc/perc-catalina.properties` → `http.port`
3. Read all user passwords from `var/config/generated/passwords`
4. Save discovered values to `.env` for faster subsequent runs

### Using Configuration in Tests

Import the auth helpers which include configuration:

```javascript
const { loginAsAdmin, loginAsEditor, loginAsContributor, BASE_URL, DTS_URL } = require('./tests/helpers/auth');

// Use BASE_URL (TEST_CMS_URL / install / fallback — never hardcode port alone)
await page.goto(`${BASE_URL}/Rhythmyx/login`);
```

Prefer `BASE_URL` from auth over raw `process.env.DEV_PERCUSSION_URL` so QA mode works.

## User Roles

The module supports three user roles:

|    Role     |    Helper Function     |           Typical Use Case            |
|-------------|------------------------|---------------------------------------|
| Admin       | `loginAsAdmin()`       | Full system access, admin workflows   |
| Editor      | `loginAsEditor()`      | Content editing, publishing workflows |
| Contributor | `loginAsContributor()` | Content creation, limited workflows   |

## How to Verify a Bug

When given a bug to verify:

1. **Understand the bug report** - Read the description carefully, noting:
   - Which user role is affected
   - Steps to reproduce
   - Expected behavior
   - Actual behavior
   - Any error messages
2. **Select appropriate login** - Use the user role specified in the bug:

   ```javascript
   await loginAsAdmin(page);    // For admin-level bugs
   await loginAsEditor(page);  // For editor-level bugs
   await loginAsContributor(page); // For contributor-level bugs
   ```
3. **Write a test case** - Create a new test file or add to existing tests:

   ```javascript
   const { test, expect } = require('@playwright/test');
   const { loginAsAdmin, BASE_URL } = require('./tests/helpers/auth');

   test.describe('Bug Verification: [Bug ID/Title]', () => {
     test('should [expected behavior]', async ({ page }) => {
       // Given: Set up the initial state
       await loginAsAdmin(page);

       // When: Perform the steps to reproduce
       await page.goto(`${BASE_URL}/cm/...`);
       await page.click('...');

       // Then: Verify the expected behavior
       await expect(page.locator('...')).toBeVisible();
     });
   });
   ```
4. **Run the test**:

   ```bash
   cd modules/perc-qa-automation/frontend
   npm test -- tests/bugs/bug-123.spec.js
   ```
5. **Report results**:
   - If test passes: Bug is FIXED
   - If test fails: Bug still EXISTS - provide the error message and screenshots

## Test Organization

### Directory Structure

```
modules/perc-qa-automation/
├── frontend/
│   ├── tests/
│   │   ├── login.spec.js           # Authentication tests
│   │   ├── bugs/
│   │   │   ├── bug-123.spec.js    # Individual bug verification tests
│   │   │   └── ...
│   │   ├── workflows/              # End-to-end workflow tests
│   │   ├── components/             # UI component tests
│   │   └── helpers/
│   │       └── auth.js             # Authentication helpers
│   ├── playwright.config.js
│   └── package.json
├── .env                           # Local configuration (gitignored)
├── .env.example                   # Configuration template
└── pom.xml
```

### Naming Conventions

- Bug verification tests: `tests/bugs/bug-{issue-id}.spec.js`
- Feature tests: `tests/workflows/{feature-name}.spec.js`
- Component tests: `tests/components/{component-name}.spec.js`

## Best Practices

### Writing Reliable Tests

1. **Use stable selectors**:

   ```javascript
   // Good - using data-testid
   await page.click('[data-testid="submit-button"]');

   // Good - using accessible attributes
   await page.click('button[aria-label="Save"]');

   // Avoid - fragile CSS selectors
   await page.click('.form-group > .btn.primary');
   ```
2. **Wait for elements properly**:

   ```javascript
   // Good - explicit wait
   await page.waitForSelector('[data-testid="results-table"]');

   // Good - assertion with auto-wait
   await expect(page.locator('[data-testid="success-message"]')).toBeVisible();
   ```
3. **Use authentication helpers**:

   ```javascript
   const { loginAsAdmin, BASE_URL } = require('./tests/helpers/auth');

   test('admin workflow', async ({ page }) => {
     await loginAsAdmin(page);
     // Continue with test...
   });
   ```
4. **Take screenshots on failure**:

   ```javascript
   test.afterEach(async ({ page }, testInfo) => {
     if (testInfo.status === 'failed') {
       await page.screenshot({ path: `tests/screenshots/${testInfo.title}.png` });
     }
   });
   ```

### Test Independence

- Each test should be able to run independently
- Clean up any state created during the test
- Don't depend on execution order

## Running Tests

### Full Test Suite

```bash
cd modules/perc-qa-automation/frontend
npm test
```

### Specific Test File

```bash
npm test -- tests/login.spec.js
```

### With Custom Config

```bash
npm test -- --config=playwright.config.js
```

### Debug Mode

```bash
npm test -- --debug
```

## Two modes: **dev** vs **QA** (HARD RULES)

Full product rules: [`docs/developer-module/workbench-rest-and-qa-modes.md`](../../docs/developer-module/workbench-rest-and-qa-modes.md).

|        Mode         |                 CMS                  |                    Docker                     |                                          Change loop                                          |                 Outcome                  |
|---------------------|--------------------------------------|-----------------------------------------------|-----------------------------------------------------------------------------------------------|------------------------------------------|
| **Dev mode** (fast) | **Local install on the dev machine** | **Binds** to that install (or uses that tree) | Build → **copy into install** → re-run Playwright — **no restart** for typical hot-copy paths | Iterate on specs + product               |
| **QA mode** (gate)  | **Fully contained in Docker**        | Stack owns the install                        | Image/stack built from the revision under test                                                | **Pass or fail** — no host install drift |

**Automation owns** full build → install/image → start → test. Agents must **not** treat manual “redeploy jars forever” as the primary process. Dev mode = local install + bind + hot copy; QA mode = all-in-docker.

### Dev mode: fast iteration (no container restart)

When the CMS is a **local install** used by docker bind/mount (or equivalent) at e.g. `localhost:9992` (see `docker-compose.yml` + `docker/scripts/perc-devctl.py`), **JS / TS / JSP changes do NOT require a restart** — rebuild and copy the artifact into the install webapp. See the WebUI AGENTS.md hot-deploy section. Quick reference:

```bash
# 1. Edit spec file in modules/perc-qa-automation/frontend/tests/
# 2. Run (no rebuild needed for spec changes)
cd modules/perc-qa-automation/frontend
npm test -- tests/<spec>.spec.js

# If you also touched a TS helper or the auth module:
cd WebUI/src/main/frontend
npm run build:modern
cp WebUI/target/generated-webui/cm/modern/assets/perc-modern-ui.js \
   /opt/Percussion/jetty/base/webapps/Rhythmyx/cm/modern/assets/perc-modern-ui.js
# (No restart needed for JS changes — the page picks up the new bundle on next request;
# add a cache-buster to the test URL to force a fresh fetch.)
```

**JSP-only changes** (e.g. residual dialog hosts or `spa.jsp`): copy the file to the runtime webapp, no build. Jetty serves the new JSP on the next request. Product explorer entry is `spa.jsp?entry=explorer` (PR-8 removed `explorerModern.jsp`).

**Test URL cache-buster** (when the CMS-side bundle changes):

```javascript
// in helpers/auth.js or the spec
const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
```

This avoids the browser caching the previous bundle across spec reruns.

## Iteration cost reference

|            Change             |                   Build                   |         Restart          |  Total   |
|-------------------------------|-------------------------------------------|--------------------------|----------|
| Spec file (.spec.js)          | none                                      | none                     | ~1 s     |
| TS / TSX in `helpers/`        | `npm run build:modern` (~3 s) + copy      | none                     | ~3 s     |
| CSS / styles                  | `npm run build:modern` + copy             | none                     | ~3 s     |
| JSP (e.g. modern entry point) | none (copy)                               | none                     | ~1 s     |
| WebUI backend Java            | `./mvnw -pl WebUI -am install` (~30–60 s) | `docker compose restart` | ~1–2 min |

When iterating, prefer the cheap paths first (spec → helpers → CSS → JSP → Java).

## Limitations

- **No bug reporting**: Currently, you should only verify existing bugs, not create new issue reports
- **Read-only testing**: Do not modify CMS content or configuration unless explicitly required by the bug reproduction steps
- **Single instance**: Tests run against a single development instance

## Future Enhancements

In the future, this system will also:
- Automatically create issues in the bug tracker for new bugs found
- Generate detailed test reports
- Integrate with CI/CD pipelines
