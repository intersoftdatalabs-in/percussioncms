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
 * Developer content type child field-set order (#6464 / parent #1690).
 *
 * Folder's packaged `acl` complex-child set (type, name, permissions) is the
 * live fixture: unlocked child move stays disabled; lock, move a child field,
 * reset (no PUT), move again, save persists the child order; reload shows the
 * new order; restore the saved order; a forced 403 stays on the panel and
 * keeps the unsaved child order. Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-field-child-order.spec.js
 *   perc-devctl qa-down
 * </pre>
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
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

function childRowNames(page) {
  return page.locator(
    '[data-testid="developer-ct-field-row"]:has([data-testid^="developer-ct-child-field-up-"])',
  );
}

const ACL_TRIO = ["type", "name", "permissions"];

async function aclNames(page) {
  const all = await childRowNames(page).evaluateAll((rows) =>
    rows.map((r) => r.getAttribute("data-field-name")),
  );
  return all.filter((n) => ACL_TRIO.includes(n));
}

async function openFolderDetail(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const row = page
    .locator('[data-testid^="developer-ct-row-"]')
    .filter({ hasText: "Folder" })
    .first();
  await expect(row).toBeVisible({ timeout: 20_000 });
  const openBtn = row.locator('button[aria-label^="Open "]');
  if (await openBtn.count()) {
    await openBtn.click();
  } else {
    await row.click();
  }
  await expect(page.locator('[data-testid="developer-ct-fields"]')).toBeVisible({
    timeout: 30_000,
  });
}

test.describe("Developer content type child field-set order (#6464)", () => {
  test("unlocked move disabled; reset does not save; save persists child order; 403 stays", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openFolderDetail(page);

    const initial = await aclNames(page);
    expect(initial.length).toBe(3);
    const [firstName, secondName] = initial;
    const down = page.locator(`[data-testid="developer-ct-child-field-down-${firstName}"]`);
    const reset = page.locator('[data-testid="developer-ct-field-order-reset"]');
    await expect(down).toBeDisabled();
    await expect(reset).toBeDisabled();

    await page.locator('[data-testid="developer-ct-lock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      "Locked by you",
      { timeout: 20_000 },
    );
    await expect(down).toBeEnabled();

    let savePuts = 0;
    page.on("request", (req) => {
      if (req.method() === "PUT" && /\/contenttypes\/[^/]+$/.test(req.url().split("?")[0])) {
        savePuts += 1;
      }
    });

    await down.click();
    const moved = await aclNames(page);
    expect(moved).toEqual([secondName, firstName, initial[2]]);
    const putsAfterMove = savePuts;
    await reset.click();
    expect(await aclNames(page)).toEqual(initial);
    expect(savePuts).toBe(putsAfterMove);

    await down.click();
    await page.locator('[data-testid="developer-ct-save"]').click();
    await expect(page.locator('[data-testid="developer-ct-detail-notice"]')).toBeVisible({
      timeout: 60_000,
    });
    await page.locator('[data-testid="developer-ct-unlock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      "Not locked",
      { timeout: 20_000 },
    );
    await page.locator('[data-testid="developer-ct-back"]').click();
    await expect(page.locator('[data-testid="developer-ct-table"]')).toBeVisible({
      timeout: 20_000,
    });
    await openFolderDetail(page);
    expect(await aclNames(page)).toEqual([secondName, firstName, initial[2]]);
    await page.locator('[data-testid="developer-ct-fields"]').scrollIntoViewIfNeeded();
    await page.screenshot({
      path: "../../../docs/ai-generated/proofs/issue-6464-child-fieldset-order.png",
    });

    // Restore the saved order.
    await page.locator('[data-testid="developer-ct-lock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      "Locked by you",
      { timeout: 20_000 },
    );
    const restore = page.locator(`[data-testid="developer-ct-child-field-up-${firstName}"]`);
    await expect(restore).toBeEnabled();
    await restore.click();
    expect(await aclNames(page)).toEqual(initial);
    await page.locator('[data-testid="developer-ct-save"]').click();
    await expect(page.locator('[data-testid="developer-ct-detail-notice"]')).toBeVisible({
      timeout: 60_000,
    });
    expect(await aclNames(page)).toEqual(initial);

    // Forced 403: error stays, unsaved child order is not reverted, lock held.
    await page.locator(`[data-testid="developer-ct-child-field-down-${firstName}"]`).click();
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
    expect(await aclNames(page)).toEqual([secondName, firstName, initial[2]]);
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      "Locked by you",
    );

    await page.unroute("**/services/contenttypes/**");
    await page.locator('[data-testid="developer-ct-field-order-reset"]').click();
    expect(await aclNames(page)).toEqual(initial);
    await page.locator('[data-testid="developer-ct-unlock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      "Not locked",
      { timeout: 20_000 },
    );

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
