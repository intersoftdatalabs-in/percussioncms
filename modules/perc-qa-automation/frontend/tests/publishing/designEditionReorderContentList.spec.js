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
 * PublishingShell Design — reorder a content list on an edition (#5184 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionReorderContentList.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const REORDER =
  /\/services\/sitemanage\/publishingdesign\/editions\/[^/]+\/contentlists\/[^/]+\/sequence$/;

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

async function assocNames(page) {
  return page.locator('[data-testid^="edition-assoc-name-"]').allTextContents();
}

test.describe("PublishingShell Design reorder content list on edition", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("moves the content list only after reorder succeeds", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const first = `NightOrdA-${stamp}`;
    const second = `NightOrdB-${stamp}`;
    const editionName = `NightEdOrd-${stamp}`;
    await openDesign(page);
    await createContentList(page, first);
    await createContentList(page, second);
    await createAndOpenEdition(page, editionName);
    await associateList(page, first);
    await associateList(page, second);
    await expect.poll(async () => assocNames(page)).toEqual([first, second]);
    await expect(page.locator('[data-testid^="edition-move-up-"]').first()).toBeDisabled();
    await expect(page.locator('[data-testid^="edition-move-down-"]').last()).toBeDisabled();

    let releasePut;
    const putGate = new Promise((resolve) => {
      releasePut = resolve;
    });
    let markSent;
    const sent = new Promise((resolve) => {
      markSent = resolve;
    });
    await page.route(REORDER, async (route) => {
      if (route.request().method() !== "PUT") {
        await route.continue();
        return;
      }
      markSent();
      await putGate;
      await route.continue();
    });

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      expect(dialog.message()).toMatch(/Move this content list up in the edition/i);
      await dialog.accept();
    });
    await page.locator('[data-testid^="edition-move-up-"]').nth(1).click();
    await sent;
    await expect.poll(async () => assocNames(page)).toEqual([first, second]);
    releasePut();
    await expect.poll(async () => assocNames(page), { timeout: 20000 }).toEqual([
      second,
      first,
    ]);
    await expect(page.getByRole("alert")).toHaveCount(0);

    await page.getByRole("button", { name: "Back" }).click();
    await expect(page.getByTestId("edition-editor")).toBeHidden();
    await page.getByRole("button", { name: editionName }).click();
    await expect(page.getByTestId("edition-editor")).toBeVisible();
    await expect.poll(async () => assocNames(page), { timeout: 20000 }).toEqual([
      second,
      first,
    ]);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("cancel does not call the server", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const first = `NightOrdKeepA-${stamp}`;
    const second = `NightOrdKeepB-${stamp}`;
    const editionName = `NightEdOrdKeep-${stamp}`;
    let puts = 0;
    await page.route(REORDER, async (route) => {
      if (route.request().method() === "PUT") {
        puts += 1;
      }
      await route.continue();
    });
    await openDesign(page);
    await createContentList(page, first);
    await createContentList(page, second);
    await createAndOpenEdition(page, editionName);
    await associateList(page, first);
    await associateList(page, second);

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      await dialog.dismiss();
    });
    await page.locator('[data-testid^="edition-move-down-"]').first().click();
    await expect.poll(async () => assocNames(page)).toEqual([first, second]);
    expect(puts).toBe(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous order", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const first = `NightOrdErrA-${stamp}`;
    const second = `NightOrdErrB-${stamp}`;
    const editionName = `NightEdOrdErr-${stamp}`;
    await openDesign(page);
    await createContentList(page, first);
    await createContentList(page, second);
    await createAndOpenEdition(page, editionName);
    await associateList(page, first);
    await associateList(page, second);

    const cases = [
      [400, "sequence must be the adjacent position"],
      [403, "Admin or Designer role required to save a publish edition"],
      [409, "Edition is in use"],
    ];
    for (const [status, message] of cases) {
      await page.unroute(REORDER);
      await page.route(REORDER, async (route) => {
        if (route.request().method() === "PUT") {
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
      await page.locator('[data-testid^="edition-move-up-"]').nth(1).click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect.poll(async () => assocNames(page)).toEqual([first, second]);
    }
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
