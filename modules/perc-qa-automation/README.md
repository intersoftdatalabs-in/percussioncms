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

**Always run from `modules/perc-qa-automation/frontend`** — Playwright resolves `playwright.config.js` relative to the current working directory. Running from the parent module dir or elsewhere will report "No tests found" because `testDir: './tests'` resolves incorrectly.

### First-time setup

```bash
cd modules/perc-qa-automation/frontend
npm ci                                              # install @playwright/test + dotenv
npx playwright install chromium                    # one-time browser download
# Optional: copy .env.example to .env (auto-discovered from
# DEV_PERCUSSION_INSTALL on first run if missing)
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
TEST_CMS_URL=http://localhost:9993 TEST_DB_TYPE=postgresql \
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
  console.log('CMS URL:', process.env.DEV_PERCUSSION_URL);
  console.log('DTS URL:', process.env.DEV_PERCUSSION_DTS_URL);
  
  await page.goto(process.env.DEV_PERCUSSION_URL);
});
```

