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
 * Playwright surface: #4887 / parent #4531 — PublishingShell retry of a failed
 * status job (confirm; same site and server; full vs incremental from payload).
 *
 * Tags: @publishing-retry-job @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-retry-job.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  publishingProductUrl,
  isCurrentStatusUrl,
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-cancel-job");

const TAGS = ["@publishing-retry-job", "@publish", "@smoke"];

const JOBS = [
  {
    jobId: 4887,
    siteName: "FastForward",
    pubServerName: "Production",
    status: "Failed",
    publishKind: "incremental",
    completedItems: 1,
    totalItems: 4,
  },
  {
    jobId: 50,
    siteName: "FastForward",
    pubServerName: "Production",
    status: "Completed with failures",
    publishKind: "full",
    completedItems: 2,
    totalItems: 4,
  },
  {
    jobId: 51,
    siteName: "FastForward",
    status: "Failed",
    completedItems: 0,
    totalItems: 1,
  },
  {
    jobId: 52,
    siteName: "DoneSite",
    pubServerName: "Production",
    status: "Completed",
    completedItems: 3,
    totalItems: 3,
  },
];

function isIncrementalPublishUrl(url) {
  return /\/sitemanage\/publish\/incremental\/publish\//i.test(String(url || ""));
}

function isFullPublishUrl(url) {
  const raw = String(url || "");
  if (!/\/sitemanage\/publish\//i.test(raw)) {
    return false;
  }
  return !/\/incremental\//i.test(raw) && !/\/pubstatus\//i.test(raw);
}

test.describe("PublishingShell retry failed status job (#4887)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`retry only failed jobs with site and server ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = [];
    const publishes = [];
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

    await page.route("**/sitemanage/publish/**", async (route) => {
      const url = route.request().url();
      if (route.request().method() === "GET" && isIncrementalPublishUrl(url)) {
        publishes.push({ kind: "incremental", url });
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ status: "Queuing content", jobid: 900 }),
        });
        return;
      }
      if (route.request().method() === "GET" && isFullPublishUrl(url)) {
        publishes.push({ kind: "full", url });
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ status: "Queuing content", jobid: 901 }),
        });
        return;
      }
      await route.continue();
    });
    await page.route("**/sitemanage/pubstatus/current**", async (route) => {
      if (
        isCurrentStatusUrl(route.request().url()) &&
        route.request().method() === "GET"
      ) {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(JOBS),
        });
        return;
      }
      await route.continue();
    });

    await page.goto(publishingProductUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-retry-job-4887")).toBeVisible({
      timeout: 15000,
    });
    await expect(page.getByTestId("publish-retry-job-50")).toBeVisible();
    await expect(page.getByTestId("publish-retry-job-51")).toHaveCount(0);
    await expect(page.getByTestId("publish-retry-job-52")).toHaveCount(0);

    page.once("dialog", (dialog) => dialog.dismiss());
    await page.getByTestId("publish-retry-job-4887").click();
    expect(publishes.length).toBe(0);

    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("publish-retry-job-4887").click();
    await expect.poll(() => publishes.length).toBe(1);
    expect(publishes[0].kind).toBe("incremental");
    expect(publishes[0].url).toMatch(/FastForward\/Production/i);

    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("publish-retry-job-50").click();
    await expect.poll(() => publishes.length).toBe(2);
    expect(publishes[1].kind).toBe("full");
    expect(consoleErrors).toEqual([]);
  });

  test(`HTTP 200 BADCONFIG on retry is an error ${TAGS.join(" ")}`, async ({
    page,
  }) => {
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

    await page.route("**/sitemanage/publish/**", async (route) => {
      const url = route.request().url();
      if (route.request().method() === "GET" && isFullPublishUrl(url)) {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            status: "BADCONFIG",
            warningMessage: "missing host",
          }),
        });
        return;
      }
      await route.continue();
    });
    await page.route("**/sitemanage/pubstatus/current**", async (route) => {
      if (
        isCurrentStatusUrl(route.request().url()) &&
        route.request().method() === "GET"
      ) {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(JOBS),
        });
        return;
      }
      await route.continue();
    });

    await page.goto(publishingProductUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publish-retry-job-50")).toBeVisible({
      timeout: 30000,
    });
    page.once("dialog", (dialog) => dialog.accept());
    await page.getByTestId("publish-retry-job-50").click();
    await expect(page.getByRole("alert")).toContainText(/missing host/i, {
      timeout: 15000,
    });
    expect(consoleErrors).toEqual([]);
  });
});
