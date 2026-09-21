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
 * Developer Extensions init-parameter dialog (#4624 / parent #1690 slice 26).
 *
 * Admin add / GET round-trip / clear of extra Extension.initParameters via
 * Developer → Extensions Workbench-parity dialog.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-extension-init-params.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

function developerExtensionsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "extensions",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueExtensionName(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}${suffix || "x"}`;
}

async function openExtensionsCatalog(page) {
  await page.goto(developerExtensionsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-ex-panel"]');
  const empty = page.locator('[data-testid="developer-ex-empty"]');
  const listError = page.locator('[data-testid="developer-ex-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer extensions catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-ex-new"]')).toBeVisible();
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

function createdRow(page, extName) {
  return page.locator(
    catalogOpenByExactName("developer-ex-open", "data-ex-name", extName),
  );
}

test.describe("Developer extension init parameters (#4624)", () => {
  test("Admin add, GET round-trip, and clear extra init parameters", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openExtensionsCatalog(page);

    const extName = uniqueExtensionName("qa4624");
    await page.locator('[data-testid="developer-ex-new"]').click();
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-ex-name"]').fill(extName);
    await page
      .locator('[data-testid="developer-ex-interfaces"]')
      .fill("com.percussion.extension.IPSUdfProcessor");
    await page
      .locator('[data-testid="developer-ex-classname"]')
      .fill("com.percussion.generic.PSAdd");
    await page.locator('[data-testid="developer-ex-init-open"]').click();
    await expect(page.locator('[data-testid="developer-ex-init-dialog"]')).toBeVisible();
    await page.locator('[data-testid="developer-ex-init-add"]').click();
    await page
      .locator('[data-testid="developer-ex-init-key-0"]')
      .fill("com.percussion.user.description");
    await page.locator('[data-testid="developer-ex-init-value-0"]').fill("qa4624 dialog");
    await page.locator('[data-testid="developer-ex-init-apply"]').click();
    await expect(page.locator('[data-testid="developer-ex-init-dialog"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-ex-init-summary-0"]')).toContainText(
      "com.percussion.user.description",
    );
    await page.locator('[data-testid="developer-ex-save"]').click();

    const notice = page.locator('[data-testid="developer-ex-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-ex-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 30_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create with init params failed: ${(await saveError.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-ex-back"]').click();
    await expect(page.locator('[data-testid="developer-ex-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(createdRow(page, extName)).toBeVisible({ timeout: 20_000 });
    const [detailResp] = await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes("/extensions/catalog/item") && r.ok(),
        { timeout: 20_000 },
      ),
      createdRow(page, extName).click(),
    ]);
    const detailJson = await detailResp.json();
    const raw = JSON.stringify(detailJson);
    expect(raw, `GET detail missing init description: ${raw.slice(0, 800)}`).toMatch(
      /qa4624 dialog|com\.percussion\.user\.description/i,
    );
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-ex-detail-loading"]')).toHaveCount(0, {
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-ex-init-summary-0"]')).toContainText(
      "qa4624 dialog",
    );

    await page.locator('[data-testid="developer-ex-init-open"]').click();
    await page.locator('[data-testid="developer-ex-init-remove-0"]').click();
    await page.locator('[data-testid="developer-ex-init-apply"]').click();
    await expect(page.locator('[data-testid="developer-ex-init-empty"]')).toBeVisible();
    await page.locator('[data-testid="developer-ex-save"]').click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Clear init params failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-ex-init-empty"]')).toBeVisible();

    await page.locator('[data-testid="developer-ex-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-ex-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
