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
 * PublishingShell Design — delete a content list (#5135 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContentListDelete.spec.js
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

async function openDesignContentLists(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
  await page.getByRole("tab", { name: /Content lists/i }).click();
  await expect(page.getByTestId("design-add-content-list")).toBeVisible();
}

async function createContentList(page, name) {
  await page.getByTestId("design-add-content-list").click();
  await expect(page.getByTestId("contentlist-editor")).toBeVisible();
  await page.locator("#cl-name").fill(name);
  const createResponse = page.waitForResponse(
    (res) =>
      res.request().method() === "POST" &&
      /\/publishingdesign\/contentlists$/.test(new URL(res.url()).pathname),
    { timeout: 30000 },
  );
  await page.getByTestId("contentlist-save").click();
  const posted = await createResponse;
  if (posted.status() !== 200 && posted.status() !== 201) {
    throw new Error(
      `create HTTP ${posted.status()} ${posted.url()} ${(await posted.text()).slice(0, 400)}`,
    );
  }
  await expect(page.getByTestId("contentlist-editor")).toBeHidden({
    timeout: 20000,
  });
  const row = page.getByRole("button", { name, exact: true });
  await expect(row).toBeVisible({ timeout: 20000 });
  return row;
}

test.describe("PublishingShell Design delete content list", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("deletes an unassociated content list after confirm and removes it", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openDesignContentLists(page);
    const name = `NightDel${Date.now().toString().slice(-8)}`;
    await createContentList(page, name);
    await page.getByRole("button", { name, exact: true }).click();
    await expect(page.getByTestId("contentlist-delete")).toBeVisible();

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      expect(dialog.message()).toMatch(/Delete this design object/i);
      await dialog.accept();
    });
    const deleteResponse = page.waitForResponse(
      (res) =>
        res.request().method() === "DELETE" &&
        /\/publishingdesign\/contentlists\/[^/]+$/.test(
          new URL(res.url()).pathname,
        ),
      { timeout: 30000 },
    );
    await page.getByTestId("contentlist-delete").click();
    const deleted = await deleteResponse;
    if (deleted.status() !== 200 && deleted.status() !== 204) {
      throw new Error(
        `delete HTTP ${deleted.status()} ${deleted.url()} ${(await deleted.text()).slice(0, 400)}`,
      );
    }
    await expect(page.getByTestId("contentlist-editor")).toBeHidden({
      timeout: 20000,
    });
    await expect(page.getByRole("button", { name, exact: true })).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("cancel leaves the content list on the list", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openDesignContentLists(page);
    const name = `NightKeep${Date.now().toString().slice(-8)}`;
    await createContentList(page, name);
    await page.getByRole("button", { name, exact: true }).click();
    let deletes = 0;
    await page.route(
      "**/services/sitemanage/publishingdesign/contentlists/**",
      (route) => {
        if (route.request().method() === "DELETE") {
          deletes += 1;
        }
        return route.continue();
      },
    );
    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      await dialog.dismiss();
    });
    await page.getByTestId("contentlist-delete").click();
    await expect(page.getByTestId("contentlist-editor")).toBeVisible();
    expect(deletes).toBe(0);
    await page.getByRole("button", { name: /Back/i }).click();
    await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("surfaces HTTP 400, 403, and 409 without removing the content list", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    await openDesignContentLists(page);
    const name = `NightErr${Date.now().toString().slice(-8)}`;
    await createContentList(page, name);
    await page.getByRole("button", { name, exact: true }).click();

    async function stubDelete(status, message) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        "**/services/sitemanage/publishingdesign/contentlists/**",
        (route) => {
          if (route.request().method() !== "DELETE") {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      page.once("dialog", (dialog) => dialog.accept());
      await page.getByTestId("contentlist-delete").click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect(page.getByTestId("contentlist-editor")).toBeVisible();
    }

    await stubDelete(400, "contentListId is required");
    await stubDelete(403, "Admin or Designer role required to save a publish edition");
    await stubDelete(409, "Content list is in use");
    await page.getByRole("button", { name: /Back/i }).click();
    await expect(page.getByRole("button", { name, exact: true })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
