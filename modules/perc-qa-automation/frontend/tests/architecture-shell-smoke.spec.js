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
 * Architecture SPA shell + top-nav routing (#3094 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-shell-smoke.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Entry: spa.jsp?entry=architecture (empty/in-progress shell until Slice C).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function architectureUrl(extra = {}) {
  const q = new URLSearchParams({
    entry: "architecture",
    _: String(Date.now()),
    ...extra,
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Architecture SPA shell (#3094)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("deep link and TopNav open Navigation shell @smoke @ui", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-spa-topnav")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("nav-architecture")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("nav-architecture")).toHaveText(/Navigation/i);
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });
    // Demo-site QA cells auto-select a site (picker/tree); empty H2 cells show empty-state.
    const emptyState = page.getByTestId("architecture-empty-state");
    const picker = page.getByTestId("architecture-site-picker");
    const tree = page.getByTestId("architecture-tree-panel");
    // Demo-site cells show picker + tree together; Playwright .or() is strict
    // when more than one locator matches.
    await expect(emptyState.or(picker).or(tree).first()).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.getByTestId("architecture-shell-title")).toContainText(
      /Navigation/i,
    );
    expect(pageErrors, "uncaught pageerror on Navigation shell").toEqual([]);

    // Top-nav Architecture is SPA NavLink (not legacy ?view=arch)
    const href = await page
      .getByTestId("nav-architecture")
      .getAttribute("href");
    expect(href || "").toMatch(/architecture/);
    expect(href || "").not.toMatch(/view=arch/);

    // Re-enter via top nav
    await page.getByTestId("nav-home").click();
    await expect(page.getByTestId("perc-spa-topnav")).toBeVisible({
      timeout: 15_000,
    });
    await page.getByTestId("nav-architecture").click();
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });
  });

  test("New Site affordance opens the create-site wizard @smoke @ui", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });
    const newSite = page.getByTestId("architecture-action-new-site");
    await expect(newSite).toBeVisible();
    await expect(newSite).toBeEnabled();
    await expect(newSite).toContainText(/New Site/i);
    await newSite.click();
    await expect(page.getByTestId("architecture-new-site-panel")).toBeVisible();
    await expect(page.getByTestId("site-create-step-type")).toBeVisible();
    await expect(page.getByTestId("site-create-type-virtual")).toBeEnabled();
    await page.getByTestId("architecture-new-site-close").click();
    await expect(page.getByTestId("architecture-new-site-panel")).toHaveCount(0);
    expect(pageErrors, pageErrors.join("\n")).toEqual([]);
  });
});
