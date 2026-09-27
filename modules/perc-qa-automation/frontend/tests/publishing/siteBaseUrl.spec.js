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
 * PublishingShell edit the open site base URL (#4978 / parent #4531).
 *
 * Surface filter:
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/siteBaseUrl.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("PublishingShell edit the open site base URL", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("save shows the base URL after refresh; cancel, empty, and 409 do not", async ({
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
        text.includes("status of 400") ||
        text.includes("status of 403") ||
        text.includes("status of 409")
      ) {
        return;
      }
      jsErrors.push(text);
    });

    const stamp = Date.now().toString().slice(-8);
    const created = `Base${stamp}`;
    const next = `https://base${stamp}.example/site`;
    let putCalls = 0;
    page.on("request", (req) => {
      const path = new URL(req.url()).pathname;
      if (req.method() === "PUT" && /\/sites\/[^/]+$/.test(path)) {
        putCalls += 1;
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
    await expect(page.getByTestId("publish-site-base-url")).toBeVisible();

    const putsBeforeCancel = putCalls;
    await page.getByTestId("publish-site-base-url-edit").click();
    await page.getByTestId("publish-site-base-url-input").fill("https://discard.example");
    await page.getByTestId("publish-site-base-url-cancel").click();
    await expect(page.getByTestId("publish-site-base-url-form")).toHaveCount(0);
    expect(putCalls).toBe(putsBeforeCancel);

    await page.getByTestId("publish-site-base-url-edit").click();
    await page.getByTestId("publish-site-base-url-input").fill("   ");
    await page.getByTestId("publish-site-base-url-save").click();
    await expect(page.getByTestId("publish-site-base-url-error")).toBeVisible();
    await expect(page.getByTestId("publish-site-base-url-saved")).toHaveCount(0);
    expect(putCalls).toBe(putsBeforeCancel);

    await page.route("**/services/sites/**", async (route) => {
      if (route.request().method() !== "PUT") {
        return route.continue();
      }
      return route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "conflict" }),
      });
    });
    await page.getByTestId("publish-site-base-url-input").fill("https://blocked.example");
    await page.getByTestId("publish-site-base-url-save").click();
    await expect(page.getByTestId("publish-site-base-url-error")).toBeVisible();
    await expect(page.getByTestId("publish-site-base-url-form")).toBeVisible();
    await expect(page.getByTestId("publish-site-base-url-saved")).toHaveCount(0);
    await expect(page.getByTestId("publish-site-base-url")).not.toHaveText(
      "https://blocked.example",
    );
    await page.unrouteAll({ behavior: "ignoreErrors" });

    await page.getByTestId("publish-site-base-url-input").fill(next);
    const putResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "PUT" &&
        /\/sites\/[^/]+$/.test(new URL(res.url()).pathname),
      { timeout: 20000 },
    );
    await page.getByTestId("publish-site-base-url-save").click();
    const saved = await putResponse;
    if (saved.status() !== 200) {
      throw new Error(
        `update HTTP ${saved.status()} ${saved.url()} ${(await saved.text()).slice(0, 400)}`,
      );
    }
    await expect(page.getByTestId("publish-site-base-url")).toHaveText(next);
    await expect(page.getByTestId("publish-site-base-url-saved")).toBeVisible();

    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 20000,
    });
    await page.getByTestId(`publish-site-card-${created}`).click();
    await expect(page.getByTestId("publish-site-base-url")).toHaveText(next, {
      timeout: 20000,
    });

    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
