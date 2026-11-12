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
 * PublishingShell Design — edition list search + schedule filter (#6078 / parent #4531).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionsFilter.spec.js
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

async function openDesign(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
}

async function openEditionsTab(page) {
  await page.getByRole("tab", { name: /Editions/i }).click();
}

async function createEdition(page, name, comment, priority) {
  await openEditionsTab(page);
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

async function scheduleEdition(page, name, startLocal) {
  const row = editionRow(page, name);
  await row.getByRole("button", { name: "Schedule", exact: true }).click();
  await expect(page.getByTestId("edition-schedule-form")).toBeVisible();
  await page.locator("#edition-schedule-start-input").fill(startLocal);
  const putResponse = page.waitForResponse(
    (res) =>
      res.request().method() === "PUT" &&
      /\/publishingdesign\/editions\/[^/]+$/.test(
        new URL(res.url()).pathname,
      ) &&
      res.ok(),
    { timeout: 20000 },
  );
  await page.getByTestId("edition-schedule-save").click();
  await putResponse;
  await expect(page.getByTestId("edition-schedule-form")).toBeHidden({
    timeout: 20000,
  });
}

test.describe("PublishingShell Design editions search and schedule filter (#6078)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("search narrows by name/comment and the schedule facet splits scheduled from unscheduled", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const alpha = `FltAlpha-${stamp}`;
    const beta = `FltBeta-${stamp}`;

    await openDesign(page);
    await createEdition(page, alpha, `plain-${stamp}`, 1);
    await createEdition(page, beta, `noted-${stamp}`, 2);
    await scheduleEdition(page, beta, "2030-11-09T08:00");

    const search = page.getByTestId("design-editions-search");
    const facet = page.getByTestId("design-editions-schedule-filter");

    // Search by name: only the matching row stays.
    await search.fill(beta);
    await expect(page.getByRole("button", { name: beta, exact: true })).toBeVisible();
    await expect(
      page.getByRole("button", { name: alpha, exact: true }),
    ).toBeHidden();

    // Search by comment: the other row stays.
    await search.fill(`noted-${stamp}`);
    await expect(page.getByRole("button", { name: beta, exact: true })).toBeVisible();
    await expect(
      page.getByRole("button", { name: alpha, exact: true }),
    ).toBeHidden();

    // No match: empty-filter state, no rows for this pair.
    await search.fill("zzzz-no-edition-match");
    await expect(page.getByTestId("design-editions-empty-filter")).toBeVisible();
    await expect(
      page.getByRole("button", { name: alpha, exact: true }),
    ).toBeHidden();
    await expect(
      page.getByRole("button", { name: beta, exact: true }),
    ).toBeHidden();

    // Clear search: both rows return.
    await search.fill("");
    await expect(page.getByRole("button", { name: alpha, exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: beta, exact: true })).toBeVisible();

    // Schedule facet: Scheduled keeps the edition with a start date.
    await facet.selectOption("scheduled");
    await expect(page.getByRole("button", { name: beta, exact: true })).toBeVisible();
    await expect(
      page.getByRole("button", { name: alpha, exact: true }),
    ).toBeHidden();

    // Schedule facet: Unscheduled keeps the edition with no dates.
    await facet.selectOption("unscheduled");
    await expect(page.getByRole("button", { name: alpha, exact: true })).toBeVisible();
    await expect(
      page.getByRole("button", { name: beta, exact: true }),
    ).toBeHidden();

    // Back to All: both rows return, still no extra server round trip needed
    // for the facet itself (client-side over the loaded list).
    await facet.selectOption("all");
    await expect(page.getByRole("button", { name: alpha, exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: beta, exact: true })).toBeVisible();

    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
