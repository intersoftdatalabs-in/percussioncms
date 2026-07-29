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

### Required Environment Variables

The module uses auto-discovery, but you can set a `.env` file in the module root (`modules/perc-qa-automation/.env`):

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

### Auto-Configuration

The test helpers will automatically:

1. Read the CMS URL from `jetty/base/etc/installation.properties` → `jetty.http.port`
2. Read the DTS URL from `{DEV_PERCUSSION_DTS_INSTALL}/Deployment/Server/conf/perc/perc-catalina.properties` → `http.port`
3. Read all user passwords from `var/config/generated/passwords`
4. Save discovered values to `.env` for faster subsequent runs

### Using Configuration in Tests

Import the auth helpers which include configuration:

```javascript
const { loginAsAdmin, loginAsEditor, loginAsContributor, BASE_URL, DTS_URL } = require('./tests/helpers/auth');

// Use BASE_URL (auto-discovered CMS URL)
await page.goto(`${BASE_URL}/Rhythmyx/login`);
```

Or use dotenv directly:

```javascript
require('dotenv').config({ path: require('path').resolve(__dirname, '../../../.env') });

const CMS_URL = process.env.DEV_PERCUSSION_URL;
const DTS_URL = process.env.DEV_PERCUSSION_DTS_URL;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;
```

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

## Fast iteration against the dev CMS (no container restart)

When the dev CMS is running via the docker compose stack at `localhost:9992` (see `docker-compose.yml` + `docker/scripts/perc-devctl.py`), **JS / TS / JSP changes do NOT require a container restart** — rebuild and copy the artifact. See the WebUI AGENTS.md "Hot Deployment" section for the full iteration cost table. Quick reference for QA work:

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

|            Change             |                      Build                      |         Restart          |  Total   |
|-------------------------------|-------------------------------------------------|--------------------------|----------|
| Spec file (.spec.js)          | none                                            | none                     | ~1 s     |
| TS / TSX in `helpers/`        | `npm run build:modern` (~3 s) + copy            | none                     | ~3 s     |
| CSS / styles                  | `npm run build:modern` + copy                   | none                     | ~3 s     |
| JSP (e.g. modern entry point) | none (copy)                                     | none                     | ~1 s     |
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
