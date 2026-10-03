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
 * PublishingShell Design — copy an edition (#5081 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionCopy.spec.js
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
}

async function createEdition(page, name) {
  await page.getByTestId("design-add-edition").click();
  await expect(page.getByTestId("edition-editor")).toBeVisible();
  await page.locator("#ed-name").fill(name);
  await page.getByTestId("edition-save").click();
  await expect(page.getByTestId("edition-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
}

test.describe("PublishingShell Design copy edition", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("copies an edition onto the same site and opens the new row", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openDesignEditions(page);
    const stamp = Date.now().toString().slice(-8);
    const source = `CpySrc${stamp}`;
    const copied = `CpyDst${stamp}`;
    await createEdition(page, source);
    await page.getByRole("button", { name: source, exact: true }).click();
    await expect(page.getByTestId("edition-editor")).toBeVisible();
    await page.locator("#copy-name").fill(copied);
    const copyResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "POST" &&
        res.url().includes("/publishingdesign/editions/copy"),
      { timeout: 30000 },
    );
    await page.getByTestId("edition-copy").click();
    const posted = await copyResponse;
    if (posted.status() !== 200 && posted.status() !== 204) {
      throw new Error(
        `copy HTTP ${posted.status()} ${posted.url()} ${(await posted.text()).slice(0, 400)}`,
      );
    }
    await expect(page.getByTestId("edition-editor")).toBeHidden({
      timeout: 20000,
    });
    const copiedRow = page.getByRole("button", { name: copied, exact: true });
    await expect(copiedRow).toBeVisible({ timeout: 20000 });
    await copiedRow.click();
    await expect(page.getByTestId("edition-editor")).toBeVisible();
    await expect(page.locator("#ed-name")).toHaveValue(copied);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual(
      [],
    );
  });

  test("surfaces HTTP 400 and 403 without leaving the editor", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openDesignEditions(page);
    const source = `CpyErr${Date.now().toString().slice(-8)}`;
    await createEdition(page, source);
    await page.getByRole("button", { name: source, exact: true }).click();
    await expect(page.getByTestId("edition-copy")).toBeVisible();

    await page.route(
      "**/services/sitemanage/publishingdesign/editions/copy",
      (route) => {
        if (route.request().method() !== "POST") {
          return route.continue();
        }
        return route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({
            message: "sourceEditionId and targetSiteId are required",
          }),
        });
      },
    );
    await page.locator("#copy-name").fill("WillNotCopy");
    await page.getByTestId("edition-copy").click();
    await expect(page.getByRole("alert")).toContainText(/required|400|Bad Request/i);
    await expect(page.getByTestId("edition-editor")).toBeVisible();

    await page.unrouteAll({ behavior: "ignoreErrors" });
    await page.route(
      "**/services/sitemanage/publishingdesign/editions/copy",
      (route) => {
        if (route.request().method() !== "POST") {
          return route.continue();
        }
        return route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({
            message: "Admin or Designer role required",
          }),
        });
      },
    );
    await page.getByTestId("edition-copy").click();
    await expect(page.getByRole("alert")).toContainText(
      /Admin or Designer|403|Forbidden/i,
    );
    await expect(page.getByTestId("edition-editor")).toBeVisible();
    await page.getByRole("button", { name: /Back/i }).click();
    await expect(page.getByRole("button", { name: source, exact: true })).toBeVisible();
    await expect(
      page.getByRole("button", { name: "WillNotCopy", exact: true }),
    ).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual(
      [],
    );
  });
});
