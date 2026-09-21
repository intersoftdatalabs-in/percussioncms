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
 * PublishingShell Design — save content list (#4676 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContentListSave.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

test.describe("PublishingShell Design content-list save", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("opens Design content-list editor and saves with unique name", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });

    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design`,
    );
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-section-design")).toBeVisible();

    await page.getByRole("tab", { name: /Content lists/i }).click();
    await page.getByTestId("design-add-content-list").click();
    await expect(page.getByTestId("contentlist-editor")).toBeVisible();

    const name = `NightCl-${Date.now()}`;
    await page.locator("#cl-name").fill(name);
    await page.getByTestId("contentlist-save").click();

    await expect(page.getByTestId("contentlist-editor")).toBeHidden({
      timeout: 20000,
    });
    await expect(page.getByRole("button", { name })).toBeVisible({
      timeout: 20000,
    });
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("surfaces HTTP 409 on duplicate content-list save", async ({ page }) => {
    await page.route(
      "**/services/sitemanage/publishingdesign/contentlists",
      (route) => {
        if (route.request().method() === "POST") {
          return route.fulfill({
            status: 409,
            contentType: "application/json",
            body: JSON.stringify({ message: "Content list name already exists" }),
          });
        }
        return route.continue();
      },
    );

    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design`,
    );
    await expect(page.getByTestId("publish-section-design")).toBeVisible({
      timeout: 30000,
    });
    await page.getByRole("tab", { name: /Content lists/i }).click();
    await page.getByTestId("design-add-content-list").click();
    await page.locator("#cl-name").fill("DupFromQa");
    await page.getByTestId("contentlist-save").click();
    await expect(page.getByRole("alert")).toContainText(
      /already exists|409|Conflict/i,
    );
  });
});
