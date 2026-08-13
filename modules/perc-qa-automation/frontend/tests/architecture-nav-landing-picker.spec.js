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
 * Architecture landing page picker / replace (#3304 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-landing-picker.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Cancel / empty pick must not 500 or throw page errors.
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

test.describe("Architecture landing picker (#3304)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("landing picker cancel does not error @smoke @ui", async ({ page }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });

    const sitesEmpty = page.getByTestId("architecture-sites-empty");
    const sitesError = page.getByTestId("architecture-sites-error");
    const treePanel = page.getByTestId("architecture-tree-panel");

    await expect
      .poll(
        async () => {
          if (await sitesEmpty.isVisible().catch(() => false)) return "empty";
          if (await sitesError.isVisible().catch(() => false)) return "error";
          if (await treePanel.isVisible().catch(() => false)) return "tree";
          return "wait";
        },
        { timeout: 45_000 },
      )
      .not.toBe("wait");

    if (await treePanel.isVisible().catch(() => false)) {
      await expect(page.getByTestId("architecture-action-landing")).toBeVisible();

      const siteSelect = page.getByTestId("architecture-site-select");
      const optionValues = await siteSelect.locator("option").evaluateAll((els) =>
        els
          .map((el) => el.getAttribute("value") || el.value || "")
          .filter((v) => v && v !== ""),
      );
      for (const site of optionValues) {
        await siteSelect.selectOption(site);
        const firstSection = page
          .locator("[data-testid^='nav-tree-item-']")
          .first();
        const hasTree = await firstSection
          .waitFor({ state: "visible", timeout: 15_000 })
          .then(() => true)
          .catch(() => false);
        if (!hasTree) {
          continue;
        }
        await firstSection.click();
        const landingBtn = page.getByTestId("architecture-action-landing");
        if (await landingBtn.isEnabled().catch(() => false)) {
          await landingBtn.click();
          await expect(
            page.getByTestId("architecture-landing-dialog"),
          ).toBeVisible({ timeout: 10_000 });
          await page.getByTestId("architecture-landing-cancel").click();
          await expect(
            page.getByTestId("architecture-landing-dialog"),
          ).toHaveCount(0);
        }
        break;
      }
    }

    expect(
      consoleErrors.filter(
        (e) =>
          !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
            String(e),
          ),
      ),
    ).toEqual([]);
  });
});
