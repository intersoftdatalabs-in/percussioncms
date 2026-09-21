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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Developer site create / update / delete (slice 22 / #4617 / parent #1690).
 *
 * Surface-filtered QA:
 *   npm run test:surface -- --path tests/developer-site-create-update-delete.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerSitesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "sites",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueSiteName() {
  return `NightlySite${Date.now()}`.slice(0, 50);
}

function attachConsoleGuards(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return {
    assertClean() {
      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      const unexpectedConsole = consoleErrors.filter(
        (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
      );
      expect(
        unexpectedConsole,
        `console error: ${unexpectedConsole.join(" | ")}`,
      ).toEqual([]);
    },
  };
}

test.describe("Developer site create update delete (#4617)", () => {
  test("Admin can create, update, and delete a site from Developer Sites", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const guards = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const name = uniqueSiteName();
    await page.locator('[data-testid="developer-site-new"]').click();
    await expect(page.locator('[data-testid="developer-site-create"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-create-name"]').fill(name);
    await page
      .locator('[data-testid="developer-site-create-description"]')
      .fill("Nightly CUD");
    await page.locator('[data-testid="developer-site-create-save"]').click();

    await expect(page.locator('[data-testid="developer-site-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.locator('[data-testid="developer-site-description-input"]').fill("Updated");
    await page.locator('[data-testid="developer-site-save"]').click();
    await expect(page.locator('[data-testid="developer-site-save-notice"]')).toBeVisible({
      timeout: 15_000,
    });

    await page.locator('[data-testid="developer-site-delete"]').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    await expect(page.locator('[data-testid="developer-site-detail"]')).toHaveCount(0, {
      timeout: 20_000,
    });
    guards.assertClean();
  });
});
