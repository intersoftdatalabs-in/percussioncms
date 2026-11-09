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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * PublishingShell Design — set or clear one edition schedule (#5837 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionSchedule.spec.js
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

test.describe("PublishingShell Design edition schedule", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("sets and clears one edition schedule only after save and keeps name, priority, comment, and order", async ({
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
    const listA = `NightSchA-${stamp}`;
    const listB = `NightSchB-${stamp}`;
    const original = `NightSch-${stamp}`;
    const comment = `keep-${stamp}`;

    await openDesign(page);
    await createContentList(page, listA);
    await createContentList(page, listB);
    await createEdition(page, original, comment, 2);

    const row = editionRow(page, original);
    await expect(row.locator("[data-testid^='design-edition-comment-']")).toHaveText(
      comment,
    );
    await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
      "2",
    );
    await expect(row.locator("[data-testid^='design-edition-schedule-value-']")).toHaveText(
      "",
    );

    await page.getByRole("button", { name: original, exact: true }).click();
    await associateList(page, listA);
    await associateList(page, listB);
    await expectOrder(page, listA, listB);
    await page.getByRole("button", { name: /^Back$/ }).click();
    await expect(page.getByTestId("edition-editor")).toBeHidden({ timeout: 10000 });

    const putsBefore = putBodies.length;
    await row.getByRole("button", { name: "Schedule", exact: true }).click();
    await expect(page.getByTestId("edition-schedule-form")).toBeVisible();
    await expect(page.getByTestId("edition-schedule-name")).toHaveText(original);
    await expect(page.getByTestId("edition-schedule-priority")).toHaveText("2");
    await expect(page.getByTestId("edition-schedule-comment")).toHaveText(comment);
    await expect(page.locator("#edition-schedule-start-input")).toHaveValue("");
    await expect(page.locator("#edition-schedule-end-input")).toHaveValue("");

    await page.locator("#edition-schedule-start-input").fill("2030-11-09T08:00");
    await page.locator("#edition-schedule-end-input").fill("2030-11-10T17:00");
    await page.getByTestId("edition-schedule-cancel").click();
    await expect(page.getByTestId("edition-schedule-form")).toBeHidden();
    await expect(row.locator("[data-testid^='design-edition-schedule-value-']")).toHaveText(
      "",
    );
    expect(putBodies.length).toBe(putsBefore);

    await row.getByRole("button", { name: "Schedule", exact: true }).click();
    await page.locator("#edition-schedule-start-input").fill("2030-11-09T08:00");
    await page.locator("#edition-schedule-end-input").fill("2030-11-10T17:00");
    const putResponse = page.waitForResponse(
      (res) =>
        editionUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 20000 },
    );
    await page.getByTestId("edition-schedule-save").click();
    await putResponse;
    await expect(page.getByTestId("edition-schedule-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(row.locator("[data-testid^='design-edition-schedule-value-']")).toHaveText(
      "11/09/2030 08:00 am → 11/10/2030 05:00 pm",
      { timeout: 20000 },
    );
    await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
      "2",
    );
    await expect(row.locator("[data-testid^='design-edition-comment-']")).toHaveText(
      comment,
    );
    await expect(page.getByRole("button", { name: original, exact: true })).toBeVisible();
    expect(putBodies.length).toBe(putsBefore + 1);
    const setBody = putBodies.at(-1);
    expect(setBody).toContain('"startDate"');
    expect(setBody).toContain("11/09/2030 08:00 am");
    expect(setBody).toContain('"endDate"');
    expect(setBody).toContain("11/10/2030 05:00 pm");
    expect(setBody).not.toContain('"name"');
    expect(setBody).not.toContain('"priority"');
    expect(setBody).not.toContain('"comment"');
    expect(setBody).not.toContain(original);
    expect(sequencePuts).toEqual([]);

    await row.getByRole("button", { name: "Schedule", exact: true }).click();
    await expect(page.locator("#edition-schedule-start-input")).toHaveValue(
      "2030-11-09T08:00",
    );
    await expect(page.locator("#edition-schedule-end-input")).toHaveValue(
      "2030-11-10T17:00",
    );
    await page.getByTestId("edition-schedule-clear").click();
    await expect(page.locator("#edition-schedule-start-input")).toHaveValue("");
    await expect(page.locator("#edition-schedule-end-input")).toHaveValue("");
    const clearResponse = page.waitForResponse(
      (res) =>
        editionUpdatePut(res.url(), res.request().method()) && res.ok(),
      { timeout: 20000 },
    );
    await page.getByTestId("edition-schedule-save").click();
    await clearResponse;
    await expect(page.getByTestId("edition-schedule-form")).toBeHidden({
      timeout: 20000,
    });
    await expect(row.locator("[data-testid^='design-edition-schedule-value-']")).toHaveText(
      "",
      { timeout: 20000 },
    );
    await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
      "2",
    );
    const clearBody = putBodies.at(-1);
    expect(clearBody).toContain('"startDate":""');
    expect(clearBody).toContain('"endDate":""');
    expect(clearBody).not.toContain('"name"');
    expect(clearBody).not.toContain('"priority"');
    expect(clearBody).not.toContain('"comment"');
    expect(sequencePuts).toEqual([]);

    await page.getByRole("button", { name: original, exact: true }).click();
    await expect(page.locator("#ed-name")).toHaveValue(original);
    await expect(page.locator("#ed-comment")).toHaveValue(comment);
    await expect(page.locator("#ed-priority")).toHaveValue("2");
    await expectOrder(page, listA, listB);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("keeps the previous schedule on HTTP 400, 403, and 409", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const original = `NightSchHttp-${stamp}`;
    const comment = `stay-${stamp}`;

    await openDesign(page);
    await createEdition(page, original, comment, 3);
    const row = editionRow(page, original);
    await expect(row.locator("[data-testid^='design-edition-schedule-value-']")).toHaveText(
      "",
    );
    await expect(row.locator("[data-testid^='design-edition-comment-']")).toHaveText(
      comment,
    );
    await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
      "3",
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
      await row.getByRole("button", { name: "Schedule", exact: true }).click();
      await expect(page.getByTestId("edition-schedule-form")).toBeVisible();
      await page.locator("#edition-schedule-start-input").fill("2030-11-11T08:00");
      await page.locator("#edition-schedule-end-input").fill("2030-11-12T17:00");
      await page.getByTestId("edition-schedule-save").click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expect(page.getByTestId("edition-schedule-form")).toBeVisible();
      await expect(page.getByTestId("edition-schedule-name")).toHaveText(original);
      await expect(page.getByTestId("edition-schedule-priority")).toHaveText("3");
      await page.getByTestId("edition-schedule-cancel").click();
      await expect(row.locator("[data-testid^='design-edition-schedule-value-']")).toHaveText(
        "",
      );
      await expect(row.locator("[data-testid^='design-edition-comment-']")).toHaveText(
        comment,
      );
      await expect(row.locator("[data-testid^='design-edition-priority-value-']")).toHaveText(
        "3",
      );
      await page.unroute("**/services/sitemanage/publishingdesign/editions/**");
    }

    await rejectPut(400, "Edition expiry must be after the embargo date");
    await rejectPut(
      403,
      "Admin or Designer role required to save a publish edition",
    );
    await rejectPut(409, "Edition name already exists");
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
