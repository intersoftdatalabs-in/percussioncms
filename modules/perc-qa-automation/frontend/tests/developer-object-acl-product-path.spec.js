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
 * Developer Object ACL — product path beyond CT-only smoke (#2605 / #2274 B5).
 *
 * Covers:
 *   - Content type ObjectAclSection: Design access / Runtime visibility + specials
 *   - Template ObjectAclSection: same layered columns (objectKind=template)
 *   - Developer Preferences default-ACL template: layered Design / Runtime columns
 *
 * Peer mounts beyond CT/Template are B4 (#2604); residual Playwright if B4 lands later.
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/developer-object-acl-product-path.spec.js
 *   # or: npm run test:surface -- --tag object-acl-product
 *
 * QA mode:
 *   perc-devctl qa-up → TEST_CMS_URL=… npm run test:surface -- --path tests/developer-object-acl-product-path.spec.js → qa-down
 *
 * Refs #2605, #2274, #2262, #1690 (builds on #2283 / PR #2342).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

function developerSectionUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * Open first catalog row detail and return when detail panel is visible.
 * @param {import('@playwright/test').Page} page
 * @param {{ rowBase: string, panel: string, empty: string, error: string, detail: string, catalogLabel: string }} ids
 */
async function openFirstCatalogDetail(page, ids) {
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });

  const error = page.locator(`[data-testid="${ids.error}"]`);
  const panel = page.locator(`[data-testid="${ids.panel}"]`);
  const empty = page.locator(`[data-testid="${ids.empty}"]`);

  await expect(panel.or(empty).or(error).first()).toBeVisible({
    timeout: 30_000,
  });

  if (await error.isVisible()) {
    const msg = (await error.innerText()).trim();
    throw new Error(`${ids.catalogLabel} catalog error: ${msg}`);
  }

  if (await empty.isVisible()) {
    test.skip(true, `No ${ids.catalogLabel} in CMS — cannot open Object ACL`);
    return false;
  }

  const firstRow = page.locator(catalogRowSelector(ids.rowBase, 0));
  await expect(firstRow).toBeVisible({ timeout: 15_000 });
  const openBtn = firstRow.locator("button");
  await expect(
    openBtn,
    `first ${ids.catalogLabel} row should expose Open when selectionKey is set`,
  ).toBeVisible({ timeout: 5_000 });
  await openBtn.click();

  await expect(page.locator(`[data-testid="${ids.detail}"]`)).toBeVisible({
    timeout: 20_000,
  });
  return true;
}

/**
 * Assert layered design/runtime ACL columns on a mounted ObjectAclSection.
 * @param {import('@playwright/test').Page} page
 * @param {string} prefix e.g. developer-ct-acl / developer-tpl-acl
 * @param {string} objectKind expected data-acl-object-kind
 */
async function assertLayeredObjectAcl(page, prefix, objectKind) {
  const aclSection = page.locator(`[data-testid="${prefix}-section"]`);
  await expect(aclSection).toBeVisible({ timeout: 15_000 });
  await expect(aclSection).toContainText(
    /Design-time and runtime|Design access|Runtime/i,
  );

  const aclError = page.locator(`[data-testid="${prefix}-error"]`);
  const aclEmpty = page.locator(`[data-testid="${prefix}-empty"]`);
  const aclTable = page.locator(`[data-testid="${prefix}-table"]`);
  const aclLoading = page.locator(`[data-testid="${prefix}-loading"]`);

  await expect(aclLoading).toBeHidden({ timeout: 30_000 }).catch(() => {});
  await expect(aclTable.or(aclEmpty).or(aclError).first()).toBeVisible({
    timeout: 30_000,
  });

  if (await aclError.isVisible()) {
    const msg = (await aclError.innerText()).trim();
    throw new Error(`Object ACL load error (${prefix}): ${msg}`);
  }

  if (await aclEmpty.isVisible()) {
    test.skip(
      true,
      `Object has no ACL on ${prefix} — create-first not required for B5 column groups`,
    );
    return false;
  }

  await expect(aclTable).toBeVisible();
  await expect(aclTable).toHaveAttribute("data-acl-show-runtime", "true");
  await expect(aclTable).toHaveAttribute("data-acl-object-kind", objectKind);

  await expect(
    page.locator(`[data-testid="${prefix}-layer-design"]`),
  ).toBeVisible();
  await expect(
    page.locator(`[data-testid="${prefix}-layer-design"]`),
  ).toContainText(/Design access/i);
  await expect(
    page.locator(`[data-testid="${prefix}-layer-runtime"]`),
  ).toBeVisible();
  await expect(
    page.locator(`[data-testid="${prefix}-layer-runtime"]`),
  ).toContainText(/Runtime visibility/i);

  await expect(
    page.locator(`[data-testid="${prefix}-perm-header-READ"]`),
  ).toContainText(/Read/i);
  await expect(
    page.locator(`[data-testid="${prefix}-perm-header-UPDATE"]`),
  ).toContainText(/Update/i);
  await expect(
    page.locator(`[data-testid="${prefix}-perm-header-DELETE"]`),
  ).toContainText(/Delete/i);
  await expect(
    page.locator(`[data-testid="${prefix}-perm-header-OWNER"]`),
  ).toContainText(/Modify ACL/i);
  await expect(
    page.locator(`[data-testid="${prefix}-perm-header-RUNTIME_VISIBLE"]`),
  ).toContainText(/Visible/i);

  const designRead = page.locator(
    `input[type="checkbox"][data-testid^="${prefix}-perm-"][data-testid$="-READ"]`,
  );
  const runtimeVis = page.locator(
    `input[type="checkbox"][data-testid^="${prefix}-perm-"][data-testid$="-RUNTIME_VISIBLE"]`,
  );
  await expect(designRead.first()).toBeVisible();
  await expect(runtimeVis.first()).toBeVisible();
  return true;
}

test.describe("Developer Object ACL product path (#2605 B5) @object-acl-product", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("content type ACL shows Design/Runtime columns and protected specials", async ({
    page,
  }) => {
    await page.goto(developerSectionUrl("content-types"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-content-types"]'),
    ).toBeVisible({ timeout: 15_000 });

    const opened = await openFirstCatalogDetail(page, {
      rowBase: "developer-ct-row",
      panel: "developer-ct-panel",
      empty: "developer-ct-empty",
      error: "developer-ct-error",
      detail: "developer-ct-detail",
      catalogLabel: "content types",
    });
    if (!opened) return;

    const ok = await assertLayeredObjectAcl(
      page,
      "developer-ct-acl",
      "content-type",
    );
    if (!ok) return;

    // Protected specials still present on product path (B1 / #2281)
    await expect(
      page.locator('[data-testid="developer-ct-acl-special-hint"]'),
    ).toBeVisible();
  });

  test("template ACL shows Design access and Runtime visibility columns", async ({
    page,
  }) => {
    await page.goto(developerSectionUrl("templates"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-templates"]'),
    ).toBeVisible({ timeout: 15_000 });

    const opened = await openFirstCatalogDetail(page, {
      rowBase: "developer-tpl-row",
      panel: "developer-tpl-panel",
      empty: "developer-tpl-empty",
      error: "developer-tpl-error",
      detail: "developer-tpl-detail",
      catalogLabel: "templates",
    });
    if (!opened) return;

    await assertLayeredObjectAcl(page, "developer-tpl-acl", "template");
  });

  test("preferences default ACL template shows Design/Runtime column groups", async ({
    page,
  }) => {
    await page.goto(developerSectionUrl("preferences"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-preferences"]'),
    ).toBeVisible({ timeout: 15_000 });

    const prefsPanel = page.locator('[data-testid="developer-prefs-panel"]');
    await expect(prefsPanel).toBeVisible({ timeout: 20_000 });

    const loading = page.locator('[data-testid="developer-prefs-acl-loading"]');
    await expect(loading).toBeHidden({ timeout: 30_000 }).catch(() => {});

    const table = page.locator('[data-testid="developer-prefs-acl-table"]');
    const error = page.locator('[data-testid="developer-prefs-acl-error"]');
    // Table should still render system default even if preference load fails
    await expect(table.or(error).first()).toBeVisible({ timeout: 30_000 });
    await expect(table).toBeVisible({ timeout: 15_000 });

    await expect(table).toHaveAttribute("data-acl-show-runtime", "true");
    await expect(table).toHaveAttribute("data-acl-layered", "true");

    await expect(
      page.locator('[data-testid="developer-prefs-acl-layer-design"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-prefs-acl-layer-design"]'),
    ).toContainText(/Design access/i);
    await expect(
      page.locator('[data-testid="developer-prefs-acl-layer-runtime"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-prefs-acl-layer-runtime"]'),
    ).toContainText(/Runtime visibility/i);

    await expect(
      page.locator('[data-testid="developer-prefs-acl-perm-header-READ"]'),
    ).toContainText(/Read/i);
    await expect(
      page.locator(
        '[data-testid="developer-prefs-acl-perm-header-RUNTIME_VISIBLE"]',
      ),
    ).toContainText(/Visible/i);

    const runtimeBoxes = page.locator(
      'input[type="checkbox"][data-testid^="developer-prefs-acl-perm-"][data-testid$="-RUNTIME_VISIBLE"]',
    );
    await expect(runtimeBoxes.first()).toBeVisible();
  });
});
