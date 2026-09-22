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
 * Explorer packaged CX view execute + 403/404 results chrome (#4721).
 *
 * {@code npm run test:surface -- --path tests/explorer-cx-view-execute.spec.js}
 */

const { test, expect } = require("@playwright/test");
const {
  BASE_URL,
  adminBasicAuthHeaders,
  loginAsAdmin,
} = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

const CATALOG = {
  ViewDef: [
    { name: "Outbox", label: "Outbox", parentCategory: 1, customView: true },
    { name: "MyCustom", label: "My Custom", parentCategory: 1, customView: true },
    { name: "MissingView", label: "Missing", parentCategory: 1, customView: true },
  ],
};

test.describe("Explorer CX view execute", () => {
  test.beforeEach(async ({ page }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));
    page.pageErrors = pageErrors;
    await page.route("**/services/views", async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(CATALOG),
      });
    });
    await page.route("**/services/views/*/execute", async (route) => {
      const url = route.request().url();
      if (url.includes("/Outbox/")) {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            children: [
              {
                id: "91",
                title: "Sent page",
                folderPath: "/Sites/Demo",
                type: "page",
              },
            ],
            totalCount: 1,
            startIndex: 1,
            viewName: "Outbox",
          }),
        });
        return;
      }
      if (url.includes("/MyCustom/")) {
        await route.fulfill({
          status: 403,
          contentType: "text/plain",
          body: "Admin role required",
        });
        return;
      }
      await route.fulfill({
        status: 404,
        contentType: "text/plain",
        body: "View not found",
      });
    });
  });

  test(
    "Outbox shows a result row; custom and missing views map 403 and 404",
    { tag: ["@explorer-cx-view-execute", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      const outbox = page.getByTestId("explorer-views-leaf-Outbox");
      await expect(outbox).toBeVisible({ timeout: 30_000 });
      await outbox.click();
      await expect(page.getByTestId("explorer-view-open-91")).toBeVisible({
        timeout: 15_000,
      });

      await page.getByTestId("explorer-views-leaf-MyCustom").click();
      const forbidden = page.getByTestId("explorer-view-results-error");
      await expect(forbidden).toHaveAttribute("data-http-status", "403", {
        timeout: 15_000,
      });
      await expect(forbidden).toContainText(/HTTP 403/);

      await page.getByTestId("explorer-views-leaf-MissingView").click();
      const missing = page.getByTestId("explorer-view-results-error");
      await expect(missing).toHaveAttribute("data-http-status", "404");
      await expect(missing).toContainText(/HTTP 404/);
      expect(page.pageErrors).toEqual([]);
    },
  );

  test(
    "REST execute of an unknown view is HTTP 404",
    { tag: ["@explorer-cx-view-execute", "@explorer"] },
    async ({ request }) => {
      const root = String(BASE_URL || "").replace(/\/$/, "");
      const res = await request.post(
        `${root}/Rhythmyx/services/views/${encodeURIComponent("no-such-view-4721")}/execute`,
        {
          headers: {
            ...adminBasicAuthHeaders(),
            Accept: "application/json",
            "Content-Type": "application/json",
          },
          data: { ViewExecuteRequest: { startIndex: 1, maxResults: 5 } },
        },
      );
      expect(res.status()).toBe(404);
    },
  );
});
