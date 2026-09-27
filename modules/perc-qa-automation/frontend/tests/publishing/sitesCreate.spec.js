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
 * PublishingShell create a site (#4959 / parent #4531).
 *
 * Surface filter:
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/sitesCreate.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("PublishingShell create a site", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("creates a named site and opens its workspace; cancel and duplicate do not", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });

    const siteName = `Nightly${Date.now().toString().slice(-8)}`;
    await page.goto(`${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish`);
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 30000,
    });

    await page.getByTestId("publish-sites-create").click();
    await page.getByTestId("publish-site-create-name").fill(" ");
    await expect(page.getByTestId("publish-site-create-save")).toBeDisabled();
    await page.getByTestId("publish-site-create-cancel").click();
    await expect(page.getByTestId("publish-site-create")).toHaveCount(0);
    await expect(page.getByTestId("publish-section-sites")).toBeVisible();

    await page.getByTestId("publish-sites-create").click();
    await page.getByTestId("publish-site-create-name").fill(siteName);
    await page.getByTestId("publish-site-create-save").click();
    await expect(page.getByTestId("publish-site-workspace")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByRole("heading", { name: siteName })).toBeVisible();

    await page.getByRole("button", { name: "Back" }).click();
    await expect(page.getByTestId("publish-section-sites")).toBeVisible();
    await page.getByTestId("publish-sites-create").click();
    await page.getByTestId("publish-site-create-name").fill(siteName);
    await page.getByTestId("publish-site-create-save").click();
    await expect(page.getByTestId("publish-site-create-error")).toBeVisible();
    await expect(page.getByTestId("publish-site-workspace")).toHaveCount(0);
    await expect(page.getByTestId("publish-section-sites")).toBeVisible();

    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
