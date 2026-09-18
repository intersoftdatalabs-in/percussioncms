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
 * Developer Extensions method-map editor (#4543 / parent #1690 slice 17).
 *
 * Admin add / update / clear of Extension.methods on Developer → Extensions.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-extension-method-map.spec.js
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

test.describe("Developer extension method map (#4543)", () => {
  test("Admin add, GET round-trip, and clear method map; system editor disabled", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openExtensionsCatalog(page);

    const byImmutable = page.locator(
      '[data-testid="developer-ex-open"][data-immutable="true"]',
    );
    if ((await byImmutable.count()) > 0) {
      await byImmutable.first().click();
      await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-ex-method-add"]')).toBeDisabled();
      await page.locator('[data-testid="developer-ex-back"]').click();
      await expect(page.locator('[data-testid="developer-ex-panel"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    const extName = uniqueExtensionName("qa4543");
    await page.locator('[data-testid="developer-ex-new"]').click();
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-ex-name"]').fill(extName);
    await page
      .locator('[data-testid="developer-ex-interfaces"]')
      .fill("com.percussion.extension.IPSUdfProcessor");
    await page
      .locator('[data-testid="developer-ex-classname"]')
      .fill("com.percussion.generic.PSAdd");
    await page.locator('[data-testid="developer-ex-method-add"]').click();
    await page.locator('[data-testid="developer-ex-method-name-0"]').fill("productVersion");
    await page
      .locator('[data-testid="developer-ex-method-return-0"]')
      .fill("java.lang.String");
    await page.locator('[data-testid="developer-ex-method-desc-0"]').fill("version string");
    await page.locator('[data-testid="developer-ex-method-param-add-0"]').click();
    await page.locator('[data-testid="developer-ex-method-param-name-0-0"]').fill("n");
    await page.locator('[data-testid="developer-ex-method-param-type-0-0"]').fill("int");
    await page.locator('[data-testid="developer-ex-save"]').click();

    const notice = page.locator('[data-testid="developer-ex-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-ex-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 30_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create with methods failed: ${(await saveError.innerText()).trim()}`);
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
    expect(raw, `GET detail missing methods: ${raw.slice(0, 800)}`).toMatch(
      /productVersion|"methods"/i,
    );
    await expect(page.locator('[data-testid="developer-ex-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-ex-detail-loading"]')).toHaveCount(0, {
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-ex-method-name-0"]')).toHaveValue(
      "productVersion",
    );

    await page.locator('[data-testid="developer-ex-method-remove-0"]').click();
    await expect(page.locator('[data-testid="developer-ex-methods-empty"]')).toBeVisible();
    await page.locator('[data-testid="developer-ex-save"]').click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Clear methods failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-ex-methods-empty"]')).toBeVisible();

    await page.locator('[data-testid="developer-ex-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-ex-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
