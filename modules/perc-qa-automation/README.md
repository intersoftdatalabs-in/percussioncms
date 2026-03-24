# perc-qa-automation

This module provides QA automation testing capabilities for Percussion CMS using Playwright.

## Purpose

The `perc-qa-automation` module is used for authoring and executing automated browser-based tests against a running Percussion CMS instance. It enables QA engineers and developers to write end-to-end tests that verify the functionality of the CMS.

## Prerequisites

- A running Percussion CMS instance
- Maven 3.x
- Java 8+

## Configuration

### Quick Start

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

|           Variable           | Required |                                    Description                                    |
|------------------------------|----------|-----------------------------------------------------------------------------------|
| `DEV_PERCUSSION_INSTALL`     | Yes      | Path to your local CMS installation (e.g., `/home/nate/installs/cms-8.1.7-317-2`) |
| `DEV_PERCUSSION_URL`         | No       | CMS URL (auto-calculated from installation)                                       |
| `DEV_PERCUSSION_DTS_INSTALL` | No       | Path to DTS installation (if separate from CMS)                                   |
| `DEV_PERCUSSION_DTS_URL`     | No       | DTS URL (auto-calculated from DTS installation)                                   |
| `ADMIN_USERNAME`             | No       | Admin username (default: `Admin`)                                                 |
| `ADMIN_PASSWORD`             | No       | Admin password (auto-read from installation)                                      |
| `EDITOR_USERNAME`            | No       | Editor username (default: `Editor`)                                               |
| `EDITOR_PASSWORD`            | No       | Editor password (auto-read from installation)                                     |
| `CONTRIBUTOR_USERNAME`       | No       | Contributor username (default: `Contributor`)                                     |
| `CONTRIBUTOR_PASSWORD`       | No       | Contributor password (auto-read from installation)                                |

### Auto-Configuration

On first run, the test helpers will automatically:

1. Read the CMS port from `jetty/base/etc/installation.properties` → `jetty.http.port`
2. Read the DTS port from `{DEV_PERCUSSION_DTS_INSTALL}/Deployment/Server/conf/perc/perc-catalina.properties` → `http.port`
3. Read all user passwords from `var/config/generated/passwords`
4. Save these values to `.env` for faster subsequent runs

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

### Using Maven

```bash
mvn test
```

### Using npm directly

```bash
cd frontend
npm test
```

### Running specific tests

```bash
npx playwright test tests/login.spec.js
```

## Test Configuration

Tests are configured in `frontend/playwright.config.js`. Key settings:

- `testDir`: Points to `./tests` where test specs are located
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
  console.log('CMS URL:', process.env.DEV_PERCUSSION_URL);
  console.log('DTS URL:', process.env.DEV_PERCUSSION_DTS_URL);
  
  await page.goto(process.env.DEV_PERCUSSION_URL);
});
```

