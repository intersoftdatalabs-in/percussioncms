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
 * PublishingShell Design — create an edition (#5082 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionCreate.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function trackJsErrors(page) {
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
  return jsErrors;
}

async function openDesignEditions(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
  await page.getByRole("tab", { name: /Editions/i }).click();
  await expect(page.getByTestId("design-add-edition")).toBeVisible();
}

test.describe("PublishingShell Design create edition", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("creates an edition on the open site and lists it", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openDesignEditions(page);
    const site = page.getByLabel("Design site");
    const siteId = await site.inputValue();
    expect(siteId.trim().length).toBeGreaterThan(0);

    const name = `NightNew${Date.now().toString().slice(-8)}`;
    await page.getByTestId("design-add-edition").click();
    await expect(page.getByTestId("edition-editor")).toBeVisible();
    await expect(page.getByRole("heading", { name: "Create edition" })).toBeVisible();
    await page.locator("#ed-name").fill(name);

    const createResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "POST" &&
        /\/publishingdesign\/editions$/.test(new URL(res.url()).pathname),
      { timeout: 30000 },
    );
    await page.getByTestId("edition-save").click();
    const posted = await createResponse;
    if (posted.status() !== 200 && posted.status() !== 201) {
      throw new Error(
        `create HTTP ${posted.status()} ${posted.url()} ${(await posted.text()).slice(0, 400)}`,
      );
    }
    const payload = posted.request().postDataJSON();
    const body = payload && payload.edition ? payload.edition : payload;
    expect(String(body.siteId)).toBe(siteId);
    expect(body.name).toBe(name);

    await expect(page.getByTestId("edition-editor")).toBeHidden({
      timeout: 20000,
    });
    const row = page.getByRole("button", { name, exact: true });
    await expect(row).toBeVisible({ timeout: 20000 });
    await row.click();
    await expect(page.locator("#ed-name")).toHaveValue(name);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("keeps invalid names in the editor without posting", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openDesignEditions(page);
    let posts = 0;
    await page.route("**/services/sitemanage/publishingdesign/editions", (route) => {
      if (route.request().method() === "POST") {
        posts += 1;
      }
      return route.continue();
    });

    await page.getByTestId("design-add-edition").click();
    await page.locator("#ed-name").fill("   ");
    await page.getByTestId("edition-save").click();
    await expect(page.getByRole("alert")).toContainText("Name is required");
    await expect(page.getByTestId("edition-editor")).toBeVisible();

    await page.locator("#ed-name").fill("N".repeat(101));
    await page.getByTestId("edition-save").click();
    await expect(page.getByRole("alert")).toContainText(
      "Edition name must be 100 characters or fewer",
    );
    await expect(page.getByTestId("edition-editor")).toBeVisible();
    expect(posts).toBe(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("surfaces HTTP 400 and 403 without leaving the editor", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openDesignEditions(page);
    await page.getByTestId("design-add-edition").click();
    await page.locator("#ed-name").fill("WillNotCreate");

    await page.route("**/services/sitemanage/publishingdesign/editions", (route) => {
      if (route.request().method() !== "POST") {
        return route.continue();
      }
      return route.fulfill({
        status: 400,
        contentType: "application/json",
        body: JSON.stringify({ message: "name and siteId are required" }),
      });
    });
    await page.getByTestId("edition-save").click();
    await expect(page.getByRole("alert")).toContainText(/required|400|Bad Request/i);
    await expect(page.getByTestId("edition-editor")).toBeVisible();

    await page.unrouteAll({ behavior: "ignoreErrors" });
    await page.route("**/services/sitemanage/publishingdesign/editions", (route) => {
      if (route.request().method() !== "POST") {
        return route.continue();
      }
      return route.fulfill({
        status: 403,
        contentType: "application/json",
        body: JSON.stringify({
          message: "Admin or Designer role required to save a publish edition",
        }),
      });
    });
    await page.getByTestId("edition-save").click();
    await expect(page.getByRole("alert")).toContainText(/Admin or Designer|403|Forbidden/i);
    await expect(page.getByTestId("edition-editor")).toBeVisible();
    await page.getByRole("button", { name: /Back/i }).click();
    await expect(
      page.getByRole("button", { name: "WillNotCreate", exact: true }),
    ).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
