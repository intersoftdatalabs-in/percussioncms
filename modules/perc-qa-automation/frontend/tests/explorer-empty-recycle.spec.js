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
 * Explorer empty recycle bin (#4762 / parent #4530).
 *
 * Confirms the bin, maps a forced HTTP 409 as an error (row stays), then
 * empties for real and expects the seeded name to leave the list.
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const {
  probePathmanagementContext,
  createNamedFolder,
  recycleFolder,
  contextDownFailureMessage,
} = require("./helpers/folder-recycle-smoke");
const {
  modernExplorerUrl,
  treeNodeSelectors,
} = require("./helpers/explorer-recycle-restore-ui");

const TAGS = ["@explorer-empty-recycle", "@explorer", "@smoke"];

test.describe("Explorer empty recycle bin (#4762)", () => {
  test(
    "confirm, 409 is not success, then empty reloads without the seeded folder",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const headers = adminBasicAuthHeaders();
      const probe = await probePathmanagementContext(request, BASE_URL, headers);
      expect(probe.ok, contextDownFailureMessage(probe)).toBeTruthy();

      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") jsErrors.push(msg.text());
      });

      const seeded = await createNamedFolder(request, BASE_URL, headers, {
        parentPath: "Assets",
        name: `qa4762empty${Date.now()}`,
      });
      expect(seeded && seeded.name, "seed folder").toBeTruthy();
      await recycleFolder(request, BASE_URL, headers, seeded);

      await loginAsAdmin(page);
      await page.goto(modernExplorerUrl(BASE_URL), {
        waitUntil: "domcontentloaded",
      });
      await page.getByTestId("content-explorer-shell").waitFor({ timeout: 60_000 });
      await page.getByTestId("explorer-nav").waitFor({ timeout: 60_000 });

      let clicked = false;
      for (const sel of treeNodeSelectors("/Recycling")) {
        const node = page.locator(sel).first();
        if ((await node.count()) > 0) {
          await node.click({ timeout: 15_000 });
          clicked = true;
          break;
        }
      }
      expect(clicked, "Recycling tree node").toBeTruthy();
      const nameLoc = page.getByText(seeded.name, { exact: true });
      const sawName = await nameLoc
        .first()
        .isVisible()
        .catch(() => false);

      const emptyBtn = page.getByTestId("action-empty-recycle");
      await expect(emptyBtn).toBeEnabled();

      await page.route("**/folders/recycle/empty", (route) =>
        route.fulfill({
          status: 409,
          contentType: "text/plain",
          body: "conflict",
        }),
      );
      page.once("dialog", (dialog) => dialog.accept());
      await emptyBtn.click();
      await expect(page.getByRole("alert")).toContainText(/could not empty/i, {
        timeout: 20_000,
      });
      if (sawName) {
        await expect(nameLoc.first()).toBeVisible();
      } else {
        await expect(page.getByText(/No items in this folder/i)).toBeVisible();
      }
      await page.unroute("**/folders/recycle/empty");

      const emptied = page.waitForResponse(
        (res) =>
          res.url().includes("/folders/recycle/empty") &&
          res.request().method() === "POST" &&
          res.status() === 200,
        { timeout: 60_000 },
      );
      page.once("dialog", (dialog) => dialog.accept());
      await emptyBtn.click();
      await emptied;
      await expect(nameLoc).toHaveCount(0, { timeout: 30_000 });
      await expect(page.getByText(/No items in this folder/i)).toBeVisible({
        timeout: 30_000,
      });

      const unexpected = jsErrors.filter(
        (line) => !/favicon|Failed to load resource/i.test(line),
      );
      expect(unexpected, unexpected.join("\n")).toEqual([]);
    },
  );
});
