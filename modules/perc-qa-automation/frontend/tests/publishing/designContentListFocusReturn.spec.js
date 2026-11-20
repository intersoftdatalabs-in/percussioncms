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
 * PublishingShell Design — content-list sub-panels return focus to the
 * invoking row button on close (#6748 / parent #6733).
 *
 * The sub-panels (Description, Copy, Generator, Legacy URL, full editor)
 * replace the whole content-lists view, so their inputs unmount on
 * Cancel/Save and focus would fall back to <body>. DesignSection records the
 * invoking row control and restores focus to it when the panel closes
 * (deleted rows fall back to Add content list).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContentListFocusReturn.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const GENERATOR = "sys_Search";

function trackJsErrors(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      text.includes("Failed to load resource") ||
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

async function openContentLists(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
  await page.getByRole("tab", { name: /Content lists/i }).click();
}

function sourceRow(page, name) {
  return page
    .getByRole("listitem")
    .filter({ has: page.getByRole("button", { name, exact: true }) });
}

function describeButton(page, name) {
  return sourceRow(page, name).locator(
    "[data-testid^='design-content-list-describe-']",
  );
}

async function chooseFilter(page) {
  const select = page.locator("#cl-item-filter");
  await expect(select).toBeEnabled({ timeout: 20000 });
  const chosen = await select.evaluate((el) => {
    const option = Array.from(el.options).find((opt) => opt.value);
    if (!option) {
      return null;
    }
    return { value: option.value, label: option.label };
  });
  expect(chosen, "H2 catalog has no item filter to select").toBeTruthy();
  await select.selectOption(chosen.value);
  return chosen;
}

async function createModernList(page, name, description) {
  await page.getByTestId("design-add-content-list").click();
  await expect(page.getByTestId("contentlist-editor")).toBeVisible();
  await page.locator("#cl-name").fill(name);
  await page.locator("#cl-desc").fill(description);
  await page.locator("#cl-gen").fill(GENERATOR);
  await chooseFilter(page);
  const createResponse = page.waitForResponse(
    (res) =>
      res.request().method() === "POST" &&
      /\/publishingdesign\/contentlists(?:\?|$)/.test(res.url()),
    { timeout: 30000 },
  );
  await page.getByTestId("contentlist-save").click();
  const posted = await createResponse;
  if (posted.status() !== 200 && posted.status() !== 201) {
    throw new Error(
      `create content list HTTP ${posted.status()} ${(await posted.text()).slice(0, 400)}`,
    );
  }
  await expect(page.getByTestId("contentlist-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
}

test.describe("PublishingShell Design content list focus return (#6748)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("Cancel returns focus to the Description row button", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openContentLists(page);
    const stamp = Date.now().toString().slice(-8);
    const name = `ClFcs${stamp}`;
    const description = `Focus return ${stamp}`;
    await createModernList(page, name, description);

    await describeButton(page, name).click();
    await expect(page.getByTestId("contentlist-description-form")).toBeVisible();
    await page.locator("#contentlist-description-input").fill("will not save");
    await page.getByTestId("contentlist-description-cancel").click();
    await expect(page.getByTestId("contentlist-description-form")).toBeHidden();
    await expect(describeButton(page, name)).toBeFocused();
    await page.screenshot({
      path: "test-results/live-proof-6748-contentlist-focus-return.png",
    });
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("Escape returns focus to the Description row button", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openContentLists(page);
    const stamp = Date.now().toString().slice(-8);
    const name = `ClFcsE${stamp}`;
    const description = `Focus escape ${stamp}`;
    await createModernList(page, name, description);

    await describeButton(page, name).click();
    await expect(page.getByTestId("contentlist-description-form")).toBeVisible();
    await page.locator("#contentlist-description-input").fill("will not save");
    await page.locator("#contentlist-description-input").press("Escape");
    await expect(page.getByTestId("contentlist-description-form")).toBeHidden();
    await expect(describeButton(page, name)).toBeFocused();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("Save returns focus to the Description row button", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    await openContentLists(page);
    const stamp = Date.now().toString().slice(-8);
    const name = `ClFcsS${stamp}`;
    const description = `Focus save ${stamp}`;
    const updated = `Focus saved ${stamp}`;
    await createModernList(page, name, description);

    await describeButton(page, name).click();
    await expect(page.getByTestId("contentlist-description-form")).toBeVisible();
    await page.locator("#contentlist-description-input").fill(updated);
    await page.getByTestId("contentlist-description-save").click();
    await expect(page.getByTestId("contentlist-description-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(
      sourceRow(page, name).locator("[data-testid^='design-content-list-description-']"),
    ).toHaveText(updated, { timeout: 20000 });
    await expect(describeButton(page, name)).toBeFocused();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
