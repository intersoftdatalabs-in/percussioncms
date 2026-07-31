/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * GH-1529: Update server startup license disclaimer + surface it on the About screen.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: {@code /services/about} returns 200 with a version string and an up-to-date
 *       third-party license disclaimer (no stale jTDS v1.2.2 / Lato font / stale ASM or
 *       XStream copyright years).</li>
 *   <li>UI: clicking the "About" link in the SPA footer opens a dialog showing the same
 *       version and disclaimer text returned by the REST endpoint (single source of truth
 *       shared with the server startup console log).</li>
 * </ul>
 *
 * <p>Run against a live CMS (e.g. a local install or docker dev stack):</p>
 * <pre>
 *   cd modules/perc-qa-automation/frontend
 *   npm test -- tests/bugs/bug-1529-about-license-disclaimer.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");

const HOME_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
const ABOUT_ENDPOINT = `${BASE_URL}/Rhythmyx/services/about`;

test.describe("GH-1529 About / license disclaimer", () => {
  test("REST: /services/about returns an up-to-date, non-stale disclaimer", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const headers = adminBasicAuthHeaders();
    const response = await request.get(ABOUT_ENDPOINT, { headers });
    expect(response.status(), `GET ${ABOUT_ENDPOINT} should be 200`).toBe(200);

    const body = await response.json();
    expect(body.versionString, "versionString should be present").toBeTruthy();
    expect(body.copyright, "copyright should be present").toContain(
      "Percussion"
    );
    expect(
      body.thirdPartyCopyright,
      "thirdPartyCopyright should be present"
    ).toBeTruthy();

    // Must reference currently bundled components.
    expect(body.thirdPartyCopyright).toContain("Jetty");
    expect(body.thirdPartyCopyright).toContain("v1.3.1");
    expect(body.thirdPartyCopyright).toContain(
      "Microsoft JDBC Driver for SQL Server"
    );

    // Must NOT reference stale/removed components (GH-1529 acceptance criteria).
    expect(body.thirdPartyCopyright).not.toContain("v1.2.2");
    expect(body.thirdPartyCopyright).not.toContain("Lato");
    expect(body.thirdPartyCopyright).not.toContain("SIL Open Font License");
    expect(body.thirdPartyCopyright).not.toContain("GNU Runtime Libraries");
  });

  test("UI: About link in the SPA footer opens a dialog with the same disclaimer", async ({
    page,
    request,
  }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
    await page.goto(HOME_URL, { waitUntil: "networkidle" });

    const footer = page.locator('[data-testid="perc-brand-footer"]');
    await expect(footer).toBeVisible({ timeout: 15_000 });

    const aboutLink = page.locator(
      '[data-testid="perc-brand-footer-about-link"]'
    );
    await expect(aboutLink).toBeVisible();
    await aboutLink.click();

    const dialog = page.locator('[data-testid="perc-about-dialog"]');
    await expect(dialog).toBeVisible({ timeout: 15_000 });

    const versionEl = page.locator('[data-testid="perc-about-dialog-version"]');
    const copyrightEl = page.locator(
      '[data-testid="perc-about-dialog-copyright"]'
    );
    const thirdPartyEl = page.locator(
      '[data-testid="perc-about-dialog-third-party"]'
    );
    await expect(versionEl).toBeVisible({ timeout: 15_000 });
    await expect(copyrightEl).toBeVisible();
    await expect(thirdPartyEl).toBeVisible();

    // Cross-check against the REST endpoint - single source of truth (GH-1529).
    const headers = adminBasicAuthHeaders();
    const response = await request.get(ABOUT_ENDPOINT, { headers });
    const body = await response.json();

    await expect(versionEl).toContainText(body.versionString);
    await expect(thirdPartyEl).toContainText("Jetty");
    await expect(thirdPartyEl).not.toContainText("Lato");

    await page.locator('[data-testid="perc-about-dialog-close"]').click();
    await expect(dialog).not.toBeVisible();
  });
});
