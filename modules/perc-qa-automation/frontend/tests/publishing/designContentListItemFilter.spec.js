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
 * PublishingShell Design — set or clear the item filter on one content list
 * (#5160 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContentListItemFilter.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function contentListPut(url, method) {
  if (method !== "PUT") {
    return false;
  }
  try {
    return /\/publishingdesign\/contentlists\/[^/]+$/.test(new URL(url).pathname);
  } catch {
    return false;
  }
}

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
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
  await page.getByRole("tab", { name: /Content lists/i }).click();
}

async function createContentList(page, name) {
  await page.getByTestId("design-add-content-list").click();
  await expect(page.getByTestId("contentlist-editor")).toBeVisible();
  await page.locator("#cl-name").fill(name);
  // A list with no generator, expander, and filter is legacy (isLegacy).
  // Keep a generator so clearing the filter does not hide this control.
  await page.locator("#cl-gen").fill("sys_Search");
  await expect(page.locator("#cl-item-filter")).toBeEnabled({ timeout: 20000 });
  await page.getByTestId("contentlist-save").click();
  await expect(page.getByTestId("contentlist-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
}

async function acceptDiscard(page) {
  page.once("dialog", async (dialog) => {
    expect(dialog.type()).toBe("confirm");
    await dialog.accept();
  });
  await page.getByRole("button", { name: /^Back$/ }).click();
  await expect(page.getByTestId("contentlist-editor")).toBeHidden({
    timeout: 10000,
  });
}

async function chooseOtherFilter(page) {
  const select = page.locator("#cl-item-filter");
  await expect(select).toBeEnabled({ timeout: 20000 });
  const chosen = await select.evaluate((el) => {
    const current = el.value;
    const option = Array.from(el.options).find(
      (opt) => opt.value && opt.value !== current,
    );
    if (!option) {
      return null;
    }
    return { value: option.value, label: option.label };
  });
  expect(chosen, "H2 catalog has no item filter to select").toBeTruthy();
  await select.selectOption(chosen.value);
  return chosen;
}

test.describe("PublishingShell Design content-list item filter", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets and clears one item filter only after save", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    page.on("request", (req) => {
      if (contentListPut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    const name = `NightFilt-${Date.now()}`;
    await openDesignContentLists(page);
    await createContentList(page, name);
    const row = page.getByRole("button", { name, exact: true });
    const rowId = await row.getAttribute("data-testid");
    const id = rowId.slice("design-content-list-".length);
    const filterLabel = page.getByTestId(`design-content-list-filter-${id}`);
    await expect(filterLabel).toHaveText(/No item filter/i);

    await row.click();
    await expect(page.getByTestId("contentlist-stored-item-filter")).toHaveText(
      /No item filter/i,
    );
    const chosen = await chooseOtherFilter(page);
    await expect(page.getByTestId("contentlist-stored-item-filter")).toHaveText(
      /No item filter/i,
    );
    const putsBeforeCancel = putBodies.length;
    await acceptDiscard(page);
    await expect(filterLabel).toHaveText(/No item filter/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await row.click();
    await page.locator("#cl-name").fill("   ");
    await page.getByTestId("contentlist-save").click();
    await expect(page.getByRole("alert")).toContainText(/Name is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await page.locator("#cl-name").fill(name);

    const chosenAgain = await chooseOtherFilter(page);
    const putResponse = page.waitForResponse(
      (res) => contentListPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 20000 },
    );
    await page.getByTestId("contentlist-save").click();
    await putResponse;
    await expect(page.getByTestId("contentlist-editor")).toBeHidden({
      timeout: 20000,
    });
    expect(putBodies.at(-1)).toContain(chosenAgain.value);
    await expect(filterLabel).toHaveText(chosenAgain.label, { timeout: 20000 });

    await row.click();
    await expect(page.getByTestId("contentlist-stored-item-filter")).toContainText(
      chosenAgain.label,
    );
    await expect(page.locator("#cl-item-filter")).toBeEnabled({ timeout: 20000 });
    await page.locator("#cl-item-filter").selectOption("");
    await expect(page.getByTestId("contentlist-stored-item-filter")).toContainText(
      chosenAgain.label,
    );
    const clearResponse = page.waitForResponse(
      (res) => contentListPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 20000 },
    );
    await page.getByTestId("contentlist-save").click();
    await clearResponse;
    await expect(page.getByTestId("contentlist-editor")).toBeHidden({
      timeout: 20000,
    });
    await expect(filterLabel).toHaveText(/No item filter/i, { timeout: 20000 });
    expect(chosen.label).toBeTruthy();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("keeps the previous filter on HTTP 400, 403, and 409", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const name = `NightFiltErr-${Date.now()}`;
    const taken = `NightFiltTaken-${Date.now()}`;
    await openDesignContentLists(page);
    await createContentList(page, name);
    await createContentList(page, taken);

    const row = page.getByRole("button", { name, exact: true });
    const rowId = await row.getAttribute("data-testid");
    const id = rowId.slice("design-content-list-".length);
    const filterLabel = page.getByTestId(`design-content-list-filter-${id}`);
    await expect(filterLabel).toHaveText(/No item filter/i);

    await row.click();
    await page.locator("#cl-name").fill(taken);
    await chooseOtherFilter(page);
    await page.getByTestId("contentlist-save").click();
    await expect(page.getByRole("alert")).toContainText(/already exists|409|Conflict/i);
    await expect(page.getByTestId("contentlist-stored-item-filter")).toHaveText(
      /No item filter/i,
    );
    await acceptDiscard(page);
    await expect(filterLabel).toHaveText(/No item filter/i);

    async function stubPut(status, message) {
      await page.unrouteAll({ behavior: "ignoreErrors" });
      await page.route(
        "**/services/sitemanage/publishingdesign/contentlists/**",
        (route) => {
          if (route.request().method() !== "PUT") {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
        },
      );
      await row.click();
      await expect(page.locator("#cl-item-filter")).toBeEnabled({ timeout: 20000 });
      await chooseOtherFilter(page);
      await page.getByTestId("contentlist-save").click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect(page.getByTestId("contentlist-stored-item-filter")).toHaveText(
        /No item filter/i,
      );
      await expect(page.getByTestId("contentlist-editor")).toBeVisible();
      await acceptDiscard(page);
      await expect(filterLabel).toHaveText(/No item filter/i);
    }

    await stubPut(400, "Unknown item filter");
    await stubPut(403, "Admin or Designer role required");
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
