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
 * Explorer view-results Next / Previous (#4930 / parent #4530).
 *
 * The H2 catalog rarely has 50+ rows in one view, so this spec fulfills
 * POST .../views/{name}/execute from the SPA while the shell, login, and
 * Views tree stay on the live QA cell.
 *
 * {@code npm run test:surface -- --path tests/explorer-view-results-paging.spec.js}
 */

const { test, expect } = require("@playwright/test");
const { BASE_URL, loginAsAdmin } = require("./helpers/auth");
const { explorerEntryUrl } = require("./helpers/explorer-views-catalog");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

function pageBody(startIndex, count, totalCount) {
  const children = [];
  for (let i = 0; i < count; i += 1) {
    const n = startIndex + i;
    children.push({
      id: String(n),
      name: `page-${n}`,
      title: `Paged row ${n}`,
      folderPath: "/Sites/Demo",
      type: "page",
    });
  }
  return {
    ViewExecuteResult: {
      children,
      totalCount,
      startIndex,
      viewName: "Inbox",
    },
  };
}

test.describe("Explorer view results paging (#4930)", () => {
  test(
    "Next and Previous replace the view results page",
    { tag: ["@explorer-view-paging", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") return;
        const text = msg.text();
        if (
          /Failed to load resource: the server responded with a status of (404|400|403|500)/i.test(
            text,
          )
        ) {
          return;
        }
        consoleErrors.push(text);
      });

      const requested = [];
      await page.route("**/services/views/**/execute", async (route) => {
        const req = route.request();
        if (req.method() !== "POST") {
          await route.continue();
          return;
        }
        let startIndex = 1;
        try {
          const raw = req.postDataJSON();
          const body = raw && raw.ViewExecuteRequest ? raw.ViewExecuteRequest : raw;
          if (body && typeof body.startIndex === "number") {
            startIndex = body.startIndex;
          }
        } catch {
          startIndex = 1;
        }
        requested.push(startIndex);
        if (startIndex === 1) {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(pageBody(1, 50, 60)),
          });
          return;
        }
        if (startIndex === 51) {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(pageBody(51, 0, 60)),
          });
          return;
        }
        await route.fulfill({
          status: 500,
          contentType: "application/json",
          body: JSON.stringify({ message: "forced paging failure" }),
        });
      });

      await loginAsAdmin(page);
      await page.goto(explorerEntryUrl(BASE_URL), { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("content-explorer-shell")).toBeVisible({
        timeout: 30_000,
      });
      for (const cat of [1, 2, 3, 4]) {
        const group = page.getByTestId(`explorer-views-group-${cat}`);
        const row = page.getByTestId(`explorer-views-group-${cat}-row`);
        if ((await group.count()) === 0) continue;
        const expanded = await row.getAttribute("aria-expanded");
        if (expanded !== "true") {
          await row.click();
        }
      }
      const leaf = page.getByTestId("explorer-views-leaf-Inbox");
      await expect(leaf).toBeVisible({ timeout: 20_000 });
      await leaf.click();

      const results = page.getByTestId("explorer-view-results");
      await expect(results).toBeVisible({ timeout: 30_000 });
      await expect(page.getByTestId("explorer-view-results-next")).toBeVisible();
      await expect(page.getByTestId("explorer-view-results-previous")).toHaveCount(0);
      await expect(page.getByTestId("explorer-view-open-1")).toBeVisible();

      await page.getByTestId("explorer-view-results-next").click();
      await expect(page.getByTestId("explorer-view-results-empty")).toBeVisible({
        timeout: 15_000,
      });
      await expect(page.getByTestId("explorer-view-results-error")).toHaveCount(0);
      await expect(page.getByTestId("explorer-view-results-next")).toHaveCount(0);
      await expect(page.getByTestId("explorer-view-results-previous")).toBeVisible();
      expect(requested).toContain(51);

      await page.getByTestId("explorer-view-results-previous").click();
      await expect(page.getByTestId("explorer-view-open-1")).toBeVisible({
        timeout: 15_000,
      });
      await expect(page.getByTestId("explorer-view-results-previous")).toHaveCount(0);

      await page.unroute("**/services/views/**/execute");
      await page.route("**/services/views/**/execute", async (route) => {
        if (route.request().method() !== "POST") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 500,
          contentType: "application/json",
          body: JSON.stringify({ message: "forced paging failure" }),
        });
      });
      await page.getByTestId("explorer-view-results-next").click();
      const err = page.getByTestId("explorer-view-results-error");
      await expect(err).toBeVisible({ timeout: 15_000 });
      await expect(page.getByTestId("explorer-view-open-1")).toBeVisible();
      await expect(results).toHaveAttribute("data-start-index", "1");

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-view-results-pager"]',
      });
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
    },
  );
});
