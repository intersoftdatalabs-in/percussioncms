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
 * Top navigation restructure (#2702 / #2784 / #2953 / #3088 / #3201):
 * - Dashboard removed from SPA top chrome
 * - Explorer immediately after Home
 * - Single consolidated Admin labeled "Admin" (not "Administration")
 * - Admin lands on working Admin tools shell (/admin); workflow/roles/users/
 *   categories are in-shell tabs (sibling chrome removed in #3088).
 *   #3340 is not a revert: do not restore admin-sibling-workflow-link.
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/top-nav-restructure.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function homeDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function expectSpaTopNav(page) {
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-spa-topnav")).toBeVisible({
    timeout: 30_000,
  });
}

test.describe("Top nav restructure (#2702 / #3201)", () => {
  test("Home then Explorer; no Dashboard; single Admin @smoke @ui", async ({
    page,
  }) => {
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
    await expectSpaTopNav(page);

    const nav = page.getByTestId("perc-spa-topnav");
    const home = nav.getByTestId("nav-home");
    const explorer = nav.getByTestId("nav-explorer");
    const admin = nav.getByTestId("nav-admin");

    await expect(home).toBeVisible();
    await expect(explorer).toBeVisible();
    await expect(admin).toBeVisible();
    await expect(admin).toHaveText(/^Admin$/);
    await expect(nav.getByTestId("nav-dashboard")).toHaveCount(0);
    await expect(nav.getByTestId("nav-workflow")).toHaveCount(0);
    await expect(nav.getByRole("link", { name: "Dashboard", exact: true })).toHaveCount(
      0,
    );
    await expect(
      nav.getByRole("link", { name: "Administration", exact: true }),
    ).toHaveCount(0);

    // DOM adjacency: Explorer is the next top-level nav item after Home
    const adjacency = await page.evaluate(() => {
      const homeLi = document
        .querySelector('[data-testid="nav-home"]')
        ?.closest("li");
      const next = homeLi?.nextElementSibling;
      return !!next?.querySelector('[data-testid="nav-explorer"]');
    });
    expect(adjacency).toBe(true);

    // Consolidated Admin lands on working Admin tools shell (#2784 / #3088 / #3201)
    await admin.click();
    await expect(page.getByTestId("perc-admin-shell")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page).toHaveURL(/\/admin(?:\/|$|\?)/);
    await expect(page.getByTestId("perc-admin-shell-title")).toContainText(
      /Admin tools/i,
    );
    await expect(page.getByTestId("tab-tools")).toBeVisible();
    await expect(page.getByTestId("tab-tasks")).toBeVisible();
    // No sibling cross-links; workflow is an Admin tab (#3088 / #3340)
    await expect(page.getByTestId("admin-sibling-workflow-link")).toHaveCount(0);
    await expect(page.getByTestId("admin-sibling-tools-link")).toHaveCount(0);
    await expect(page.getByTestId("tab-workflow")).toBeVisible();
    await expect(page.getByTestId("tab-roles")).toBeVisible();
    await expect(page.getByTestId("tab-users")).toBeVisible();
    await expect(page.getByTestId("tab-categories")).toBeVisible();

    await page.getByTestId("tab-workflow").click();
    await expect(page.getByTestId("perc-workflow-section")).toBeVisible({
      timeout: 30_000,
    });
    // Still one Admin shell (no WorkflowAdminShell product chrome)
    await expect(page.getByTestId("perc-admin-shell")).toBeVisible();
    await expect(page.getByTestId("perc-workflow-admin-shell")).toHaveCount(0);

    const unexpected = consoleErrors.filter(
      (t) =>
        !/favicon|404|net::ERR|Failed to load resource/i.test(t) &&
        !/Download the React DevTools/i.test(t),
    );
    expect(unexpected, `JS console errors: ${unexpected.join(" | ")}`).toEqual(
      [],
    );
  });
});
