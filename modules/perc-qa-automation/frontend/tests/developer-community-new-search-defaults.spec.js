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
 * Developer communities — CX new-search defaults (UI-09 / #4220 / parent #1690).
 *
 * Opens community detail and asserts the new-search defaults picker, save
 * chrome, and empty-or-table state. Does not create searches.
 *
 * Entry: spa.jsp?entry=developer&section=communities
 *
 *     npm run test:surface -- --path tests/developer-community-new-search-defaults.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

function developerCommunitiesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "communities",
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

test.describe("Developer community new-search defaults (UI-09 / #4220)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("community detail shows new-search defaults picker and save", async ({
    page,
  }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await page.goto(developerCommunitiesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-communities"]'),
    ).toBeVisible({ timeout: 15_000 });

    const error = page.locator('[data-testid="developer-comm-error"]');
    const panel = page.locator('[data-testid="developer-comm-panel"]');
    const empty = page.locator('[data-testid="developer-comm-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Communities catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No communities in CMS — cannot open new-search defaults");
      return;
    }

    const firstRow = page.locator(catalogRowSelector("developer-comm-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator("button");
    await expect(openBtn).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(page.locator('[data-testid="developer-comm-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-comm-nsd"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-comm-nsd-save"]')).toBeVisible();

    const nsdTable = page.locator('[data-testid="developer-comm-nsd-table"]');
    const nsdEmpty = page.locator('[data-testid="developer-comm-nsd-empty"]');
    const nsdErr = page.locator('[data-testid="developer-comm-nsd-error"]');
    const nsdLoading = page.locator('[data-testid="developer-comm-nsd-loading"]');

    await expect(nsdLoading).toBeHidden({ timeout: 30_000 }).catch(() => {});
    await expect(nsdTable.or(nsdEmpty).or(nsdErr).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await nsdErr.isVisible()) {
      const msg = (await nsdErr.innerText()).trim();
      throw new Error(`Community new-search defaults error: ${msg}`);
    }

    if (await nsdTable.isVisible()) {
      const firstCheck = nsdTable.locator('input[type="checkbox"]').first();
      await expect(firstCheck).toBeVisible();
      await firstCheck.click();
      await expect(page.locator('[data-testid="developer-comm-nsd-dirty"]')).toBeVisible();
      await page.locator('[data-testid="developer-comm-nsd-save"]').click();
      const notice = page.locator('[data-testid="developer-comm-detail-notice"]');
      const saveErr = page.locator('[data-testid="developer-comm-detail-error"]');
      await expect(notice.or(saveErr).first()).toBeVisible({ timeout: 20_000 });
      if (await saveErr.isVisible()) {
        const msg = (await saveErr.innerText()).trim();
        throw new Error(`Save new-search defaults failed: ${msg}`);
      }
      await expect(page.locator('[data-testid="developer-comm-nsd-dirty"]')).toHaveCount(0);
      // Restore so the H2 cell is not left dirty for later tests.
      await firstCheck.click();
      const stillDirty = await page
        .locator('[data-testid="developer-comm-nsd-dirty"]')
        .isVisible()
        .catch(() => false);
      if (stillDirty) {
        await page.locator('[data-testid="developer-comm-nsd-save"]').click();
        await expect(notice.or(saveErr).first()).toBeVisible({ timeout: 20_000 });
      }
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
