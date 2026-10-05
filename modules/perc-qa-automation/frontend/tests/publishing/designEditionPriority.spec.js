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
 * PublishingShell Design — set one edition priority (#5223 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionPriority.spec.js
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

function editionRow(page, name) {
  return page.locator("li").filter({
    has: page.getByRole("button", { name, exact: true }),
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

test.describe("PublishingShell Design edition priority", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets one edition priority only after save and keeps name, comment, and order", async ({
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
    const listA = `NightPriA-${stamp}`;
    const listB = `NightPriB-${stamp}`;
    const original = `NightPri-${stamp}`;
    const comment = `keep-${stamp}`;

    await openDesign(page);
    await createContentList(page, listA);
    await createContentList(page, listB);
    await createEdition(page, original, comment, 1);

    const row = editionRow(page, original);
    await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
      "1",
    );
    await expect(row.locator("[data-testid^='design-edition-comment-']")).toHaveText(
      comment,
    );

    await page.getByRole("button", { name: original, exact: true }).click();
    await associateList(page, listA);
    await associateList(page, listB);
    await expectOrder(page, listA, listB);
    await page.getByRole("button", { name: /^Back$/ }).click();
    await expect(page.getByTestId("edition-editor")).toBeHidden({ timeout: 10000 });

    const putsBefore = putBodies.length;
    await row.getByRole("button", { name: "Priority", exact: true }).click();
    await expect(page.getByTestId("edition-priority-form")).toBeVisible();
    await expect(page.getByTestId("edition-priority-name")).toHaveText(original);
    await expect(page.getByTestId("edition-priority-comment")).toHaveText(comment);
    await expect(page.locator("#edition-priority-input")).toHaveValue("1");

    await page.locator("#edition-priority-input").fill("5");
    await page.getByTestId("edition-priority-cancel").click();
    await expect(page.getByTestId("edition-priority-form")).toBeHidden();
    await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
      "1",
    );
    expect(putBodies.length).toBe(putsBefore);

    await row.getByRole("button", { name: "Priority", exact: true }).click();
    for (const bad of ["0", "6", "1.5", ""]) {
      await page.locator("#edition-priority-input").fill(bad);
      await page.getByTestId("edition-priority-save").click();
      await expect(page.getByRole("alert")).toContainText(
        /Edition priority must be from 1 to 5/i,
      );
    }
    expect(putBodies.length).toBe(putsBefore);
    await expect(page.getByTestId("edition-priority-form")).toBeVisible();

    await page.locator("#edition-priority-input").fill("5");
    const putResponse = page.waitForResponse(
      (res) =>
        editionUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 20000 },
    );
    await page.getByTestId("edition-priority-save").click();
    await putResponse;
    await expect(page.getByTestId("edition-priority-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
      "5",
      { timeout: 20000 },
    );
    await expect(row.locator("[data-testid^='design-edition-comment-']")).toHaveText(
      comment,
    );
    await expect(page.getByRole("button", { name: original, exact: true })).toBeVisible();
    expect(putBodies.length).toBe(putsBefore + 1);
    const body = putBodies.at(-1);
    expect(body).toContain('"priority"');
    expect(body).toContain("5");
    expect(body).not.toContain('"name"');
    expect(body).not.toContain('"comment"');
    expect(body).not.toContain(original);
    expect(sequencePuts).toEqual([]);

    await page.getByRole("button", { name: original, exact: true }).click();
    await expect(page.locator("#ed-name")).toHaveValue(original);
    await expect(page.locator("#ed-comment")).toHaveValue(comment);
    await expect(page.locator("#ed-priority")).toHaveValue("5");
    await expectOrder(page, listA, listB);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("keeps the previous priority on HTTP 400, 403, and 409", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const original = `NightPriHttp-${stamp}`;
    const comment = `stay-${stamp}`;

    await openDesign(page);
    await createEdition(page, original, comment, 2);
    const row = editionRow(page, original);
    await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
      "2",
    );

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
      await row.getByRole("button", { name: "Priority", exact: true }).click();
      await expect(page.getByTestId("edition-priority-form")).toBeVisible();
      await page.locator("#edition-priority-input").fill("5");
      await page.getByTestId("edition-priority-save").click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect(page.getByTestId("edition-priority-form")).toBeVisible();
      await expect(page.getByTestId("edition-priority-name")).toHaveText(original);
      await expect(page.getByTestId("edition-priority-comment")).toHaveText(comment);
      await page.getByTestId("edition-priority-cancel").click();
      await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
        "2",
      );
      await expect(row.locator("[data-testid^='design-edition-comment-']")).toHaveText(
        comment,
      );
      await page.unroute("**/services/sitemanage/publishingdesign/editions/**");
    }

    await rejectPut(400, "Edition priority must be from 1 to 5");
    await rejectPut(
      403,
      "Admin or Designer role required to save a publish edition",
    );
    await rejectPut(409, "Edition name already exists");
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
