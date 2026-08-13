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
 * GH-3340 — Admin tools must NOT restore a right-side Administration sibling.
 *
 * QA filed #3340 against an older test plan that expected:
 *   - admin-sibling-workflow-link on Admin tools
 *   - a separate Administration shell (workflow / users / roles / categories)
 *   - reverse Admin tools sibling from that shell
 *
 * 8.2 product (#3088 / #3201) is a single Admin tools shell. Workflow,
 * users, roles, and categories are in-shell tabs. Product docs:
 *   product-docs/8.2/admin/index.md
 * Companion: tests/top-nav-restructure.spec.js
 *
 * Surface filter:
 *   npm run test:surface -- --path tests/bugs/bug-3340-admin-no-sibling.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function adminEntry() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=admin&_=${Date.now()}`;
}

test.describe("GH-3340 no Administration sibling on Admin tools", () => {
  test(
    "Admin tools has in-shell tabs, not admin-sibling-workflow-link",
    {
      tag: ["@admin", "@workflow-admin", "@bug-3340", "@smoke"],
    },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.goto(adminEntry(), { waitUntil: "domcontentloaded" });

      const shell = page.getByTestId("perc-admin-shell");
      await expect(shell).toBeVisible({ timeout: 30_000 });
      await expect(page.getByTestId("perc-admin-shell-title")).toContainText(
        /Admin tools/i,
      );

      // #3340 expected result (old plan) is NOT product: no sibling chrome
      await expect(page.getByTestId("admin-sibling-workflow-link")).toHaveCount(
        0,
      );
      await expect(page.getByTestId("admin-sibling-tools-link")).toHaveCount(0);
      await expect(page.getByTestId("perc-workflow-admin-shell")).toHaveCount(0);
      await expect(
        shell.getByRole("link", { name: "Administration", exact: true }),
      ).toHaveCount(0);

      await expect(page.getByTestId("tab-workflow")).toBeVisible();
      await expect(page.getByTestId("tab-users")).toBeVisible();
      await expect(page.getByTestId("tab-roles")).toBeVisible();
      await expect(page.getByTestId("tab-categories")).toBeVisible();
      await expect(page.getByTestId("tab-tools")).toBeVisible();

      await page.getByTestId("tab-workflow").click();
      await expect(page.getByTestId("perc-workflow-section")).toBeVisible({
        timeout: 30_000,
      });
      // Still one shell — no reverse sibling to a second Administration page
      await expect(page.getByTestId("perc-admin-shell")).toBeVisible();
      await expect(page.getByTestId("admin-sibling-tools-link")).toHaveCount(0);
      await expect(page.getByTestId("route-error")).toHaveCount(0);

      const unexpected = [...pageErrors, ...consoleErrors].filter(
        (t) =>
          !/favicon|404|net::ERR|Failed to load resource/i.test(t) &&
          !/Download the React DevTools/i.test(t),
      );
      expect(unexpected, `JS console errors: ${unexpected.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
