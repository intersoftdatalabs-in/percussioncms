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
 * Playwright surface: #6471 / parent #4531 — Site workspace incremental
 * queue filter presets (My items / Failed only / Scheduled only) with a
 * persisted preference. The presets narrow already-loaded rows client-side
 * (zero new REST); the choice is remembered in localStorage across reloads.
 *
 * Tags: @publishing-incremental-queue @publish
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-incremental-queue-presets.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  publishingProductUrl,
  isPubServersListUrl,
  isIncrementalContentListUrl,
  isIncrementalRelatedListUrl,
  mockPublishServer,
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-incremental-site");

function pagedQueue(children) {
  return {
    PagedItemList: {
      childrenInPage: children,
      childrenCount: children.length,
    },
  };
}

async function stubQueueApis(
  page,
  { contentBody, currentUserBody, currentUserStatus = 200 },
) {
  await page.route("**/publishmanagement/servers/**", async (route) => {
    const req = route.request();
    if (req.method() === "GET" && isPubServersListUrl(req.url())) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ PubServer: [mockPublishServer()] }),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/sitemanage/publish/incremental/**", async (route) => {
    const req = route.request();
    const url = req.url();
    if (req.method() === "GET" && isIncrementalContentListUrl(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(contentBody),
      });
      return;
    }
    if (req.method() === "GET" && isIncrementalRelatedListUrl(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(pagedQueue([])),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/user/user/current**", async (route) => {
    if (currentUserStatus !== 200) {
      await route.fulfill({ status: currentUserStatus, body: "denied" });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(
        currentUserBody ?? { CurrentUser: { name: "Admin", email: "" } },
      ),
    });
  });
}

async function openQueuePreview(page) {
  await page.goto(publishingProductUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  const siteCard = page.locator('[data-testid^="publish-site-card-"]').first();
  await expect(siteCard).toBeVisible({ timeout: 30000 });
  await siteCard.click();
  await expect(page.getByTestId("publish-site-workspace")).toBeVisible();
  const preview = page.getByTestId("publish-incremental-preview-btn");
  await expect(preview).toBeEnabled({ timeout: 15000 });
  await preview.click();
  const list = page.getByTestId("publish-incremental-queue-list");
  await expect(list).toBeVisible({ timeout: 15000 });
}

function trackConsole(page) {
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

const TAGS = ["@publishing-incremental-queue", "@publish"];

const STUB_ROWS = [
  { id: "301", name: "Home Page", owner: "Admin", status: "Approved" },
  { id: "88", title: "About Us", createdBy: "editor1", status: "Failed" },
  {
    id: "89",
    title: "Contact",
    owner: "Admin",
    scheduledDate: "2026-11-20",
  },
];

test.describe("PublishingShell incremental queue presets (#6471)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`mine failed scheduled presets narrow rows with a count ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubQueueApis(page, { contentBody: pagedQueue(STUB_ROWS) });
    await openQueuePreview(page);
    const rows = page.getByTestId("publish-incremental-queue-row");
    await expect(rows).toHaveCount(3);
    const preset = page.getByTestId("publish-incremental-queue-preset-filter");
    await expect(preset).toBeVisible();
    await expect(
      page.getByTestId("publish-incremental-queue-preset-count"),
    ).toContainText("3 of 3");

    await preset.selectOption("failed");
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText("88");

    await preset.selectOption("scheduled");
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText("89");

    await preset.selectOption("mine");
    await expect(rows).toHaveCount(2);
    await expect(rows.first()).toContainText("301");

    await preset.selectOption("all");
    await expect(rows).toHaveCount(3);
    await page.screenshot({
      path: "test-results/live-proof-6471-queue-presets.png",
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`preset choice persists across reload ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubQueueApis(page, { contentBody: pagedQueue(STUB_ROWS) });
    await openQueuePreview(page);
    const preset = page.getByTestId("publish-incremental-queue-preset-filter");
    await preset.selectOption("failed");
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(
      1,
    );

    await page.reload({ waitUntil: "domcontentloaded" });
    await openQueuePreview(page);
    await expect(
      page.getByTestId("publish-incremental-queue-preset-filter"),
    ).toHaveValue("failed");
    await expect(page.getByTestId("publish-incremental-queue-row")).toHaveCount(
      1,
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`mine explains itself when the sign-in name is unknown ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubQueueApis(page, {
      contentBody: pagedQueue(STUB_ROWS),
      currentUserStatus: 500,
    });
    await openQueuePreview(page);
    const preset = page.getByTestId("publish-incremental-queue-preset-filter");
    await preset.selectOption("mine");
    await expect(
      page.getByTestId("publish-incremental-queue-preset-mine-unknown"),
    ).toBeVisible({ timeout: 15000 });
    await expect(
      page.getByTestId("publish-incremental-queue-row"),
    ).toHaveCount(0);
    // The stub intentionally 500s /user/user/current so the workspace takes
    // the unknown-operator path; the browser logs those resource loads as
    // console errors. Everything else must stay clean.
    const unexpected = consoleErrors.filter(
      (msg) => !msg.startsWith("Failed to load resource"),
    );
    expect(unexpected, unexpected.join("\n")).toEqual([]);
  });
});
