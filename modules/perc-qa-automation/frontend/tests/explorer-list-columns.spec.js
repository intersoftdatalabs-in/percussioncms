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
 * Explorer session display-format columns (#4722).
 *
 * {@code npm run test:surface -- --path tests/explorer-list-columns.spec.js}
 */

const { test, expect } = require("@playwright/test");
const {
  BASE_URL,
  adminBasicAuthHeaders,
  loginAsAdmin,
} = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

test.describe("Explorer list columns", () => {
  test.beforeEach(async ({ page }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));
    page.pageErrors = pageErrors;
  });

  test(
    "Apply adds a workflow column and a later GET keeps it",
    { tag: ["@explorer-list-columns", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      let saved = [];
      await page.route("**/services/explorer/list-columns**", async (route) => {
        const method = route.request().method();
        if (method === "GET") {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              ExplorerListColumns: {
                folderPath: "/",
                columns: saved,
              },
            }),
          });
          return;
        }
        if (method === "PUT") {
          const body = route.request().postDataJSON();
          const columns = (body && (body.columns || (body.ExplorerListColumns || {}).columns)) || [];
          if (columns.includes("not_a_field")) {
            await route.fulfill({ status: 400, contentType: "text/plain", body: "unknown column" });
            return;
          }
          saved = columns;
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              ExplorerListColumns: { folderPath: body.folderPath || "/", columns: saved },
            }),
          });
          return;
        }
        await route.continue();
      });

      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await page.getByTestId("explorer-list-columns-open").click();
      await page.getByTestId("explorer-list-column-sys_workflow").check();
      await page.getByTestId("explorer-list-columns-apply").click();
      await expect(page.getByTestId("detail-col-header-workflow")).toBeVisible({
        timeout: 15_000,
      });
      expect(page.pageErrors).toEqual([]);
    },
  );

  test(
    "HTTP 400 and 403 are shown on the columns control",
    { tag: ["@explorer-list-columns", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      let mode = "400";
      await page.route("**/services/explorer/list-columns**", async (route) => {
        if (route.request().method() === "GET") {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({ ExplorerListColumns: { folderPath: "/", columns: [] } }),
          });
          return;
        }
        await route.fulfill({
          status: mode === "400" ? 400 : 403,
          contentType: "text/plain",
          body: mode === "400" ? "unknown column" : "Not authorized",
        });
      });
      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await page.getByTestId("explorer-list-columns-open").click();
      await page.getByTestId("explorer-list-columns-apply").click();
      const err = page.getByTestId("explorer-list-columns-error");
      await expect(err).toHaveAttribute("data-http-status", "400", { timeout: 15_000 });
      await expect(err).toContainText(/HTTP 400/);

      mode = "403";
      await page.getByTestId("explorer-list-columns-apply").click();
      await expect(err).toHaveAttribute("data-http-status", "403", { timeout: 15_000 });
      await expect(err).toContainText(/HTTP 403/);
      expect(page.pageErrors).toEqual([]);
    },
  );

  test(
    "REST rejects an unknown column with HTTP 400",
    { tag: ["@explorer-list-columns", "@explorer"] },
    async ({ request }) => {
      const root = String(BASE_URL || "").replace(/\/$/, "");
      const res = await request.put(`${root}/Rhythmyx/services/explorer/list-columns`, {
        headers: {
          ...adminBasicAuthHeaders(),
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        data: {
          ExplorerListColumns: {
            folderPath: "//Sites",
            columns: ["sys_title", "not_a_field"],
          },
        },
      });
      expect(res.status()).toBe(400);
    },
  );
});
