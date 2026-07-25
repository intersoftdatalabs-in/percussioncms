/**
 * CMS Install Test - Verify CMS runs on each database backend
 *
 * Tests that a fresh CMS install works with each supported database.
 * The CMS containers must be running (see docker-compose.yml profiles).
 *
 * Usage:
 *   # Start containers for each backend:
 *   docker compose --profile cms-h2 up -d
 *   docker compose --profile cms-mysql up -d
 *   docker compose --profile cms-postgresql up -d
 *   docker compose --profile cms-sqlserver up -d
 *
 *   # Run tests:
 *   TEST_CMS_URL=http://localhost:9993 npm test -- tests/install.spec.js
 *   TEST_CMS_URL=http://localhost:9994 npm test -- tests/install.spec.js
 *   TEST_CMS_URL=http://localhost:9995 npm test -- tests/install.spec.js
 *   TEST_CMS_URL=http://localhost:9996 npm test -- tests/install.spec.js
 *
 * Environment Variables:
 *   TEST_CMS_URL    - CMS URL (default: http://localhost:9993)
 *   TEST_DB_TYPE    - Database type label for reporting (default: h2)
 */

const { test, expect } = require('@playwright/test');

const CMS_URL = process.env.TEST_CMS_URL || 'http://localhost:9993';
const DB_TYPE = process.env.TEST_DB_TYPE || 'h2';

test.describe(`CMS on ${DB_TYPE} database`, () => {
  test(`should load login page for ${DB_TYPE} backend`, async ({ page }) => {
    console.log(`Testing CMS at ${CMS_URL} with ${DB_TYPE} database...`);

    await page.goto(`${CMS_URL}/Rhythmyx/login`, { timeout: 120000 });

    const title = await page.title();
    console.log('Page title:', title);

    await expect(page).toHaveTitle(/Percussion|Login|Rhythmyx/i, { timeout: 60000 });

    console.log(`✓ Login page verified for ${DB_TYPE} backend!`);
  });
});
