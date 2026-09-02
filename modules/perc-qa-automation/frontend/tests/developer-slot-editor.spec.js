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
 * Developer Slots create / delete chrome (#4056 AS-01 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-slot-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerSlotsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "slots",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** Unique slot name: no spaces (REST create rule). */
function uniqueSlotName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4056${suffix || "slot"}`;
}

async function openSlotsCatalog(page) {
  await page.goto(developerSlotsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-slot-panel"]');
  const empty = page.locator('[data-testid="developer-slot-empty"]');
  const listError = page.locator('[data-testid="developer-slot-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer slots catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-slot-new"]')).toBeVisible();
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

test.describe("Developer slot editor (#4056 / AS-01)", () => {
  test("Admin can create a uniquely named slot and delete it", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openSlotsCatalog(page);

    const name = uniqueSlotName();
    const label = `QA 4056 ${name}`;

    await page.locator('[data-testid="developer-slot-new"]').click();
    await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-slot-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-slot-name"]').fill("my slot");
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-slot-name"]').fill(name);
    await page.locator('[data-testid="developer-slot-label"]').fill(label);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-slot-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(notice).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-slot-name"]')).toBeDisabled({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-slot-name"]')).toHaveValue(name);

    await page.locator('[data-testid="developer-slot-back"]').click();
    await expect(page.locator('[data-testid="developer-slot-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-slot-name="${name}"]`)).toBeVisible({
      timeout: 20_000,
    });

    await page.locator(`[data-slot-name="${name}"]`).click();
    await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-slot-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-slot-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-slot-name="${name}"]`)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openSlotsCatalog(page);

    const name = uniqueSlotName();
    await page.locator('[data-testid="developer-slot-new"]').click();
    await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-slot-name"]').fill(name);
    await page.locator('[data-testid="developer-slot-label"]').fill(`QA dup ${name}`);
    await page.locator('[data-testid="developer-slot-save"]').click();
    await expect(page.locator('[data-testid="developer-slot-detail-notice"]')).toBeVisible({
      timeout: 20_000,
    });

    await page.locator('[data-testid="developer-slot-back"]').click();
    await expect(page.locator('[data-testid="developer-slot-new"]')).toBeVisible();
    await page.locator('[data-testid="developer-slot-new"]').click();
    await page.locator('[data-testid="developer-slot-name"]').fill(name);
    await page.locator('[data-testid="developer-slot-label"]').fill("Duplicate slot");
    await page.locator('[data-testid="developer-slot-save"]').click();

    const err = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    await page.locator('[data-testid="developer-slot-back"]').click();
    const openCreated = page.locator(`[data-slot-name="${name}"]`);
    if (await openCreated.count()) {
      await openCreated.click();
      await expect(page.locator('[data-testid="developer-slot-delete"]')).toBeVisible();
      await page.locator('[data-testid="developer-slot-delete"]').click();
      await confirmDeveloperCatalogDelete(page);
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("system slot delete is 409", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openSlotsCatalog(page);

    const sysOpen = page.locator('[data-slot-name="sys_inline_link"]');
    const autoIndex = page.locator('[data-slot-name="sys_AutoIndex"]');
    const target = (await sysOpen.count()) > 0 ? sysOpen : autoIndex;
    if ((await target.count()) === 0) {
      test.skip(true, "No known system slot (sys_inline_link / sys_AutoIndex) in catalog");
      return;
    }

    await target.first().click();
    await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-slot-delete"]')).toBeVisible();
    await page.locator('[data-testid="developer-slot-delete"]').click();
    await confirmDeveloperCatalogDelete(page);

    const err = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/system|409|cannot be deleted/i);
    await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible();

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
