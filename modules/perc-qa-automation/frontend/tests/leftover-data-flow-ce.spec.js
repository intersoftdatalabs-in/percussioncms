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
 * Leftover Data Flow / CM1 Content Editor HTML is retired (#3473 / parent #2400).
 *
 * Product shells must not request {@code editAsset.jsp}, {@code ?view=editor},
 * {@code checkoutedit.xml}, or {@code contenteditorurls.html}. Bookmarks to
 * {@code editAsset.jsp} / {@code ?view=editor} land on the React editor host.
 *
 * Tags: {@code @editor} {@code @home} {@code @explorer}
 *
 * Run (QA mode after perc-devctl qa-up):
 *   npm run test:surface -- --path tests/leftover-data-flow-ce.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  isLeftoverContentEditorUrl,
} = require("./helpers/explorer-content-editor");

function leftoverRequests(page) {
  const blocked = [];
  page.on("request", (req) => {
    const u = req.url();
    if (isLeftoverContentEditorUrl(u)) {
      blocked.push(u);
    }
  });
  return blocked;
}

test.describe("Leftover Data Flow CE HTML retirement (#3473)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "Home / Explorer / editor SPA shells do not request leftover CE URLs",
    { tag: ["@home", "@explorer", "@editor"] },
    async ({ page }) => {
      const blocked = leftoverRequests(page);
      const consoleErrors = [];
      page.on("pageerror", (err) =>
        consoleErrors.push(String(err && err.message ? err.message : err)),
      );
      const shells = [
        `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home`,
        `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer`,
        `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=editor`,
      ];
      for (const url of shells) {
        await page.goto(`${url}&_=${Date.now()}`, {
          waitUntil: "domcontentloaded",
        });
      }
      expect(blocked, `leftover CE requested: ${blocked.join(" ")}`).toEqual([]);
      const unexpected = consoleErrors.filter(
        (t) => !/Failed to load resource/i.test(t) && !/ResizeObserver/i.test(t),
      );
      expect(unexpected, unexpected.join("\n")).toEqual([]);
    },
  );

  test(
    "editAsset.jsp bookmark lands on React editor host",
    { tag: ["@editor"] },
    async ({ page }) => {
      const consoleErrors = [];
      page.on("pageerror", (err) =>
        consoleErrors.push(String(err && err.message ? err.message : err)),
      );
      await page.goto(
        `${BASE_URL}/Rhythmyx/cm/app/editAsset.jsp?_=${Date.now()}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(page.getByTestId("editor-host")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page).toHaveURL(/entry=editor|\/editor/);
      await expect(page).not.toHaveURL(/editAsset\.jsp/i);
      const unexpected = consoleErrors.filter(
        (t) => !/Failed to load resource/i.test(t) && !/ResizeObserver/i.test(t),
      );
      expect(unexpected, unexpected.join("\n")).toEqual([]);
    },
  );

  test(
    "?view=editor bookmark lands on React editor host",
    { tag: ["@editor"] },
    async ({ page }) => {
      await page.goto(
        `${BASE_URL}/Rhythmyx/cm/app/?view=editor&_=${Date.now()}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(page.getByTestId("editor-host")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page).toHaveURL(/entry=editor|\/editor/);
    },
  );

  test(
    "editor host shows linkback warning when contentId is missing",
    { tag: ["@editor"] },
    async ({ page }) => {
      await page.goto(
        `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=editor&warningMessage=${encodeURIComponent(
          "The page you are attempting to reach, does not exist in the CMS.",
        )}&_=${Date.now()}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(page.getByTestId("editor-error")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.getByTestId("editor-error")).toContainText(
        /does not exist in the CMS/i,
      );
    },
  );
});
