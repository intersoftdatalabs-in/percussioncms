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
 * Explorer executes a non-Inbox custom URL view against the live H2 catalog
 * (#4834 / parent #4530). Does not stub GET /services/views.
 *
 * {@code npm run test:surface -- --path tests/explorer-custom-url-view.spec.js}
 */

const { test, expect } = require("@playwright/test");
const {
  BASE_URL,
  adminBasicAuthHeaders,
  loginAsAdmin,
} = require("./helpers/auth");
const { explorerEntryUrl } = require("./helpers/explorer-views-catalog");
const {
  unwrapViewDefs,
  pickNonInboxCustomView,
  isNamedViewExecute,
} = require("./helpers/explorer-custom-url-view");

const STUB = /Custom URL views cannot be run/i;

test.describe("Explorer non-Inbox custom URL view (#4834)", () => {
  test(
    "selecting a catalog custom URL view shows rows or an explicit error",
    { tag: ["@explorer-custom-url-view", "@explorer"] },
    async ({ page, request }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") return;
        const text = msg.text();
        if (/Failed to load resource: the server responded with a status of (404|400|403)/i.test(text)) {
          return;
        }
        consoleErrors.push(text);
      });

      const root = String(BASE_URL || "").replace(/\/$/, "");
      const listed = await request.get(`${root}/Rhythmyx/services/views`, {
        headers: {
          ...adminBasicAuthHeaders(),
          Accept: "application/json",
        },
      });
      expect(listed.status(), await listed.text()).toBe(200);
      const preferred = pickNonInboxCustomView(unwrapViewDefs(await listed.json()));
      expect(
        preferred,
        "H2 catalog must include a non-Inbox custom URL view (Outbox)",
      ).toBeTruthy();
      const viewName = String(preferred.name).trim();

      await loginAsAdmin(page);
      await page.goto(explorerEntryUrl(BASE_URL), { waitUntil: "networkidle" });
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
      const leaf = page.getByTestId(`explorer-views-leaf-${viewName}`);
      await expect(leaf).toBeVisible({ timeout: 20_000 });

      const executeWait = page.waitForResponse(
        (res) =>
          res.request().method() === "POST" &&
          isNamedViewExecute(res.url(), viewName),
        { timeout: 30_000 },
      );
      await leaf.click();
      const executeResponse = await executeWait;
      const status = executeResponse.status();

      const results = page.getByTestId("explorer-view-results");
      await expect(results).toBeVisible({ timeout: 30_000 });
      await expect(results).not.toContainText(STUB);
      await expect(page.getByTestId("explorer-view-results-loading")).toHaveCount(0);
      await expect(page.getByTestId("detail-col-header-icon")).toHaveCount(0);
      await expect(page.getByTestId("detail-list-empty")).toHaveCount(0);

      const empty = page.getByTestId("explorer-view-results-empty");
      const error = page.getByTestId("explorer-view-results-error");
      const list = page.getByTestId("explorer-view-results-list");
      if (status >= 200 && status < 300) {
        await expect(error).toHaveCount(0);
        await expect(list.or(empty)).toHaveCount(1);
      } else {
        await expect(error).toBeVisible();
        await expect(error).toContainText(new RegExp(`HTTP ${status}`));
        await expect(list).toHaveCount(0);
        await expect(empty).toHaveCount(0);
      }
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
    },
  );
});
