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
 * Developer Communities create / delete chrome (#4077 SE-01 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-community-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

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
  return `QA 4077 ${suffix || "comm"}`;
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

test.describe("Developer community editor (#4077 / SE-01)", () => {
  test("Admin can create a uniquely named community and delete it", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openCommunitiesCatalog(page);

    const name = uniqueCommunityName();

    await page.locator('[data-testid="developer-comm-new"]').click();
    await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible();
    const createBtn = page.locator('[data-testid="developer-comm-create"]');
    await expect(createBtn).toBeDisabled();

    await page.locator('[data-testid="developer-comm-name"]').fill("   ");
    await expect(createBtn).toBeDisabled();
    await page.locator('[data-testid="developer-comm-name"]').fill(name);
    await expect(createBtn).toBeEnabled();
    await createBtn.click();

    const notice = page.locator('[data-testid="developer-comm-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-comm-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-comm-roles-save"]')).toBeVisible({
      timeout: 20_000,
    });

    await page.locator('[data-testid="developer-comm-back"]').click();
    await expect(page.locator('[data-testid="developer-comm-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-comm-name="${name}"]`)).toBeVisible({
      timeout: 20_000,
    });

    await page.locator(`[data-comm-name="${name}"]`).click();
    await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-comm-roles-save"]')).toBeVisible();
    await page.locator('[data-testid="developer-comm-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-comm-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-comm-name="${name}"]`)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openCommunitiesCatalog(page);

    const name = uniqueCommunityName();
    await page.locator('[data-testid="developer-comm-new"]').click();
    await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-comm-name"]').fill(name);
    await page.locator('[data-testid="developer-comm-create"]').click();
    const firstNotice = page.locator('[data-testid="developer-comm-detail-notice"]');
    const firstErr = page.locator('[data-testid="developer-comm-detail-error"]');
    await expect(firstNotice.or(firstErr).first()).toBeVisible({ timeout: 20_000 });
    if (await firstErr.isVisible()) {
      throw new Error(`Create failed: ${(await firstErr.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-comm-back"]').click();
    await expect(page.locator('[data-testid="developer-comm-new"]')).toBeVisible();
    await page.locator('[data-testid="developer-comm-new"]').click();
    await page.locator('[data-testid="developer-comm-name"]').fill(name);
    await page.locator('[data-testid="developer-comm-create"]').click();

    const err = page.locator('[data-testid="developer-comm-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    await page.locator('[data-testid="developer-comm-back"]').click();
    const openCreated = page.locator(`[data-comm-name="${name}"]`);
    if (await openCreated.count()) {
      await openCreated.click();
      await expect(page.locator('[data-testid="developer-comm-delete"]')).toBeVisible();
      await page.locator('[data-testid="developer-comm-delete"]').click();
      await confirmDeveloperCatalogDelete(page);
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
