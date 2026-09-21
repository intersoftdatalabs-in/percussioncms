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
 * PublishingShell Sites list search (#4649 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesListSearch.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("PublishingShell Sites list search", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("filters site cards by name from Sites section", async ({ page }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });

    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish`);
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-section-sites")).toBeVisible();
    const filter = page.getByTestId("publish-sites-filter");
    await expect(filter).toBeVisible();

    const cards = page.locator("[data-testid='publish-section-sites'] [role='listitem']");
    await expect(cards.first()).toBeVisible({ timeout: 30000 });
    const before = await cards.count();
    expect(before).toBeGreaterThan(0);

    await filter.fill("zzzz-no-such-site");
    await expect(page.getByTestId("publish-empty-sites-filter")).toBeVisible();
    expect(await cards.count()).toBe(0);

    await filter.fill("");
    await expect(cards.first()).toBeVisible();
    expect(await cards.count()).toBe(before);

    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
