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
 * Developer Object ACL — Default / AnyCommunity special principal UX (#2281 / #2274).
 *
 * Opens a content-type detail panel and asserts ObjectAclSection exposes
 * Workbench-parity special principal handling: protected labels, non-removable
 * Default / AnyCommunity rows, and add actions when either is missing.
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npx playwright test tests/developer-object-acl-special-entries.spec.js
 *
 * Entry: spa.jsp?entry=developer&section=content-types
 * Refs #2281, #2274, #2262, #1690.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer Object ACL Default/AnyCommunity specials (#2281)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("content type ACL shows protected Default/AnyCommunity UX", async ({
    page,
  }) => {
    await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-content-types"]'),
    ).toBeVisible({ timeout: 15_000 });

    const error = page.locator('[data-testid="developer-ct-error"]');
    const panel = page.locator('[data-testid="developer-ct-panel"]');
    const empty = page.locator('[data-testid="developer-ct-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Content types catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No content types in CMS — cannot open Object ACL");
      return;
    }

    const firstRow = page.locator(catalogRowSelector("developer-ct-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator("button");
    await expect(
      openBtn,
      "first content-type row should expose Open when selectionKey is set",
    ).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(page.locator('[data-testid="developer-ct-detail"]')).toBeVisible({
      timeout: 20_000,
    });

    const aclSection = page.locator('[data-testid="developer-ct-acl-section"]');
    await expect(aclSection).toBeVisible({ timeout: 15_000 });
    await expect(
      page.locator('[data-testid="developer-ct-acl-special-hint"]'),
    ).toBeVisible();

    const aclError = page.locator('[data-testid="developer-ct-acl-error"]');
    const aclEmpty = page.locator('[data-testid="developer-ct-acl-empty"]');
    const aclTable = page.locator('[data-testid="developer-ct-acl-table"]');
    const aclLoading = page.locator('[data-testid="developer-ct-acl-loading"]');

    await expect(aclLoading).toBeHidden({ timeout: 30_000 }).catch(() => {});
    await expect(aclTable.or(aclEmpty).or(aclError).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await aclError.isVisible()) {
      const msg = (await aclError.innerText()).trim();
      throw new Error(`Object ACL load error: ${msg}`);
    }

    if (await aclEmpty.isVisible()) {
      // Create path is out of scope for specials UX; skip when object has no ACL.
      test.skip(true, "Object has no ACL — create-first path not required for #2281");
      return;
    }

    // Special hint always present when ACL is loaded.
    await expect(
      page.locator('[data-testid="developer-ct-acl-special-hint"]'),
    ).toContainText(/Default|AnyCommunity/i);

    const defaultRows = page.locator(
      '[data-testid^="developer-ct-acl-row-"][data-special-acl="default"]',
    );
    const anyRows = page.locator(
      '[data-testid^="developer-ct-acl-row-"][data-special-acl="any-community"]',
    );
    const addDefault = page.locator('[data-testid="developer-ct-acl-add-default"]');
    const addAny = page.locator(
      '[data-testid="developer-ct-acl-add-any-community"]',
    );

    const defaultCount = await defaultRows.count();
    const anyCount = await anyRows.count();

    if (defaultCount === 0) {
      await expect(addDefault).toBeVisible();
      await addDefault.click();
      await expect(
        page.locator(
          '[data-testid^="developer-ct-acl-row-"][data-special-acl="default"]',
        ),
      ).toHaveCount(1, { timeout: 5_000 });
    } else {
      await expect(defaultRows.first()).toBeVisible();
      await expect(
        defaultRows.first().locator('[data-testid*="developer-ct-acl-label-"]'),
      ).toContainText(/Default/i);
      // Protected: no remove control on Default row
      await expect(
        defaultRows.first().locator('[data-testid*="developer-ct-acl-remove-"]'),
      ).toHaveCount(0);
      await expect(
        defaultRows.first().locator('[data-testid*="developer-ct-acl-protected-"]'),
      ).toBeVisible();
    }

    if (anyCount === 0) {
      await expect(addAny).toBeVisible();
      await addAny.click();
      await expect(
        page.locator(
          '[data-testid^="developer-ct-acl-row-"][data-special-acl="any-community"]',
        ),
      ).toHaveCount(1, { timeout: 5_000 });
    } else {
      await expect(anyRows.first()).toBeVisible();
      await expect(
        anyRows.first().locator('[data-testid*="developer-ct-acl-label-"]'),
      ).toContainText(/Any community/i);
      await expect(
        anyRows.first().locator('[data-testid*="developer-ct-acl-remove-"]'),
      ).toHaveCount(0);
      await expect(
        anyRows.first().locator('[data-testid*="developer-ct-acl-protected-"]'),
      ).toBeVisible();
    }

    // After ensuring both specials (existing or just-added), special add actions go away.
    const finalDefault = page.locator(
      '[data-testid^="developer-ct-acl-row-"][data-special-acl="default"]',
    );
    const finalAny = page.locator(
      '[data-testid^="developer-ct-acl-row-"][data-special-acl="any-community"]',
    );
    await expect(finalDefault).toHaveCount(1);
    await expect(finalAny).toHaveCount(1);
    await expect(addDefault).toHaveCount(0);
    await expect(addAny).toHaveCount(0);

    // Permission toggles remain available on special rows (no save required for UX gate).
    const defaultRow = finalDefault.first();
    const readToggle = defaultRow.locator(
      '[data-testid*="developer-ct-acl-perm-"][data-testid$="-READ"]',
    );
    await expect(readToggle).toBeVisible();
    await expect(readToggle).toBeEnabled();
  });
});
