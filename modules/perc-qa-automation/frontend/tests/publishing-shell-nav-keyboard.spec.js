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
 * Playwright surface: parent #4531 — PublishingShell section-nav keyboard
 * operation (roving-tabindex single tab stop, ArrowLeft/ArrowRight with
 * wrap, Home/End jumps, automatic activation).
 *
 * Tags: @publishing-shell-nav-keyboard @publish @smoke
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-shell-nav-keyboard.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  isPubstatusCurrentUrl,
  isPubstatusLogsUrl,
  isPubstatusDetailsUrl,
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-deep-links");

const TAGS = ["@publishing-shell-nav-keyboard", "@publish", "@smoke"];

function publishingShellUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/+$/, "");
  return `${root}/cm/app/spa.jsp?entry=publish`;
}

async function mockShellApis(page) {
  await page.route("**/sitemanage/site/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ SiteSummary: [] }),
    });
  });
  await page.route("**/sitemanage/pubstatus/**", async (route) => {
    const url = route.request().url();
    const method = route.request().method();
    if (isPubstatusLogsUrl(url) && method === "POST") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([]),
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
        body: JSON.stringify([]),
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

async function focusedNavTestId(page) {
  return page.evaluate(() => {
    const el = document.activeElement;
    return el ? el.getAttribute("data-testid") : null;
  });
}

test.describe("PublishingShell section-nav keyboard operation (#4531)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
    await mockShellApis(page);
  });

  test(`header subtitle resolves through the PUBLISH_SUBTITLE key ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = collectConsoleErrors(page);
    await page.goto(publishingShellUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publishing-shell-subtitle")).toHaveText(
      "Sites & servers first — Design and Runtime available when needed.",
      { timeout: 15000 },
    );
    await page.screenshot({
      path: "test-results/live-proof-6894-shell-subtitle.png",
    });
    expect(consoleErrors).toEqual([]);
  });

  test(`arrow keys walk the sections with wrap ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = collectConsoleErrors(page);
    await page.goto(publishingShellUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-nav-sites")).toBeVisible({
      timeout: 15000,
    });

    await page.getByTestId("publish-nav-sites").click();
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 15000,
    });

    await page.keyboard.press("ArrowRight");
    await expect(page.getByTestId("publish-section-status")).toBeVisible({
      timeout: 15000,
    });
    expect(await focusedNavTestId(page)).toBe("publish-nav-status");

    await page.keyboard.press("ArrowRight");
    await expect(page.getByTestId("publish-section-logs")).toBeVisible({
      timeout: 15000,
    });
    expect(await focusedNavTestId(page)).toBe("publish-nav-logs");

    await page.keyboard.press("ArrowLeft");
    await expect(page.getByTestId("publish-section-status")).toBeVisible({
      timeout: 15000,
    });
    expect(await focusedNavTestId(page)).toBe("publish-nav-status");

    expect(consoleErrors).toEqual([]);
  });

  test(`Home and End jump across the nav ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = collectConsoleErrors(page);
    await page.goto(publishingShellUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await expect(page.getByTestId("publishing-shell")).toBeVisible({
      timeout: 30000,
    });
    await expect(page.getByTestId("publish-nav-sites")).toBeVisible({
      timeout: 15000,
    });

    await page.getByTestId("publish-nav-sites").click();
    await page.keyboard.press("End");
    expect(await focusedNavTestId(page)).toBe("publish-nav-runtime");

    await page.keyboard.press("Home");
    await expect(page.getByTestId("publish-section-sites")).toBeVisible({
      timeout: 15000,
    });
    expect(await focusedNavTestId(page)).toBe("publish-nav-sites");

    expect(consoleErrors).toEqual([]);
  });
});
