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
 * Developer Locales auto-translation table editor (#4028 CD-18 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-auto-translation-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerLocalesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "locales",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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

async function openLocalesCatalog(page) {
  await page.goto(developerLocalesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-loc-panel"]');
  const empty = page.locator('[data-testid="developer-loc-empty"]');
  const listError = page.locator('[data-testid="developer-loc-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer locales catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-at-open"]')).toBeVisible();
}

async function openAutoTranslations(page) {
  await page.locator('[data-testid="developer-at-open"]').click();
  await expect(page.locator('[data-testid="developer-at-panel"]')).toBeVisible({
    timeout: 20_000,
  });
  const loadErr = page.locator('[data-testid="developer-at-error"]');
  const table = page.locator('[data-testid="developer-at-table"]');
  const empty = page.locator('[data-testid="developer-at-empty"]');
  await expect(table.or(empty).or(loadErr).first()).toBeVisible({ timeout: 30_000 });
  if (await loadErr.isVisible() && !(await table.isVisible()) && !(await empty.isVisible())) {
    throw new Error(
      `Auto-translations failed to load: ${(await loadErr.innerText()).trim()}`,
    );
  }
}

async function snapshotRows(page) {
  const count = await page.locator('[data-testid^="developer-at-row-"]').count();
  const rows = [];
  for (let i = 0; i < count; i++) {
    rows.push({
      locale: await page.locator(`[data-testid="developer-at-locale-${i}"]`).inputValue(),
      type: await page.locator(`[data-testid="developer-at-type-${i}"]`).inputValue(),
      workflow: await page.locator(`[data-testid="developer-at-workflow-${i}"]`).inputValue(),
      community: await page.locator(`[data-testid="developer-at-community-${i}"]`).inputValue(),
    });
  }
  return rows;
}

async function fillRow(page, index, values) {
  await page.locator(`[data-testid="developer-at-locale-${index}"]`).fill(values.locale);
  await page.locator(`[data-testid="developer-at-type-${index}"]`).fill(values.type);
  await page.locator(`[data-testid="developer-at-workflow-${index}"]`).fill(values.workflow);
  await page.locator(`[data-testid="developer-at-community-${index}"]`).fill(values.community);
}

test.describe("Developer auto-translation editor (#4028 / CD-18)", () => {
  test("Admin can add a locale×content-type row, save, and round-trip", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openLocalesCatalog(page);
    await openAutoTranslations(page);

    let lastPutJson = "{\"AutoTranslationRow\":[]}";
    await page.route("**/services/locales/auto-translations", async (route) => {
      const method = route.request().method();
      if (method === "PUT") {
        lastPutJson = route.request().postData() || lastPutJson;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: lastPutJson,
        });
        return;
      }
      if (method === "GET" && lastPutJson && lastPutJson !== "{\"AutoTranslationRow\":[]}") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: lastPutJson,
        });
        return;
      }
      await route.continue();
    });

    const original = await snapshotRows(page);
    await page.locator('[data-testid="developer-at-add"]').click();
    const index = original.length;
    await expect(page.locator(`[data-testid="developer-at-row-${index}"]`)).toBeVisible();

    const locale = "en-us";
    const type =
      (await page.locator("#developer-at-type-options option").first().getAttribute("value")) ||
      "percPage";
    const workflow =
      (await page
        .locator("#developer-at-workflow-options option")
        .first()
        .getAttribute("value")) || "Default Workflow";
    const community =
      (await page
        .locator("#developer-at-community-options option")
        .first()
        .getAttribute("value")) || "Default";

    await fillRow(page, index, { locale, type, workflow, community });
    const saveBtn = page.locator('[data-testid="developer-at-save"]');
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-at-notice"]');
    const saveError = page.locator('[data-testid="developer-at-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    await page.locator('[data-testid="developer-at-back"]').click();
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await openAutoTranslations(page);
    await expect(
      page.locator(`[data-at-locale="${locale}"][data-at-type="${type}"]`),
    ).toHaveCount(1);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("unknown locale/type 400 and lock 409 are surfaced", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openLocalesCatalog(page);
    await openAutoTranslations(page);

    await page.route("**/services/locales/auto-translations", async (route) => {
      if (route.request().method() !== "PUT") {
        await route.continue();
        return;
      }
      const body = route.request().postData() || "";
      if (/xx-unknown-4028/i.test(body)) {
        await route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({ message: "unknown locale: xx-unknown-4028" }),
        });
        return;
      }
      if (/lock-probe-4028/i.test(body)) {
        await route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({
            message: "Could not save auto-translations; locked by other",
          }),
        });
        return;
      }
      await route.continue();
    });

    await page.locator('[data-testid="developer-at-add"]').click();
    const idx = (await page.locator('[data-testid^="developer-at-row-"]').count()) - 1;
    await fillRow(page, idx, {
      locale: "xx-unknown-4028",
      type: "percPage",
      workflow: "Default Workflow",
      community: "Default",
    });
    await page.locator('[data-testid="developer-at-save"]').click();
    const err = page.locator('[data-testid="developer-at-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/unknown locale|400/i);

    await fillRow(page, idx, {
      locale: "en-us",
      type: "lock-probe-4028",
      workflow: "Default Workflow",
      community: "Default",
    });
    await page.locator('[data-testid="developer-at-save"]').click();
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/locked|409/i);

    await page.locator(`[data-testid="developer-at-remove-${idx}"]`).click();

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
