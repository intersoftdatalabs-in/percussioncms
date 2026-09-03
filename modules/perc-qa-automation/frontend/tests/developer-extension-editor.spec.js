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
 * Developer Extensions create / catalog / edit / delete (#4241 SY-01 / parent #1690).
 *
 * SPA catalog (#4240) + REST write (#4239). System rows keep Save/Delete disabled.
 * Live path requires hot-deployed sitemanage with lazy PSExtensionService manager
 * resolve (Spring-before-PSServer init on H2 QA).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-extension-editor.spec.js
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

/** Java-identifier unique extension name. */
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

/**
 * Open a known shipped system UDF (`add` under global/percussion/udf/).
 * Prefer exact data-ex-name over hasText filters — the H2 catalog has hundreds
 * of rows and Playwright text filters over the full table are too slow.
 */
async function openSystemExtension(page) {
  // Prefer data-immutable / global/percussion context (stable across catalog order);
  // fall back to exact name "add" (shipped H2 UDF). Override via SYSTEM_EXTENSION_NAME.
  const systemName = process.env.SYSTEM_EXTENSION_NAME || "add";
  const byImmutable = page.locator(
    '[data-testid="developer-ex-open"][data-immutable="true"]',
  );
  const byContext = page.locator(
    '[data-testid="developer-ex-open"][data-ex-context^="global/percussion"]',
  );
  const byName = page.locator(
    catalogOpenByExactName("developer-ex-open", "data-ex-name", systemName),
  );
  const open =
    (await byImmutable.count()) > 0
      ? byImmutable.first()
      : (await byContext.count()) > 0
        ? byContext.first()
        : byName;
  await expect(open).toBeVisible({ timeout: 30_000 });
  await open.click();
  await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-ex-detail-loading"]')).toHaveCount(0, {
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-ex-immutable-hint"]')).toBeVisible({
    timeout: 10_000,
  });
  await expect(page.locator('[data-testid="developer-ex-save"]')).toBeDisabled();
  await expect(page.locator('[data-testid="developer-ex-delete"]')).toBeDisabled();
}

test.describe("Developer extension editor (#4241 / SY-01)", () => {
  test("catalog opens create chrome and disables save until fields are valid", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openExtensionsCatalog(page);

    await page.locator('[data-testid="developer-ex-new"]').click();
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-ex-save"]');
    await expect(saveBtn).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ex-delete"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-ex-name"]').fill("has space");
    await expect(saveBtn).toBeDisabled();
    const name = uniqueExtensionName("qa4241");
    await page.locator('[data-testid="developer-ex-name"]').fill(name);
    await page
      .locator('[data-testid="developer-ex-interfaces"]')
      .fill("com.percussion.extension.IPSUdfProcessor");
    await expect(saveBtn).toBeDisabled();
    await page
      .locator('[data-testid="developer-ex-classname"]')
      .fill("com.percussion.generic.PSAdd");
    await expect(saveBtn).toBeEnabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin create → catalog → edit → delete user extension; system blocked", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openExtensionsCatalog(page);

    // System row: Save/Delete disabled (data-immutable / exact name, not .first())
    await openSystemExtension(page);
    await page.locator('[data-testid="developer-ex-back"]').click();
    await expect(page.locator('[data-testid="developer-ex-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    const extName = uniqueExtensionName("qa4241");
    await page.locator('[data-testid="developer-ex-new"]').click();
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-ex-name"]').fill(extName);
    await page
      .locator('[data-testid="developer-ex-interfaces"]')
      .fill("com.percussion.extension.IPSUdfProcessor");
    // Class used by ExtensionAdaptorWriteTest fixture (shipped generic UDF).
    await page
      .locator('[data-testid="developer-ex-classname"]')
      .fill("com.percussion.generic.PSAdd");
    await page.locator('[data-testid="developer-ex-save"]').click();

    const notice = page.locator('[data-testid="developer-ex-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-ex-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 30_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-ex-name"]')).toHaveValue(extName);
    await expect(page.locator('[data-testid="developer-ex-name"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ex-delete"]')).toBeEnabled();

    // Catalog list must show the new user extension
    await page.locator('[data-testid="developer-ex-back"]').click();
    await expect(page.locator('[data-testid="developer-ex-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(createdRow(page, extName)).toBeVisible({ timeout: 20_000 });
    await createdRow(page, extName).click();
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();

    await page.locator('[data-testid="developer-ex-deprecated"]').check();
    await page.locator('[data-testid="developer-ex-save"]').click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-ex-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-ex-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    const listNotice = page.locator('[data-testid="developer-ex-list-notice"]');
    await expect(listNotice).toBeVisible({ timeout: 20_000 });
    await expect(listNotice).toContainText(/deleted/i);
    await expect(createdRow(page, extName)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
