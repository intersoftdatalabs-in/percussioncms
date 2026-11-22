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
 * Playwright surface: #7005 / parent #4531 — SiteWorkspace incremental
 * approval chrome renders through i18n keys (no bare English).
 *
 * Tags: @publishing-incremental-related-approval @publish
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-incremental-related-approval.spec.js
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

async function stubRelatedApis(page) {
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
    if (req.method() === "GET" && isIncrementalRelatedListUrl(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(
          pagedQueue([
            { id: "301", name: "Home Page" },
            { id: "88", title: "About Us" },
          ]),
        ),
      });
      return;
    }
    if (req.method() === "GET" && isIncrementalContentListUrl(url)) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(pagedQueue([])),
      });
      return;
    }
    await route.continue();
  });
}

async function openPreview(page) {
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
  await expect(page.getByTestId("publish-related-approval")).toBeVisible({
    timeout: 15000,
  });
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

const TAGS = ["@publishing-incremental-related-approval", "@publish"];

test.describe("PublishingShell incremental approval chrome is keyed (#7005)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`preview button, heading, and count resolve through keys ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubRelatedApis(page);
    await openPreview(page);
    await expect(page.getByTestId("publish-incremental-preview-btn")).toHaveText(
      "Incremental preview",
    );
    const panel = page.getByTestId("publish-related-approval");
    await expect(panel).toContainText("Publish Related Items for approval");
    await expect(page.getByTestId("publish-related-approval-count")).toHaveText(
      "Selected for approval: 0 of 2",
    );
    await expect(page.getByTestId("publish-incremental-confirm")).toHaveText(
      "Incremental (with approval)",
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`checking a row updates the keyed count ${TAGS.join(" ")}`, async ({ page }) => {
    const consoleErrors = trackConsole(page);
    await stubRelatedApis(page);
    await openPreview(page);
    await page.getByRole("checkbox", { name: "Approve related 301" }).check();
    await expect(page.getByTestId("publish-related-approval-count")).toHaveText(
      "Selected for approval: 1 of 2",
    );
    await page.getByRole("checkbox", { name: "Select all related items" }).check();
    await expect(page.getByTestId("publish-related-approval-count")).toHaveText(
      "Selected for approval: 2 of 2",
    );
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
