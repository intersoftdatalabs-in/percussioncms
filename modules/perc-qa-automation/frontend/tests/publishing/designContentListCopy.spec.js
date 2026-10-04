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
 * PublishingShell Design — copy one content list (#5159 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designContentListCopy.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function contentListCopyPost(url, method) {
  if (method !== "POST") {
    return false;
  }
  try {
    return /\/publishingdesign\/contentlists\/copy$/.test(new URL(url).pathname);
  } catch {
    return false;
  }
}

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

async function sourceCopyButton(page, name) {
  const source = page.getByRole("button", { name, exact: true });
  const testId = await source.getAttribute("data-testid");
  expect(testId).toMatch(/^design-content-list-/);
  const id = testId.slice("design-content-list-".length);
  return {
    id,
    testId,
    copy: page.getByTestId(`design-content-list-copy-${id}`),
  };
}

async function acceptDiscard(page) {
  page.once("dialog", async (dialog) => {
    expect(dialog.type()).toBe("confirm");
    await dialog.accept();
  });
  await page.getByTestId("contentlist-copy-cancel").click();
  await expect(page.getByTestId("contentlist-copy")).toBeHidden({
    timeout: 10000,
  });
}

test.describe("PublishingShell Design content-list copy", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("copies one content list only after the new name is confirmed", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        jsErrors.push(msg.text());
      }
    });
    const copyBodies = [];
    const putBodies = [];
    page.on("request", (req) => {
      if (contentListCopyPost(req.url(), req.method())) {
        copyBodies.push(req.postData() || "");
      }
      if (contentListPut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    const stamp = Date.now();
    const original = `NightCl-${stamp}`;
    const copied = `NightClCopy-${stamp}`;

    await openDesignContentLists(page);
    await createContentList(page, original);
    const source = await sourceCopyButton(page, original);

    await source.copy.click();
    await expect(page.getByTestId("contentlist-copy")).toBeVisible();
    await expect(page.locator("#contentlist-copy-name")).toHaveValue(
      `${original} copy`,
    );
    await page.locator("#contentlist-copy-name").fill(copied);
    const copiesBeforeCancel = copyBodies.length;
    await acceptDiscard(page);
    await expect(page.getByTestId(source.testId)).toHaveText(original);
    await expect(
      page.getByRole("button", { name: copied, exact: true }),
    ).toHaveCount(0);
    expect(copyBodies.length).toBe(copiesBeforeCancel);
    expect(putBodies.length).toBe(0);

    await page.getByTestId(`design-content-list-copy-${source.id}`).click();
    await page.locator("#contentlist-copy-name").fill("   ");
    await page.getByTestId("contentlist-copy-submit").click();
    await expect(page.getByRole("alert")).toContainText(/Name is required/i);
    expect(copyBodies.length).toBe(copiesBeforeCancel);
    await expect(page.getByTestId("contentlist-copy")).toBeVisible();

    await page.locator("#contentlist-copy-name").fill("N".repeat(101));
    await page.getByTestId("contentlist-copy-submit").click();
    await expect(page.getByRole("alert")).toContainText(/100 characters/i);
    expect(copyBodies.length).toBe(copiesBeforeCancel);

    await page.locator("#contentlist-copy-name").fill(copied);
    const copyResponse = page.waitForResponse(
      (res) =>
        contentListCopyPost(res.url(), res.request().method()) && res.ok(),
      { timeout: 20000 },
    );
    await page.getByTestId("contentlist-copy-submit").click();
    await copyResponse;
    await expect(page.getByTestId("contentlist-copy")).toBeHidden({
      timeout: 20000,
    });
    await expect(
      page.getByRole("button", { name: copied, exact: true }),
    ).toBeVisible({ timeout: 20000 });
    await expect(page.getByTestId(source.testId)).toHaveText(original);
    expect(copyBodies.at(-1)).toContain(copied);
    expect(copyBodies.at(-1)).toContain(source.id);
    expect(putBodies.length).toBe(0);

    await page.getByTestId(source.testId).click();
    await expect(page.locator("#cl-name")).toHaveValue(original);
    await expect(page.locator("#cl-type")).toBeDisabled();
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("does not add a row when the copy name is a duplicate", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
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
    const source = await sourceCopyButton(page, original);

    await source.copy.click();
    await page.locator("#contentlist-copy-name").fill(taken);
    await page.getByTestId("contentlist-copy-submit").click();
    await expect(page.getByRole("alert")).toContainText(
      /already exists|409|Conflict/i,
    );
    await expect(page.getByTestId("contentlist-copy")).toBeVisible();
    await acceptDiscard(page);
    await expect(page.getByTestId(source.testId)).toHaveText(original);
    await expect(
      page.getByRole("button", { name: taken, exact: true }),
    ).toHaveCount(1);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
