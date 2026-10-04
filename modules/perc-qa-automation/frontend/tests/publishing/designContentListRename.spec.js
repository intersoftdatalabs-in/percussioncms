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
 * PublishingShell Design — rename one content list (#5158 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContentListRename.spec.js
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

test.describe("PublishingShell Design content-list rename", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("renames one content list only after save", async ({ page }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });
    const putBodies = [];
    page.on("request", (req) => {
      if (contentListPut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    const stamp = Date.now();
    const original = `NightCl-${stamp}`;
    const renamed = `NightClRenamed-${stamp}`;

    await openDesignContentLists(page);
    await createContentList(page, original);

    await page.getByRole("button", { name: original, exact: true }).click();
    await expect(page.locator("#cl-name")).toBeEnabled();
    await expect(page.locator("#cl-type")).toBeDisabled();
    await page.locator("#cl-name").fill(renamed);
    const putsBeforeCancel = putBodies.length;
    await acceptDiscard(page);
    await expect(
      page.getByRole("button", { name: original, exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: renamed, exact: true }),
    ).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByRole("button", { name: original, exact: true }).click();
    await page.locator("#cl-name").fill("   ");
    await page.getByTestId("contentlist-save").click();
    await expect(page.getByRole("alert")).toContainText(/Name is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("contentlist-editor")).toBeVisible();

    await page.locator("#cl-name").fill(original);
    await page.locator("#cl-desc").fill(`notes-${stamp}`);
    await page.getByTestId("contentlist-save").click();
    await expect(page.getByTestId("contentlist-editor")).toBeHidden({
      timeout: 20000,
    });
    expect(putBodies.length).toBe(putsBeforeCancel + 1);
    expect(putBodies.at(-1)).toContain(original);
    expect(putBodies.at(-1)).not.toContain(renamed);
    await expect(
      page.getByRole("button", { name: original, exact: true }),
    ).toBeVisible();

    await page.getByRole("button", { name: original, exact: true }).click();
    await expect(page.locator("#cl-type")).toBeDisabled();
    await page.locator("#cl-name").fill(renamed);
    const putResponse = page.waitForResponse(
      (res) => contentListPut(res.url(), res.request().method()) && res.ok(),
      { timeout: 20000 },
    );
    await page.getByTestId("contentlist-save").click();
    await putResponse;
    await expect(page.getByTestId("contentlist-editor")).toBeHidden({
      timeout: 20000,
    });
    await expect(
      page.getByRole("button", { name: renamed, exact: true }),
    ).toBeVisible({ timeout: 20000 });
    await expect(
      page.getByRole("button", { name: original, exact: true }),
    ).toHaveCount(0);
    expect(putBodies.at(-1)).toContain(renamed);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("keeps the old name when a rename is a duplicate", async ({ page }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      // The browser logs the expected duplicate-name 409 as a resource error.
      if (/status of 409/.test(text)) {
        return;
      }
      jsErrors.push(text);
    });

    const stamp = Date.now();
    const original = `NightClA-${stamp}`;
    const taken = `NightClB-${stamp}`;

    await openDesignContentLists(page);
    await createContentList(page, original);
    await createContentList(page, taken);

    await page.getByRole("button", { name: original, exact: true }).click();
    await page.locator("#cl-name").fill(taken);
    await page.getByTestId("contentlist-save").click();
    await expect(page.getByRole("alert")).toContainText(
      /already exists|409|Conflict/i,
    );
    await expect(page.getByTestId("contentlist-editor")).toBeVisible();
    await acceptDiscard(page);
    await expect(
      page.getByRole("button", { name: original, exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: taken, exact: true }),
    ).toHaveCount(1);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
