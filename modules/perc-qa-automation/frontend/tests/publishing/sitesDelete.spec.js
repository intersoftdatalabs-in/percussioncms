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
 * PublishingShell delete the open site (#4976 / parent #4531).
 *
 * Surface filter:
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesDelete.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("PublishingShell delete the open site", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("confirm removes the site from the list; cancel and errors stay", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (
        text.includes("status of 403") ||
        text.includes("status of 404") ||
        text.includes("status of 409")
      ) {
        return;
      }
      jsErrors.push(text);
    });

    const stamp = Date.now().toString().slice(-8);
    const created = `DelSrc${stamp}`;
    let deleteCalls = 0;
    page.on("request", (req) => {
      if (req.method() === "DELETE" && /\/sites\/[^/]+$/.test(new URL(req.url()).pathname)) {
        deleteCalls += 1;
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

    const deletesBeforeCancel = deleteCalls;
    await page.getByTestId("publish-site-delete-open").click();
    await page.getByTestId("publish-site-delete-cancel").click();
    await expect(page.getByTestId("publish-site-delete")).toHaveCount(0);
    await expect(page.getByTestId("publish-site-title")).toHaveText(created);
    expect(deleteCalls).toBe(deletesBeforeCancel);

    await page.route("**/services/sites/**", async (route) => {
      if (route.request().method() !== "DELETE") {
        return route.continue();
      }
      return route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "in use" }),
      });
    });
    await page.getByTestId("publish-site-delete-open").click();
    await page.getByTestId("publish-site-delete-confirm").click();
    await expect(page.getByTestId("publish-site-delete-error")).toBeVisible();
    await expect(page.getByTestId("publish-site-title")).toHaveText(created);
    await expect(page.getByTestId("publish-site-workspace")).toBeVisible();
    await page.unrouteAll({ behavior: "ignoreErrors" });

    const deleteResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "DELETE" &&
        /\/sites\/[^/]+$/.test(new URL(res.url()).pathname),
      { timeout: 20000 },
    );
    await page.getByTestId("publish-site-delete-confirm").click();
    const deleted = await deleteResponse;
    if (deleted.status() !== 204 && deleted.status() !== 200) {
      throw new Error(
        `delete HTTP ${deleted.status()} ${deleted.url()} ${(await deleted.text()).slice(0, 400)}`,
      );
    }
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 20000,
    });
    await expect(page.getByTestId("publish-site-workspace")).toHaveCount(0);
    await expect(page.getByTestId(`publish-site-card-${created}`)).toHaveCount(0);

    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
