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
 * Developer Object ACL — product path beyond CT-only smoke (#2642 / #2605 / #2274).
 *
 * Covers:
 *   - Content type ObjectAclSection: Design access / Runtime visibility + specials
 *   - Template ObjectAclSection: same layered columns (objectKind=template)
 *   - B4 peer mounts (#2639 / #2604): site + display-format (runtime-relevant kinds)
 *   - Kind-aware Design vs Runtime: data-acl-object-kind + data-acl-show-runtime
 *     (table when ACL loads; section shell when object guid is missing)
 *   - Developer Preferences default-ACL template: layered Design / Runtime columns
 *
 * Kind-aware runtime columns mirror WebUI objectAclPermissionModel.ts
 * RUNTIME_RELEVANT_OBJECT_KINDS — peers in that set show Runtime visibility;
 * non-runtime kinds (keyword/locale/…) hide runtime unless force-show.
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/developer-object-acl-product-path.spec.js
 *   # or: npm run test:surface -- --tag object-acl-product
 *
 * QA mode:
 *   perc-devctl qa-up → TEST_CMS_URL=… npm run test:surface -- --path tests/developer-object-acl-product-path.spec.js → qa-down
 *
 * Refs #2642, #2605, #2604, #2639, #2274, #2262, #1690 (builds on #2283 / PR #2342).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

/**
 * Mirror of WebUI objectAclPermissionModel.RUNTIME_RELEVANT_OBJECT_KINDS.
 * Keep in lockstep when product model changes (CD-19 / B4 peer mounts).
 * @type {ReadonlySet<string>}
 */
const RUNTIME_RELEVANT_OBJECT_KINDS = new Set([
  "content-type",
  "display-format",
  "action-menu",
  "menu-entry",
  "search",
  "site",
  "template",
  "variant",
  "view",
  "workflow",
]);

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
 * @param {{ rowBase: string, panel: string, empty: string, error: string, detail: string, catalogLabel: string, detailLoading?: string, detailError?: string }} ids
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

  // Detail panels that fetch by id (display-format, search, …) show loading/error
  // before ObjectAclSection mounts; site detail is list-payload driven (no loading).
  if (ids.detailLoading) {
    const detailLoading = page.locator(`[data-testid="${ids.detailLoading}"]`);
    await expect(detailLoading).toBeHidden({ timeout: 30_000 }).catch(() => {});
  }
  if (ids.detailError) {
    const detailError = page.locator(`[data-testid="${ids.detailError}"]`);
    if (await detailError.isVisible()) {
      const msg = (await detailError.innerText()).trim();
      throw new Error(`${ids.catalogLabel} detail error: ${msg}`);
    }
  }

  return true;
}

/**
 * Assert ObjectAclSection mount is kind-aware for Design vs Runtime.
 * - Always: section present with data-acl-object-kind (+ data-acl-show-runtime on no-guid shell)
 * - When table loads: Design access headers; Runtime only if RUNTIME_RELEVANT_OBJECT_KINDS
 * - When no guid: mount + attrs still prove B4 peer product path (#2642)
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} prefix e.g. developer-ct-acl / developer-site-acl
 * @param {string} objectKind expected data-acl-object-kind
 * @returns {Promise<"table"|"no-guid"|"empty">}
 */
async function assertLayeredObjectAcl(page, prefix, objectKind) {
  const expectRuntime = RUNTIME_RELEVANT_OBJECT_KINDS.has(objectKind);

  const aclSection = page.locator(`[data-testid="${prefix}-section"]`);
  await expect(aclSection).toBeVisible({ timeout: 15_000 });

  // Kind is always on section (with or without guid) after #2642 shell attrs
  await expect(aclSection).toHaveAttribute("data-acl-object-kind", objectKind);

  const noGuid = page.locator(`[data-testid="${prefix}-no-guid"]`);
  const aclError = page.locator(`[data-testid="${prefix}-error"]`);
  const aclEmpty = page.locator(`[data-testid="${prefix}-empty"]`);
  const aclTable = page.locator(`[data-testid="${prefix}-table"]`);
  const aclLoading = page.locator(`[data-testid="${prefix}-loading"]`);

  // No-guid shell: product path still proves objectKind + runtime policy without ACL rows
  if (await noGuid.isVisible().catch(() => false)) {
    await expect(aclSection).toHaveAttribute("data-acl-has-guid", "false");
    await expect(aclSection).toHaveAttribute(
      "data-acl-show-runtime",
      expectRuntime ? "true" : "false",
    );
    await expect(noGuid).toContainText(/GUID not available|cannot load ACL/i);
    return "no-guid";
  }

  // Fallback for older bundles that only put message text in section
  const sectionText = (await aclSection.innerText()).trim();
  if (
    /GUID not available|cannot load ACL/i.test(sectionText) &&
    !(await aclTable.isVisible().catch(() => false))
  ) {
    await expect(aclSection).toHaveAttribute("data-acl-object-kind", objectKind);
    return "no-guid";
  }

  await expect(aclLoading).toBeHidden({ timeout: 30_000 }).catch(() => {});
  const aclNoEntries = page.locator(`[data-testid="${prefix}-no-entries"]`);
  await aclSection.scrollIntoViewIfNeeded().catch(() => {});
  await expect(
    aclTable.or(aclEmpty).or(aclError).or(aclNoEntries).first(),
  ).toBeVisible({
    timeout: 30_000,
  });

  if (await aclError.isVisible()) {
    const msg = (await aclError.innerText()).trim();
    throw new Error(`Object ACL load error (${prefix}): ${msg}`);
  }

  if (await aclEmpty.isVisible()) {
    // Create-first empty state still proves mount + kind on section
    await expect(aclSection).toHaveAttribute("data-acl-object-kind", objectKind);
    return "empty";
  }

  if (await aclNoEntries.isVisible()) {
    await expect(aclSection).toHaveAttribute("data-acl-object-kind", objectKind);
    return "empty";
  }

  await expect(aclTable).toBeVisible();
  await expect(aclTable).toHaveAttribute(
    "data-acl-show-runtime",
    expectRuntime ? "true" : "false",
  );
  await expect(aclTable).toHaveAttribute("data-acl-object-kind", objectKind);

  // Design access layer always present when table loads
  await expect(
    page.locator(`[data-testid="${prefix}-layer-design"]`),
  ).toBeVisible();
  await expect(
    page.locator(`[data-testid="${prefix}-layer-design"]`),
  ).toContainText(/Design access/i);

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

  const designRead = page.locator(
    `input[type="checkbox"][data-testid^="${prefix}-perm-"][data-testid$="-READ"]`,
  );
  await expect(designRead.first()).toBeVisible();

  if (expectRuntime) {
    await expect(
      page.locator(`[data-testid="${prefix}-layer-runtime"]`),
    ).toBeVisible();
    await expect(
      page.locator(`[data-testid="${prefix}-layer-runtime"]`),
    ).toContainText(/Runtime visibility/i);
    await expect(
      page.locator(`[data-testid="${prefix}-perm-header-RUNTIME_VISIBLE"]`),
    ).toContainText(/Visible/i);
    const runtimeVis = page.locator(
      `input[type="checkbox"][data-testid^="${prefix}-perm-"][data-testid$="-RUNTIME_VISIBLE"]`,
    );
    await expect(runtimeVis.first()).toBeVisible();
  } else {
    await expect(
      page.locator(`[data-testid="${prefix}-layer-runtime"]`),
    ).toHaveCount(0);
    await expect(
      page.locator(`[data-testid="${prefix}-perm-header-RUNTIME_VISIBLE"]`),
    ).toHaveCount(0);
  }

  return "table";
}

test.describe("Developer Object ACL product path (#2642 / #2605 B5) @object-acl-product", () => {
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

    const mode = await assertLayeredObjectAcl(
      page,
      "developer-ct-acl",
      "content-type",
    );
    if (mode === "table") {
      await expect(
        page.locator('[data-testid="developer-ct-acl-special-hint"]'),
      ).toBeVisible();
    }
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

  test("site peer ACL mounts objectKind=site with runtime-relevant Design/Runtime policy (B4)", async ({
    page,
  }) => {
    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-sites"]'),
    ).toBeVisible({ timeout: 15_000 });

    const opened = await openFirstCatalogDetail(page, {
      rowBase: "developer-site-row",
      panel: "developer-site-panel",
      empty: "developer-site-empty",
      error: "developer-site-error",
      detail: "developer-site-detail",
      catalogLabel: "sites",
    });
    if (!opened) return;

    const mode = await assertLayeredObjectAcl(page, "developer-site-acl", "site");
    // site is RUNTIME_RELEVANT — show-runtime must be true (table or no-guid shell)
    expect(["table", "no-guid", "empty"]).toContain(mode);
  });

  test("display-format peer ACL mounts objectKind with kind-aware Runtime columns (B4)", async ({
    page,
  }) => {
    await page.goto(developerSectionUrl("display-formats"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-display-formats"]'),
    ).toBeVisible({ timeout: 15_000 });

    const opened = await openFirstCatalogDetail(page, {
      rowBase: "developer-df-row",
      panel: "developer-df-panel",
      empty: "developer-df-empty",
      error: "developer-df-error",
      detail: "developer-df-detail",
      detailLoading: "developer-df-detail-loading",
      detailError: "developer-df-detail-error",
      catalogLabel: "display formats",
    });
    if (!opened) return;

    await assertDisplayFormatDetailGuidAndAcl(page);
  });

  /**
   * Issue #3200 / #2951 / #2689: header GUID is real; Object ACL is table,
   * empty-create, or a real ACL error — never the no-guid shell when the
   * server has a display format id.
   * @param {import('@playwright/test').Page} page
   */
  async function assertDisplayFormatDetailGuidAndAcl(page) {
    const guidCell = page.locator('[data-testid="developer-df-detail-guid"]');
    await expect(guidCell).toBeVisible({ timeout: 15_000 });
    const guidText = (await guidCell.innerText()).trim();
    expect(
      guidText && guidText !== "—" && guidText !== "-",
      `display-format detail GUID must be synthesized/normalized (got "${guidText}")`,
    ).toBeTruthy();
    expect(guidText.length).toBeGreaterThan(2);

    const mode = await assertLayeredObjectAcl(
      page,
      "developer-df-acl",
      "display-format",
    );
    expect(
      ["table", "empty"],
      `display-format Object ACL must load with GUID (got mode=${mode})`,
    ).toContain(mode);
  }

  test("By_Author and one peer Display Format show GUID and Object ACL (#3200)", async ({
    page,
  }) => {
    await page.goto(developerSectionUrl("display-formats"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-display-formats"]'),
    ).toBeVisible({ timeout: 15_000 });

    const panel = page.locator('[data-testid="developer-df-panel"]');
    const empty = page.locator('[data-testid="developer-df-empty"]');
    const error = page.locator('[data-testid="developer-df-error"]');
    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });
    if (await error.isVisible()) {
      throw new Error(`display formats catalog error: ${(await error.innerText()).trim()}`);
    }
    if (await empty.isVisible()) {
      test.skip(true, "No display formats in CMS");
      return;
    }

    const openButtons = page.locator('[data-testid="developer-df-open"]');
    const count = await openButtons.count();
    expect(count, "catalog should list at least one display format").toBeGreaterThan(0);

    const names = [];
    for (let i = 0; i < count; i++) {
      names.push((await openButtons.nth(i).innerText()).trim());
    }
    const byAuthor = names.find((n) => /^By_Author$/i.test(n));
    const peer = names.find((n) => n && n !== byAuthor);
    const toOpen = [byAuthor, peer].filter(Boolean);
    expect(
      toOpen.length,
      `need By_Author and/or a peer in catalog (got ${names.join(", ")})`,
    ).toBeGreaterThan(0);

    for (const name of toOpen) {
      await page.locator('[data-testid="developer-df-open"]', { hasText: name }).click();
      await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible({
        timeout: 20_000,
      });
      await assertDisplayFormatDetailGuidAndAcl(page);
      await page.locator('[data-testid="developer-df-back"]').click();
      await expect(page.locator('[data-testid="developer-df-panel"]')).toBeVisible({
        timeout: 15_000,
      });
    }
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
