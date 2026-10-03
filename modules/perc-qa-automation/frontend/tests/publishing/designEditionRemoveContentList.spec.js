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
 * PublishingShell Design — remove a content list from an edition (#5108 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionRemoveContentList.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const DELETE_ONE =
  /\/services\/sitemanage\/publishingdesign\/editions\/[^/]+\/contentlists\/[^/]+$/;

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

async function openDesign(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
}

async function createContentList(page, name) {
  await page.getByRole("tab", { name: /Content lists/i }).click();
  await page.getByTestId("design-add-content-list").click();
  await expect(page.getByTestId("contentlist-editor")).toBeVisible();
  await page.locator("#cl-name").fill(name);
  await page.getByTestId("contentlist-save").click();
  await expect(page.getByTestId("contentlist-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name })).toBeVisible({
    timeout: 20000,
  });
}

async function createAndOpenEdition(page, name) {
  await page.getByRole("tab", { name: /Editions/i }).click();
  await page.getByTestId("design-add-edition").click();
  await expect(page.getByTestId("edition-editor")).toBeVisible();
  await page.locator("#ed-name").fill(name);
  await page.getByTestId("edition-save").click();
  await expect(page.getByTestId("edition-editor")).toBeHidden({
    timeout: 20000,
  });
  await page.getByRole("button", { name }).click();
  await expect(page.getByTestId("edition-editor")).toBeVisible();
  await expect(page.getByTestId("edition-assoc-empty")).toBeVisible();
}

async function chooseFirstContext(page) {
  const context = page.getByTestId("edition-assoc-context");
  await expect
    .poll(
      async () =>
        context.locator("option").evaluateAll((opts) => {
          const found = opts.map((opt) => opt.value).find((id) => id);
          return found || "";
        }),
      { timeout: 20000 },
    )
    .not.toEqual("");
  const value = await context.locator("option").evaluateAll((opts) => {
    const found = opts.map((opt) => opt.value).find((id) => id);
    return found || "";
  });
  await context.selectOption(value);
}

async function associateList(page, listName) {
  await page.getByTestId("edition-assoc-content-list").selectOption({
    label: listName,
  });
  await chooseFirstContext(page);
  await page.getByTestId("edition-associate").click();
  await expect(page.getByTestId("edition-assoc-list")).toContainText(listName, {
    timeout: 20000,
  });
}

function removeButton(page) {
  return page.locator('[data-testid^="edition-disassociate-"]');
}

test.describe("PublishingShell Design remove content list from edition", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("drops the content list only after remove succeeds", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const listName = `NightRm-${stamp}`;
    const editionName = `NightEdRm-${stamp}`;
    await openDesign(page);
    await createContentList(page, listName);
    await createAndOpenEdition(page, editionName);
    await associateList(page, listName);

    let releaseDelete;
    const deleteGate = new Promise((resolve) => {
      releaseDelete = resolve;
    });
    let markDeleted;
    const deleted = new Promise((resolve) => {
      markDeleted = resolve;
    });
    await page.route(DELETE_ONE, async (route) => {
      if (route.request().method() !== "DELETE") {
        await route.continue();
        return;
      }
      markDeleted();
      await deleteGate;
      await route.continue();
    });

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      expect(dialog.message()).toMatch(
        /Remove this content list from the edition/i,
      );
      await dialog.accept();
    });
    await removeButton(page).click();
    await deleted;
    await expect(page.getByTestId("edition-assoc-list")).toContainText(listName);
    releaseDelete();
    await expect(page.getByTestId("edition-assoc-empty")).toBeVisible({
      timeout: 20000,
    });
    await expect(page.getByRole("alert")).toHaveCount(0);
    await page.getByRole("tab", { name: /Content lists/i }).click();
    await expect(page.getByRole("button", { name: listName })).toBeVisible();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("cancel does not call the server", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const listName = `NightRmKeep-${stamp}`;
    const editionName = `NightEdKeep-${stamp}`;
    let deletes = 0;
    await page.route(DELETE_ONE, async (route) => {
      if (route.request().method() === "DELETE") {
        deletes += 1;
      }
      await route.continue();
    });
    await openDesign(page);
    await createContentList(page, listName);
    await createAndOpenEdition(page, editionName);
    await associateList(page, listName);

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      await dialog.dismiss();
    });
    await removeButton(page).click();
    await expect(page.getByTestId("edition-assoc-list")).toContainText(listName);
    expect(deletes).toBe(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 do not remove the content list", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const listName = `NightRmErr-${stamp}`;
    const editionName = `NightEdRmErr-${stamp}`;
    await openDesign(page);
    await createContentList(page, listName);
    await createAndOpenEdition(page, editionName);
    await associateList(page, listName);

    const cases = [
      [400, "contentListId is required"],
      [403, "Admin or Designer role required to save a publish edition"],
      [409, "Edition is in use"],
    ];
    for (const [status, message] of cases) {
      await page.unroute(DELETE_ONE);
      await page.route(DELETE_ONE, async (route) => {
        if (route.request().method() === "DELETE") {
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
          return;
        }
        await route.continue();
      });
      page.once("dialog", (dialog) => dialog.accept());
      await removeButton(page).click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect(page.getByTestId("edition-assoc-list")).toContainText(listName);
    }
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
