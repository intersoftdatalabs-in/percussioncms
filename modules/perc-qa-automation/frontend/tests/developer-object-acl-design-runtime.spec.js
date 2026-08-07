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
 * Developer Object ACL — design vs runtime permission model depth (#2283 / #2274 CD-19).
 *
 * Opens a content-type detail panel and asserts ObjectAclSection exposes
 * Workbench-parity Design access vs Runtime visibility column groups and
 * layered permission toggles (Read / Update / Delete / Modify ACL / Visible).
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npx playwright test tests/developer-object-acl-design-runtime.spec.js
 *
 * Entry: spa.jsp?entry=developer&section=content-types
 * Refs #2283, #2274, #2262, #1690.
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

test.describe("Developer Object ACL design vs runtime permissions (#2283)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("content type ACL shows Design access and Runtime visibility columns", async ({
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

    // Hint mentions design + runtime model
    await expect(aclSection).toContainText(/Design-time and runtime|Design access|Runtime/i);

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
      test.skip(
        true,
        "Object has no ACL — create-first path not required for #2283 design/runtime columns",
      );
      return;
    }

    await expect(aclTable).toBeVisible();
    await expect(aclTable).toHaveAttribute("data-acl-show-runtime", "true");
    await expect(aclTable).toHaveAttribute("data-acl-object-kind", "content-type");

    // Layer group headers
    await expect(
      page.locator('[data-testid="developer-ct-acl-layer-design"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-ct-acl-layer-design"]'),
    ).toContainText(/Design access/i);
    await expect(
      page.locator('[data-testid="developer-ct-acl-layer-runtime"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-ct-acl-layer-runtime"]'),
    ).toContainText(/Runtime visibility/i);

    // Permission column headers (Workbench labels)
    await expect(
      page.locator('[data-testid="developer-ct-acl-perm-header-READ"]'),
    ).toContainText(/Read/i);
    await expect(
      page.locator('[data-testid="developer-ct-acl-perm-header-UPDATE"]'),
    ).toContainText(/Update/i);
    await expect(
      page.locator('[data-testid="developer-ct-acl-perm-header-DELETE"]'),
    ).toContainText(/Delete/i);
    await expect(
      page.locator('[data-testid="developer-ct-acl-perm-header-OWNER"]'),
    ).toContainText(/Modify ACL/i);
    await expect(
      page.locator('[data-testid="developer-ct-acl-perm-header-RUNTIME_VISIBLE"]'),
    ).toContainText(/Visible/i);

    // At least one permission checkbox exists for design + runtime
    const designRead = page.locator(
      '[data-testid^="developer-ct-acl-perm-"][data-testid$="-READ"]',
    );
    const runtimeVis = page.locator(
      '[data-testid^="developer-ct-acl-perm-"][data-testid$="-RUNTIME_VISIBLE"]',
    );
    await expect(designRead.first()).toBeVisible();
    await expect(runtimeVis.first()).toBeVisible();

    // Toggle a design permission (non-destructive if we don't save)
    const firstRead = designRead.first();
    const wasChecked = await firstRead.isChecked();
    await firstRead.click();
    await expect(firstRead).toBeChecked({ checked: !wasChecked });
    // Restore so we leave UI clean if user has dirty state visible
    await firstRead.click();
    await expect(firstRead).toBeChecked({ checked: wasChecked });
  });
});
