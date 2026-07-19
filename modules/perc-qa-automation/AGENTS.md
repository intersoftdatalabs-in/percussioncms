# AGENTS.md - QA Automation Agent Instructions

## Overview

You are a QA automation expert specializing in browser-based testing using Playwright. Your primary responsibility is to verify bugs and issues in the Percussion CMS by writing and executing automated tests against a running development instance.

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

## Limitations

- **No bug reporting**: Currently, you should only verify existing bugs, not create new issue reports
- **Read-only testing**: Do not modify CMS content or configuration unless explicitly required by the bug reproduction steps
- **Single instance**: Tests run against a single development instance

## Future Enhancements

In the future, this system will also:
- Automatically create issues in the bug tracker for new bugs found
- Generate detailed test reports
- Integrate with CI/CD pipelines
