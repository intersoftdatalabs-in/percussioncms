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
 * Developer Views Inbox-family / packaged sys_cxViews blocked mutate (#4625 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-view-inbox-family.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerViewsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "views",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

async function openViewsCatalog(page) {
  await page.goto(developerViewsUrl(), { waitUntil: "domcontentloaded" });
  const panel = page.locator('[data-testid="developer-vw-panel"]');
  if ((await panel.count()) === 0) {
    const tab = page.locator('[data-testid="tab-developer-views"]');
    if ((await tab.count()) > 0) {
      await tab.click();
    }
  }
  await expect(page.locator('[data-testid="developer-vw-new"]')).toBeVisible({
    timeout: 20_000,
  });
}

function attachConsoleGuards(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

test.describe("Developer Inbox-family view mutate (#4625 / UI-07)", () => {
  test("Inbox-family rows are protected in the catalog and editor", async ({ page }) => {
    test.setTimeout(90_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openViewsCatalog(page);

    const badge = page.locator('[data-testid="developer-vw-protected-badge"]').first();
    await expect(badge).toBeVisible({ timeout: 20_000 });
    await expect(badge).toContainText(/Protected/i);

    const inboxOpen = page.locator('[data-vw-name="Inbox"]');
    await expect(inboxOpen).toHaveCount(1, { timeout: 20_000 });
    await inboxOpen.click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-vw-protected-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-vw-delete"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-vw-save"]')).toBeDisabled();
    const urlField = page.locator('[data-testid="developer-vw-url"]');
    if ((await urlField.count()) > 0) {
      await expect(urlField).toBeDisabled();
    }

    await page.locator('[data-testid="developer-vw-back"]').click();
    await expect(page.locator('[data-testid="developer-vw-panel"]')).toBeVisible();
    await expect(page.locator('[data-vw-name="Inbox"]')).toHaveCount(1);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("create chrome blocks a packaged sys_cxViews URL", async ({ page }) => {
    test.setTimeout(90_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openViewsCatalog(page);

    await page.locator('[data-testid="developer-vw-new"]').click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-vw-name"]').fill("QaInboxHijack");
    await page.locator('[data-testid="developer-vw-type"]').selectOption("CustomView");
    await page.locator('[data-testid="developer-vw-url"]').fill("../sys_cxViews/inbox.xml");
    await expect(page.locator('[data-testid="developer-vw-protected-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-vw-save"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-vw-delete"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
