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
 * PublishingShell rename the open site (#4960 / parent #4531).
 *
 * Surface filter:
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesRename.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("PublishingShell rename the open site", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("confirm updates the title and the sites list; cancel and errors stay", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      // The conflict case is an expected HTTP 409; Chromium logs the failed resource.
      if (text.includes("status of 409")) {
        return;
      }
      jsErrors.push(text);
    });

    const stamp = Date.now().toString().slice(-8);
    const created = `RenSrc${stamp}`;
    const renamed = `RenDst${stamp}`;
    let renamePosts = 0;
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/sites/") && req.url().includes("/rename")) {
        renamePosts += 1;
      }
    });

    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish`);
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 30000,
    });
    await page.getByTestId("publish-sites-create").click();
    await page.getByTestId("publish-site-create-name").fill(created);
    await page.getByTestId("publish-site-create-save").click();
    await expect(page.getByTestId("publish-site-workspace")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-site-title")).toHaveText(created);

    const postsBeforeCancel = renamePosts;
    await page.getByTestId("publish-site-rename-open").click();
    await page.getByTestId("publish-site-rename-name").fill(" ");
    await expect(page.getByTestId("publish-site-rename-save")).toBeDisabled();
    await page.getByTestId("publish-site-rename-cancel").click();
    await expect(page.getByTestId("publish-site-rename")).toHaveCount(0);
    await expect(page.getByTestId("publish-site-title")).toHaveText(created);
    expect(renamePosts).toBe(postsBeforeCancel);

    let renameStatus = 409;
    await page.route("**/sites/**/rename", async (route) => {
      if (route.request().method() !== "POST") {
        return route.continue();
      }
      if (renameStatus === 200) {
        return route.continue();
      }
      return route.fulfill({
        status: renameStatus,
        contentType: "application/json",
        body: JSON.stringify({ message: "exists" }),
      });
    });
    await page.getByTestId("publish-site-rename-open").click();
    await page.getByTestId("publish-site-rename-name").fill(`Clash${stamp}`);
    await page.getByTestId("publish-site-rename-save").click();
    await expect(page.getByTestId("publish-site-rename-error")).toBeVisible();
    await expect(page.getByTestId("publish-site-title")).toHaveText(created);
    await expect(page.getByTestId("publish-site-workspace")).toBeVisible();
    await page.unrouteAll({ behavior: "ignoreErrors" });

    await page.getByTestId("publish-site-rename-name").fill(renamed);
    const renameResponse = page.waitForResponse(
      (res) => res.request().method() === "POST" && res.url().includes("/rename"),
      { timeout: 20000 },
    );
    await page.getByTestId("publish-site-rename-save").click();
    const posted = await renameResponse;
    if (posted.status() !== 200) {
      throw new Error(
        `rename HTTP ${posted.status()} ${posted.url()} ${(await posted.text()).slice(0, 400)}`,
      );
    }
    await expect(page.getByTestId("publish-site-title")).toHaveText(renamed, {
      timeout: 20000,
    });
    await expect(page.getByTestId("publish-site-rename")).toHaveCount(0);

    await page.getByRole("button", { name: "Back" }).click();
    await expect(page.getByTestId("publish-section-sites")).toBeVisible();
    await expect(page.getByTestId(`publish-site-card-${renamed}`)).toBeVisible();

    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
