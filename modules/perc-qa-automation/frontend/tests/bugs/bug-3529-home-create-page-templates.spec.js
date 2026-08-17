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
 * GH-3529: Home → Add New → Create Page Template dropdown must list
 * templates after a site is selected (not stay on “Select…”).
 *
 * <p>Tags: {@code @home} {@code @page-wizard}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/bugs/bug-3529-home-create-page-templates.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function homeDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
}

test.describe("GH-3529 Home Create Page templates", () => {
  test(
    "selecting a site populates the Create Page template dropdown",
    { tag: ["@home", "@page-wizard"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const consoleErrors = [];
      page.on("pageerror", (err) => {
        consoleErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.goto(homeDeepLink(), { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("home-shell")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("home-nav-create").click();
      await expect(page.getByTestId("create-type-chooser")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("create-choose-page").click();
      const empty = page.getByTestId("create-wizard-no-sites");
      const wizard = page.getByTestId("page-wizard");
      await expect(empty.or(wizard)).toBeVisible({ timeout: 20_000 });
      if (await empty.isVisible()) {
        test.skip(true, "No sites available for Home Create Page");
        return;
      }

      const siteSelect = page.getByTestId("page-wizard-site");
      if (await siteSelect.isVisible()) {
        const firstSite = siteSelect.locator("option[value]:not([value=''])").first();
        const siteValue = await firstSite.getAttribute("value");
        if (!siteValue) {
          test.skip(true, "No site options in Create Page wizard");
          return;
        }
        await siteSelect.selectOption(siteValue);
      }

      const templateSelect = page.getByTestId("page-wizard-template");
      await expect(templateSelect).toBeVisible({ timeout: 20_000 });
      await expect
        .poll(
          async () =>
            templateSelect.locator("option[value]:not([value=''])").count(),
          { timeout: 20_000 },
        )
        .toBeGreaterThan(0);

      const unexpected = consoleErrors.filter(
        (t) =>
          !/favicon|404|net::ERR|Failed to load resource|ResizeObserver/i.test(t) &&
          !/Download the React DevTools/i.test(t),
      );
      expect(
        unexpected,
        `JS console errors: ${unexpected.join(" | ")}`,
      ).toEqual([]);
    },
  );
});
