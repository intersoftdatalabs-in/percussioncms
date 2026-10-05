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
 * PublishingShell Design — rename one edition (#5202 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionRename.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function editionUpdatePut(url, method) {
  if (method !== "PUT") {
    return false;
  }
  try {
    return /\/publishingdesign\/editions\/[^/]+$/.test(new URL(url).pathname);
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

async function openDesign(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
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
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
}

async function createEdition(page, name, comment, priority) {
  await page.getByRole("tab", { name: /Editions/i }).click();
  await page.getByTestId("design-add-edition").click();
  await expect(page.getByTestId("edition-editor")).toBeVisible();
  await page.locator("#ed-name").fill(name);
  await page.locator("#ed-comment").fill(comment);
  await page.locator("#ed-priority").fill(String(priority));
  await page.getByTestId("edition-save").click();
  await expect(page.getByTestId("edition-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
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

async function expectOrder(page, first, second) {
  const names = page.locator("[data-testid^='edition-assoc-name-']");
  await expect(names).toHaveCount(2);
  await expect(names.nth(0)).toHaveText(first);
  await expect(names.nth(1)).toHaveText(second);
}

test.describe("PublishingShell Design edition rename", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("renames one edition only after save and keeps priority, comment, and order", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    const sequencePuts = [];
    page.on("request", (req) => {
      if (editionUpdatePut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
      if (
        req.method() === "PUT" &&
        /\/contentlists\/[^/]+\/sequence$/.test(req.url())
      ) {
        sequencePuts.push(req.url());
      }
    });

    const stamp = Date.now();
    const listA = `NightEdA-${stamp}`;
    const listB = `NightEdB-${stamp}`;
    const original = `NightEd-${stamp}`;
    const renamed = `NightEdRenamed-${stamp}`;
    const comment = `keep-${stamp}`;

    await openDesign(page);
    await createContentList(page, listA);
    await createContentList(page, listB);
    await createEdition(page, original, comment, 1);

    await page.getByRole("button", { name: original, exact: true }).click();
    await expect(page.locator("#ed-name")).toHaveValue(original);
    await associateList(page, listA);
    await associateList(page, listB);
    await expectOrder(page, listA, listB);

    await page.locator("#ed-name").fill(renamed);
    const putsBeforeCancel = putBodies.length;
    await page.getByRole("button", { name: /^Back$/ }).click();
    await expect(page.getByTestId("edition-editor")).toBeHidden({
      timeout: 10000,
    });
    await expect(
      page.getByRole("button", { name: original, exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: renamed, exact: true }),
    ).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.getByRole("button", { name: original, exact: true }).click();
    await expect(page.locator("#ed-priority")).toHaveValue("1");
    await expect(page.locator("#ed-comment")).toHaveValue(comment);
    await expectOrder(page, listA, listB);

    await page.locator("#ed-name").fill("   ");
    await page.getByTestId("edition-save").click();
    await expect(page.getByRole("alert")).toContainText(/Name is required/i);
    expect(putBodies.length).toBe(putsBeforeCancel);

    await page.locator("#ed-name").fill("N".repeat(101));
    await page.getByTestId("edition-save").click();
    await expect(page.getByRole("alert")).toContainText(
      /100 characters or fewer/i,
    );
    expect(putBodies.length).toBe(putsBeforeCancel);
    await expect(page.getByTestId("edition-editor")).toBeVisible();

    await page.locator("#ed-name").fill(renamed);
    const putResponse = page.waitForResponse(
      (res) =>
        editionUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 20000 },
    );
    await page.getByTestId("edition-save").click();
    await putResponse;
    await expect(page.getByTestId("edition-editor")).toBeHidden({
      timeout: 20000,
    });
    await expect(
      page.getByRole("button", { name: renamed, exact: true }),
    ).toBeVisible({ timeout: 20000 });
    await expect(
      page.getByRole("button", { name: original, exact: true }),
    ).toHaveCount(0);
    expect(putBodies.length).toBe(putsBeforeCancel + 1);
    const body = putBodies.at(-1);
    expect(body).toContain(renamed);
    expect(body).not.toContain('"priority"');
    expect(body).not.toContain('"comment"');
    expect(sequencePuts).toEqual([]);

    await page.getByRole("button", { name: renamed, exact: true }).click();
    await expect(page.locator("#ed-name")).toHaveValue(renamed);
    await expect(page.locator("#ed-priority")).toHaveValue("1");
    await expect(page.locator("#ed-comment")).toHaveValue(comment);
    await expectOrder(page, listA, listB);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("keeps the old name when a rename is a duplicate", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const original = `NightEdDupA-${stamp}`;
    const taken = `NightEdDupB-${stamp}`;
    const comment = `stay-${stamp}`;

    await openDesign(page);
    await createEdition(page, original, comment, 4);
    await createEdition(page, taken, "other", 2);

    await page.getByRole("button", { name: original, exact: true }).click();
    await page.locator("#ed-name").fill(taken);
    await page.getByTestId("edition-save").click();
    await expect(page.getByRole("alert")).toContainText(
      /already exists|409|Conflict/i,
    );
    await expect(page.getByTestId("edition-editor")).toBeVisible();
    await expect(page.locator("#ed-priority")).toHaveValue("4");
    await expect(page.locator("#ed-comment")).toHaveValue(comment);
    await page.getByRole("button", { name: /^Back$/ }).click();
    await expect(
      page.getByRole("button", { name: original, exact: true }),
    ).toBeVisible();
    await expect(page.getByRole("button", { name: taken, exact: true })).toHaveCount(
      1,
    );
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("keeps the old name on HTTP 400 and 403", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const original = `NightEdHttp-${stamp}`;

    await openDesign(page);
    await createEdition(page, original, `note-${stamp}`, 2);
    await page.getByRole("button", { name: original, exact: true }).click();
    await expect(page.locator("#ed-name")).toHaveValue(original);

    async function rejectPut(status, message) {
      await page.route(
        "**/services/sitemanage/publishingdesign/editions/**",
        async (route) => {
          const req = route.request();
          let path = "";
          try {
            path = new URL(req.url()).pathname;
          } catch {
            path = "";
          }
          if (req.method() === "PUT" && /\/editions\/[^/]+$/.test(path)) {
            await route.fulfill({
              status,
              contentType: "application/json",
              body: JSON.stringify({ message }),
            });
            return;
          }
          await route.continue();
        },
      );
      await page.locator("#ed-name").fill(`${original}-x`);
      await page.getByTestId("edition-save").click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect(page.getByTestId("edition-editor")).toBeVisible();
      await page.unroute("**/services/sitemanage/publishingdesign/editions/**");
    }

    await rejectPut(400, "Edition name must be 100 characters or fewer");
    await rejectPut(
      403,
      "Admin or Designer role required to save a publish edition",
    );

    await page.getByRole("button", { name: /^Back$/ }).click();
    await expect(
      page.getByRole("button", { name: original, exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: `${original}-x`, exact: true }),
    ).toHaveCount(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
