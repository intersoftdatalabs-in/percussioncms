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
 * Developer content type parent field reorder (#4894 / parent #1690).
 *
 * Lock, move a parent field, reset (no save), move again, save, reload shows
 * the new order. Unlocked move stays disabled. A forced 403 stays on the panel.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
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
  expect(unexpectedConsole, `console error: ${unexpectedConsole.join(" | ")}`).toEqual([]);
}

async function openFirstContentType(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const table = page.locator('[data-testid="developer-ct-table"]');
  await expect(table).toBeVisible({ timeout: 30_000 });
  const rows = table.locator('[data-testid^="developer-ct-row-"]');
  const total = await rows.count();
  if (total < 1) {
    throw new Error("No content type catalog rows");
  }
  let opened = false;
  for (let i = 0; i < total; i++) {
    const row = rows.nth(i);
    const openBtn = row.locator('button[aria-label^="Open "]');
    if (await openBtn.count()) {
      await openBtn.click();
    } else {
      await row.click();
    }
    const fields = page.locator('[data-testid="developer-ct-fields"]');
    const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
    await expect(fields.or(detailError).first()).toBeVisible({ timeout: 30_000 });
    const parentCount = await parentRowNames(page).count();
    if (await fields.isVisible() && parentCount > 1) {
      await page.locator('[data-testid="developer-ct-lock"]').click();
      const status = page.locator('[data-testid="developer-ct-lock-status"]');
      try {
        await expect(status).toHaveText("Locked by you", { timeout: 8_000 });
        opened = true;
        break;
      } catch (err) {
        // Another session may still hold this type (409). Try the next row.
      }
    }
    const back = page.locator('[data-testid="developer-ct-back"]');
    if (await back.count()) {
      await back.click();
      await expect(table).toBeVisible({ timeout: 15_000 });
    }
  }
  if (!opened) {
    throw new Error("No content type could be locked with two parent fields");
  }
}

function parentRowNames(page) {
  return page.locator(
    '[data-testid="developer-ct-field-row"]:has([data-testid^="developer-ct-field-up-"])',
  );
}

test.describe("Developer content type field order (#4894)", () => {
  test("unlocked move stays disabled; reset does not save; save persists order; 403 stays", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openFirstContentType(page);

    const rows = parentRowNames(page);
    await expect(rows.first()).toBeVisible();
    const count = await rows.count();
    expect(count, "parent fields").toBeGreaterThan(1);
    const firstName = await rows.nth(0).getAttribute("data-field-name");
    const secondName = await rows.nth(1).getAttribute("data-field-name");
    const down = page.locator(`[data-testid="developer-ct-field-down-${firstName}"]`);
    const reset = page.locator('[data-testid="developer-ct-field-order-reset"]');
    await expect(reset).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      "Locked by you",
    );
    await expect(down).toBeEnabled();
    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();

    let savePuts = 0;
    page.on("request", (req) => {
      if (req.method() === "PUT" && /\/contenttypes\/[^/]+$/.test(req.url().split("?")[0])) {
        savePuts += 1;
      }
    });

    await down.click();
    await expect(parentRowNames(page).nth(0)).toHaveAttribute("data-field-name", secondName);
    const putsAfterMove = savePuts;
    await reset.click();
    await expect(parentRowNames(page).nth(0)).toHaveAttribute("data-field-name", firstName);
    expect(savePuts).toBe(putsAfterMove);

    await down.click();
    await page.locator('[data-testid="developer-ct-save"]').click();
    await expect(page.locator('[data-testid="developer-ct-detail-notice"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.locator('[data-testid="developer-ct-unlock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText("Not locked", {
      timeout: 20_000,
    });
    await page.locator('[data-testid="developer-ct-back"]').click();
    await expect(page.locator('[data-testid="developer-ct-table"]')).toBeVisible({
      timeout: 20_000,
    });
    const same = page
      .locator('[data-testid^="developer-ct-row-"]')
      .filter({ hasText: typeName })
      .first();
    await same.locator('button[aria-label^="Open "]').click();
    await expect(page.locator('[data-testid="developer-ct-fields"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(parentRowNames(page).nth(0)).toHaveAttribute("data-field-name", secondName, {
      timeout: 20_000,
    });

    await page.locator('[data-testid="developer-ct-lock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      "Locked by you",
      { timeout: 20_000 },
    );
    const restore = page.locator(`[data-testid="developer-ct-field-up-${firstName}"]`);
    await expect(restore).toBeEnabled();
    await restore.click();
    await page.locator('[data-testid="developer-ct-save"]').click();
    await expect(page.locator('[data-testid="developer-ct-detail-notice"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(parentRowNames(page).nth(0)).toHaveAttribute("data-field-name", firstName);

    await page.locator(`[data-testid="developer-ct-field-down-${firstName}"]`).click();
    await page.route("**/services/contenttypes/**", async (route) => {
      const req = route.request();
      const path = req.url().split("?")[0];
      if (req.method() === "PUT" && /\/contenttypes\/[^/]+$/.test(path)) {
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "Admin role is required" }),
        });
        return;
      }
      await route.continue();
    });
    await page.locator('[data-testid="developer-ct-save"]').click();
    await expect(page.locator('[data-testid="developer-ct-detail-error"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(parentRowNames(page).nth(0)).toHaveAttribute("data-field-name", secondName);
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      "Locked by you",
    );

    await page.unroute("**/services/contenttypes/**");
    await page.locator('[data-testid="developer-ct-field-order-reset"]').click();
    await expect(parentRowNames(page).nth(0)).toHaveAttribute("data-field-name", firstName);
    await page.locator('[data-testid="developer-ct-unlock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText("Not locked", {
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
