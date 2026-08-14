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
 * Architecture section properties dialog (#3353 / parent #3092).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-section-properties.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Cancel must not POST /section/update and must not 500 / pageerror.
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

test.describe("Architecture section properties (#3353)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("properties dialog loads and cancel does not error @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = [];
    const updatePosts = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });
    page.on("request", (req) => {
      if (
        req.method() === "POST" &&
        /\/section\/update(?:\?|$)/.test(req.url())
      ) {
        updatePosts.push(req.url());
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
      await expect(
        page.getByTestId("architecture-action-properties"),
      ).toBeVisible();

      const siteSelect = page.getByTestId("architecture-site-select");
      const optionValues = await siteSelect
        .locator("option")
        .evaluateAll((els) =>
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
        const propsBtn = page.getByTestId("architecture-action-properties");
        if (await propsBtn.isEnabled().catch(() => false)) {
          await propsBtn.click();
          await expect(
            page.getByTestId("architecture-properties-dialog"),
          ).toBeVisible({ timeout: 15_000 });
          await expect(
            page.getByTestId("architecture-properties-title"),
          ).toBeVisible({ timeout: 10_000 });
          await page.getByTestId("architecture-properties-cancel").click();
          await expect(
            page.getByTestId("architecture-properties-dialog"),
          ).toHaveCount(0);
        }
        break;
      }
    }

    expect(updatePosts).toEqual([]);
    expect(
      consoleErrors.filter(
        (e) =>
          !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
            e,
          ),
      ),
    ).toEqual([]);
  });
});
