/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * Design template-list legacy entry retirement (#3306 / #3579 / parent #2631).
 *
 * Required on perc-devctl qa-up H2 — no skip when classic Design list is
 * still hosted. Fail if admin.jsp / ?view=design do not land on
 * perc-design-shell.
 *
 * Surface-filtered:
 *   npm run test:surface -- --path tests/design-legacy-redirect.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  CLASSIC_ASSIGNED_TEMPLATES_ID,
  designLegacyAdminUrl,
  designLegacyViewUrl,
  isDesignSpaLandingUrl,
  filterConsoleNoise,
} = require("./helpers/design-spa-surface");

function tid(id) {
  return `[data-testid="${id}"]`;
}

function attachConsoleGate(page) {
  const errors = [];
  page.on("pageerror", (err) => {
    errors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      errors.push(msg.text());
    }
  });
  return errors;
}

test.describe("Design legacy list entry retirement (#3306 / #3579)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("admin.jsp hard-redirects to SPA Design @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = attachConsoleGate(page);
    await page.goto(designLegacyAdminUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });

    await expect(page.locator(tid(TEST_IDS.nav))).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator(tid(TEST_IDS.shell))).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator(`#${CLASSIC_ASSIGNED_TEMPLATES_ID}`)).toHaveCount(
      0,
    );
    expect(isDesignSpaLandingUrl(page.url())).toBe(true);
    expect(filterConsoleNoise(consoleErrors)).toEqual([]);
  });

  test("admin.jsp?view=admin still lands on SPA Design @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = attachConsoleGate(page);
    const url = `${BASE_URL.replace(/\/+$/, "")}/Rhythmyx/cm/app/admin.jsp?view=admin&_=${Date.now()}`;
    await page.goto(url, { waitUntil: "domcontentloaded" });

    await expect(page.locator(tid(TEST_IDS.shell))).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator(`#${CLASSIC_ASSIGNED_TEMPLATES_ID}`)).toHaveCount(
      0,
    );
    const finalUrl = page.url();
    expect(isDesignSpaLandingUrl(finalUrl)).toBe(true);
    expect(finalUrl).not.toMatch(/[?&]view=admin(?:&|$)/i);
    expect(filterConsoleNoise(consoleErrors)).toEqual([]);
  });

  test("?view=design deep link lands on SPA Design @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = attachConsoleGate(page);
    await page.goto(designLegacyViewUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });

    await expect(page.locator(tid(TEST_IDS.shell))).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator(tid(TEST_IDS.panel))).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("panel-design-templates")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator(`#${CLASSIC_ASSIGNED_TEMPLATES_ID}`)).toHaveCount(
      0,
    );
    expect(isDesignSpaLandingUrl(page.url())).toBe(true);
    expect(filterConsoleNoise(consoleErrors)).toEqual([]);
  });
});
