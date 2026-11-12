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
 * Playwright surface: #6215 — Publishing shell job-id / request-id deep links.
 *
 * `section=logs&jobId=` preselects and opens the job detail; unknown ids
 * degrade to the plain Logs list. `section=status&requestId=` surfaces the
 * request as the matching job; unknown ids degrade to the Status list.
 * Never 500, zero JS console errors.
 *
 * Tags: @publishing-deep-links @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-deep-links.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  publishingLogsJobUrl,
  publishingStatusRequestUrl,
  isPubstatusCurrentUrl,
  isPubstatusLogsUrl,
  isPubstatusDetailsUrl,
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-deep-links");

const TAGS = ["@publishing-deep-links", "@publish", "@smoke"];

const JOBS = [
  {
    jobId: 4616,
    siteName: "FastForward",
    editionName: "Full",
    status: "Running",
    completedItems: 1,
    totalItems: 2,
  },
];

const LOGS = [
  {
    jobId: 4616,
    siteName: "FastForward",
    serverName: "prod",
    status: "Completed",
  },
  {
    jobId: 4617,
    siteName: "Marketing",
    serverName: "stage",
    status: "Failed",
  },
];

async function mockPublishingApis(page) {
  await page.route("**/sitemanage/pubstatus/**", async (route) => {
    const url = route.request().url();
    const method = route.request().method();
    if (isPubstatusLogsUrl(url) && method === "POST") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(LOGS),
      });
      return;
    }
    if (isPubstatusDetailsUrl(url) && method === "POST") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ SitePublishItem: [] }),
      });
      return;
    }
    if (isPubstatusCurrentUrl(url) && method === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(JOBS),
      });
      return;
    }
    await route.continue();
  });
}

function collectConsoleErrors(page) {
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    consoleErrors.push(String(err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      const text = msg.text();
      if (!isKnownPublishConsoleNoise(text)) {
        consoleErrors.push(text);
      }
    }
  });
  return consoleErrors;
}

async function expectShellReady(page) {
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
}

test.describe("PublishingShell job/request deep links (#6215)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await mockPublishingApis(page);
  });

  test(`logs jobId opens the job detail ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = collectConsoleErrors(page);
    await page.goto(publishingLogsJobUrl(BASE_URL, 4616), {
      waitUntil: "domcontentloaded",
    });
    await expectShellReady(page);
    await expect(page.getByTestId("publish-section-logs")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByTestId("publish-log-row-4616")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByTestId("publish-log-details")).toBeVisible({
      timeout: 15000,
    });
    expect(consoleErrors).toEqual([]);
  });

  test(`unknown logs jobId degrades to the list ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = collectConsoleErrors(page);
    await page.goto(publishingLogsJobUrl(BASE_URL, 9999), {
      waitUntil: "domcontentloaded",
    });
    await expectShellReady(page);
    await expect(page.getByTestId("publish-section-logs")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByTestId("publish-logs-table")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByTestId("publish-log-details")).toHaveCount(0);
    expect(consoleErrors).toEqual([]);
  });

  test(`status requestId surfaces the request ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = collectConsoleErrors(page);
    await page.goto(publishingStatusRequestUrl(BASE_URL, 4616), {
      waitUntil: "domcontentloaded",
    });
    await expectShellReady(page);
    await expect(page.getByTestId("publish-section-status")).toBeVisible({
      timeout: 15000,
    });
    await expect(
      page.getByTestId("publish-status-detail-job-id"),
    ).toHaveText("4616", { timeout: 15000 });
    expect(consoleErrors).toEqual([]);
  });

  test(`unknown status requestId degrades to the list ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = collectConsoleErrors(page);
    await page.goto(publishingStatusRequestUrl(BASE_URL, 9999), {
      waitUntil: "domcontentloaded",
    });
    await expectShellReady(page);
    await expect(page.getByTestId("publish-section-status")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByTestId("publish-status-job-4616")).toBeVisible({
      timeout: 15000,
    });
    await expect(
      page.getByTestId("publish-status-detail-job-id"),
    ).toHaveCount(0);
    expect(consoleErrors).toEqual([]);
  });
});
