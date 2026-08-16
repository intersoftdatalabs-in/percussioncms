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
 * Home Recent / Create open the React editor, not leftover {@code ?view=editor}.
 *
 * <p>Tags: {@code @home} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/home-react-editor.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function homeDeepLink() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
}

test.describe("Home React Content Editor", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "opening a Home recent item does not request leftover ?view=editor",
    { tag: ["@home", "@editor"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        if (/view=editor/.test(req.url())) {
          blocked.push(req.url());
        }
      });
      await page.goto(homeDeepLink(), { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("home-shell")).toBeVisible({
        timeout: 20_000,
      });
      const openBtn = page.locator('[data-testid="home-recent-open"]').first();
      if ((await openBtn.count()) === 0 || !(await openBtn.isVisible())) {
        test.skip(true, "No Home recent/open row available");
        return;
      }
      const popupPromise = page.waitForEvent("popup", { timeout: 10_000 }).catch(() => null);
      await openBtn.click();
      const popup = await popupPromise;
      if (popup) {
        await expect(popup).toHaveURL(/entry=editor|\/editor/);
        await expect(popup).not.toHaveURL(/view=editor/);
      }
      expect(blocked, `leftover editor requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Home Create asset does not request leftover editAsset.jsp",
    { tag: ["@home", "@editor"] },
    async ({ page }) => {
      const blocked = [];
      const consoleErrors = [];
      page.on("request", (req) => {
        const u = req.url();
        if (/editAsset\.jsp/i.test(u) || /view=editor/.test(u)) {
          blocked.push(u);
        }
      });
      page.on("pageerror", (err) => {
        consoleErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      await page.goto(homeDeepLink(), { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("home-shell")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("home-nav-create").click();
      await expect(page.getByTestId("create-type-chooser")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("create-choose-asset").click();
      const empty = page.getByTestId("asset-wizard-empty");
      const wizard = page.getByTestId("asset-wizard");
      await expect(empty.or(wizard)).toBeVisible({ timeout: 20_000 });
      if (await empty.isVisible()) {
        expect(blocked, `leftover asset editor: ${blocked.join(" ")}`).toEqual(
          [],
        );
        return;
      }
      const typeSelect = page.getByTestId("asset-wizard-type");
      const selected = await typeSelect.inputValue();
      if (!selected) {
        const firstReal = typeSelect.locator("option[value]:not([value=''])").first();
        const value = await firstReal.getAttribute("value");
        if (!value) {
          test.skip(true, "No Home Create asset types available");
          return;
        }
        await typeSelect.selectOption(value);
      }
      const popupPromise = page
        .waitForEvent("popup", { timeout: 15_000 })
        .catch(() => null);
      await page.getByTestId("asset-wizard-submit").click();
      let popup = await popupPromise;
      if (!popup) {
        const retry = page.getByTestId("asset-wizard-open-editor");
        if (await retry.isVisible().catch(() => false)) {
          const retryPopup = page
            .waitForEvent("popup", { timeout: 10_000 })
            .catch(() => null);
          await retry.click();
          popup = await retryPopup;
        }
      }
      if (popup) {
        await expect(popup).toHaveURL(/entry=editor|\/editor/);
        await expect(popup).not.toHaveURL(/editAsset\.jsp|view=editor/);
      }
      expect(blocked, `leftover asset editor: ${blocked.join(" ")}`).toEqual([]);
      const unexpected = consoleErrors.filter(
        (t) =>
          !/favicon|404|net::ERR|Failed to load resource/i.test(t) &&
          !/Download the React DevTools/i.test(t),
      );
      expect(
        unexpected,
        `JS console errors: ${unexpected.join(" | ")}`,
      ).toEqual([]);
    },
  );
});
