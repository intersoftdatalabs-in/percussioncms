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
 * Developer Communities delete-one flow (#6064 / parent #1690).
 *
 * Admin confirms and deletes one non-system community. The catalog row is
 * gone only after DELETE succeeds. Cancel does not call the server and the
 * row stays. HTTP 400, 403, and 409 keep the row on the detail panel.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-community-delete.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerCommunitiesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "communities",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** Unique community name; spaces are allowed by REST. */
function uniqueCommunityName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QA 6064 ${suffix || "comm"}`;
}

async function openCommunitiesCatalog(page) {
  await page.goto(developerCommunitiesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-comm-panel"]');
  const empty = page.locator('[data-testid="developer-comm-empty"]');
  const listError = page.locator('[data-testid="developer-comm-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer communities catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-comm-new"]')).toBeVisible();
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

function isCommunityDelete(request) {
  return (
    request.method() === "DELETE" &&
    /\/services\/communities\/bulk(?:\?|$)/.test(request.url())
  );
}

function communityRow(page, name) {
  return page.locator(`[data-comm-name="${name}"]`);
}

async function createCommunity(page, name) {
  await page.locator('[data-testid="developer-comm-new"]').click();
  await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible();
  await page.locator('[data-testid="developer-comm-name"]').fill(name);
  await page.locator('[data-testid="developer-comm-create"]').click();
  const notice = page.locator('[data-testid="developer-comm-detail-notice"]');
  const saveError = page.locator('[data-testid="developer-comm-detail-error"]');
  await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-comm-roles-save"]')).toBeVisible({
    timeout: 20_000,
  });
}

async function backToCatalog(page, name) {
  await page.locator('[data-testid="developer-comm-back"]').click();
  await expect(page.locator('[data-testid="developer-comm-panel"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(communityRow(page, name)).toBeVisible({ timeout: 20_000 });
}

test.describe("Developer delete one community (#6064)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("cancel does not delete and the row stays", async ({ page }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const deletes = [];
    page.on("request", (req) => {
      if (isCommunityDelete(req)) {
        deletes.push(req.url());
      }
    });

    await openCommunitiesCatalog(page);
    const name = uniqueCommunityName();
    await createCommunity(page, name);
    await backToCatalog(page, name);

    await communityRow(page, name).click();
    await expect(page.locator('[data-testid="developer-comm-delete"]')).toBeVisible();
    await page.locator('[data-testid="developer-comm-delete"]').click();
    await expect(
      page.locator('[data-testid="developer-catalog-confirm-dialog"]'),
    ).toBeVisible({ timeout: 10_000 });
    await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
    await expect(
      page.locator('[data-testid="developer-catalog-confirm-dialog"]'),
    ).toHaveCount(0);
    // Still on detail; nothing was deleted.
    await expect(page.locator('[data-testid="developer-comm-delete"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-comm-detail-error"]')).toHaveCount(0);
    expect(deletes, `unexpected community DELETE: ${deletes.join(" | ")}`).toEqual([]);

    await backToCatalog(page, name);
    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("catalog drops the row only after delete succeeds", async ({ page }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await openCommunitiesCatalog(page);
    const name = uniqueCommunityName();
    await createCommunity(page, name);
    await backToCatalog(page, name);

    await communityRow(page, name).click();
    await expect(page.locator('[data-testid="developer-comm-delete"]')).toBeVisible();
    const deleted = page.waitForResponse((response) =>
      isCommunityDelete(response.request()),
    );
    await page.locator('[data-testid="developer-comm-delete"]').click();
    await expect(
      page.locator('[data-testid="developer-catalog-confirm-dialog"]'),
    ).toBeVisible({ timeout: 10_000 });
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const response = await deleted;
    // Status only: success returns to the catalog, so the response body is
    // gone by the time navigation settles. Live bulk DELETE answers 204.
    expect([200, 204]).toContain(response.status());
    await expect(page.locator('[data-testid="developer-comm-panel"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(communityRow(page, name)).toHaveCount(0, { timeout: 30_000 });
    // Live Proof (#6064): pass-run capture of the catalog after the row
    // drops; committed copy under docs/ai-generated/proofs/.
    await page.screenshot({ path: "test-results/live-proof-6064-community-delete.png" });
    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("HTTP 400, 403, and 409 keep the row", async ({ page }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await openCommunitiesCatalog(page);
    const name = uniqueCommunityName();
    await createCommunity(page, name);
    await backToCatalog(page, name);

    await communityRow(page, name).click();
    await expect(page.locator('[data-testid="developer-comm-delete"]')).toBeVisible();

    let deleteRouted = false;
    for (const status of [400, 403, 409]) {
      if (deleteRouted) {
        await page.unroute("**/services/communities/bulk**");
      }
      await page.route("**/services/communities/bulk**", async (route) => {
        if (isCommunityDelete(route.request())) {
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message: `Rejected ${status}` }),
          });
          return;
        }
        await route.continue();
      });
      await page.locator('[data-testid="developer-comm-delete"]').click();
      await expect(
        page.locator('[data-testid="developer-catalog-confirm-dialog"]'),
      ).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      const err = page.locator('[data-testid="developer-comm-detail-error"]');
      await expect(err).toBeVisible({ timeout: 20_000 });
      await expect(err).toContainText(String(status));
      // Still on detail; the community was not deleted.
      await expect(page.locator('[data-testid="developer-comm-delete"]')).toBeVisible();
      deleteRouted = true;
    }

    await backToCatalog(page, name);
    assertConsoleClean(pageErrors, consoleErrors);
  });
});
