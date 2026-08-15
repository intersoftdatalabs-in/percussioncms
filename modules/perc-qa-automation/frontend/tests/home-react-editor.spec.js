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
});
