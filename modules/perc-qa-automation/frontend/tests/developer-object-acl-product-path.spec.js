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
 *   - Preferences persist: Runtime Visible survives Save + reload (#3204 / #2643)
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
 * Refs #3319, #3204, #2643, #2642, #2605, #2604, #2639, #2274, #2262, #1690 (builds on #2283 / PR #2342).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
  catalogOpenByExactName,
  catalogRowByExactName,
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
 * @returns {Promise<"table"|"no-guid"|"empty"|"no-entries">}
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
  const aclNoEntries = page.locator(`[data-testid="${prefix}-no-entries"]`);
  const aclTable = page.locator(`[data-testid="${prefix}-table"]`);
  const aclLoading = page.locator(`[data-testid="${prefix}-loading"]`);

  // No-guid shell: product path still proves objectKind + runtime policy without ACL rows
  if (await noGuid.isVisible().catch(() => false)) {
    await expect(aclSection).toHaveAttribute("data-acl-has-guid", "false");
    await expect(aclSection).toHaveAttribute(
      "data-acl-show-runtime",
      expectRuntime ? "true" : "false",
    );
    await expect(noGuid).toContainText(
      /GUID not available|cannot load ACL|has no object GUID/i,
    );
    return "no-guid";
  }

  // Fallback for older bundles that only put message text in section
  const sectionText = (await aclSection.innerText()).trim();
  if (
    /GUID not available|cannot load ACL|has no object GUID/i.test(sectionText) &&
    !(await aclTable.isVisible().catch(() => false))
  ) {
    await expect(aclSection).toHaveAttribute("data-acl-object-kind", objectKind);
    return "no-guid";
  }

  await expect(aclLoading).toBeHidden({ timeout: 30_000 }).catch(() => {});
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
    // ACL document exists but has no principals yet — still readable/editable
    // (add specials / add entry). Live By_Author often serializes without
    // aclEntries (#3203 / #2672). #3377: Design + Runtime headers stay visible
    // before any draft row exists (do not require forceShow from existing bits).
    await expect(aclSection).toHaveAttribute("data-acl-object-kind", objectKind);
    await expect(aclSection).toHaveAttribute("data-acl-has-guid", "true");
    await expect(aclNoEntries).toContainText(/No ACL entries yet/i);
    await expect(aclTable).toBeVisible();
    await expect(aclTable).toHaveAttribute(
      "data-acl-show-runtime",
      expectRuntime ? "true" : "false",
    );
    await expect(
      page.locator(`[data-testid="${prefix}-layer-design"]`),
    ).toBeVisible();
    if (expectRuntime) {
      await expect(
        page.locator(`[data-testid="${prefix}-layer-runtime"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${prefix}-perm-header-RUNTIME_VISIBLE"]`),
      ).toContainText(/Visible/i);
    }
    return "no-entries";
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
      detailLoading: "developer-ct-detail-loading",
      detailError: "developer-ct-detail-error",
      catalogLabel: "content types",
    });
    if (!opened) return;

    const guidCell = page.locator('[data-testid="developer-ct-detail-guid"]');
    await expect(guidCell).toBeVisible({ timeout: 15_000 });
    const guidText = (await guidCell.innerText()).trim();
    const mode = await assertLayeredObjectAcl(
      page,
      "developer-ct-acl",
      "content-type",
    );
    // Detail / catalog GUID resolver (#3319): when a GUID is present, ACL must load.
    if (guidText && guidText !== "—" && guidText !== "-") {
      expect(
        ["table", "empty", "no-entries"],
        `content-type Object ACL must load with GUID "${guidText}" (got mode=${mode})`,
      ).toContain(mode);
    } else {
      expect(["table", "no-guid", "empty", "no-entries"]).toContain(mode);
    }
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
      detailLoading: "developer-tpl-detail-loading",
      detailError: "developer-tpl-detail-error",
      catalogLabel: "templates",
    });
    if (!opened) return;

    const guidCell = page.locator('[data-testid="developer-tpl-detail-guid"]');
    await expect(guidCell).toBeVisible({ timeout: 15_000 });
    const guidText = (await guidCell.innerText()).trim();
    const mode = await assertLayeredObjectAcl(page, "developer-tpl-acl", "template");
    // templateId / Guid / catalog fallback (#3319): when a GUID is present, ACL must load.
    if (guidText && guidText !== "—" && guidText !== "-") {
      expect(
        ["table", "empty", "no-entries"],
        `template Object ACL must load with GUID "${guidText}" (got mode=${mode})`,
      ).toContain(mode);
    } else {
      expect(["table", "no-guid", "empty", "no-entries"]).toContain(mode);
    }
  });

  test("opening Content Type then Template does not crash Developer shell (#3377)", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(developerSectionUrl("content-types"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-content-types"]'),
    ).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="perc-developer-shell"]')).toBeVisible();

    const ctOpened = await openFirstCatalogDetail(page, {
      rowBase: "developer-ct-row",
      panel: "developer-ct-panel",
      empty: "developer-ct-empty",
      error: "developer-ct-error",
      detail: "developer-ct-detail",
      detailLoading: "developer-ct-detail-loading",
      detailError: "developer-ct-detail-error",
      catalogLabel: "content types",
    });
    if (!ctOpened) return;

    await expect(page.locator('[data-testid="perc-developer-shell"]')).toBeVisible();
    await expect(page.locator('[data-testid="route-error"]')).toHaveCount(0);

    await page.locator('[data-testid="tab-developer-templates"]').click();
    await expect(
      page.locator('[data-testid="panel-developer-templates"]'),
    ).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="perc-developer-shell"]')).toBeVisible();
    await expect(page.locator('[data-testid="route-error"]')).toHaveCount(0);
    await expect(page.getByText(/Unable to load Developer/i)).toHaveCount(0);

    const tplError = page.locator('[data-testid="developer-tpl-error"]');
    const tplPanel = page.locator('[data-testid="developer-tpl-panel"]');
    const tplEmpty = page.locator('[data-testid="developer-tpl-empty"]');
    const tplSectionError = page.locator('[data-testid="developer-section-error"]');
    await expect(
      tplPanel.or(tplEmpty).or(tplError).or(tplSectionError).first(),
    ).toBeVisible({ timeout: 30_000 });

    if (await tplError.isVisible()) {
      const msg = (await tplError.innerText()).trim();
      throw new Error(`templates catalog error after CT: ${msg}`);
    }
    if (await tplEmpty.isVisible()) {
      // Catalog empty is valid; shell must still be intact.
      await expect(page.locator('[data-testid="perc-developer-shell"]')).toBeVisible();
      expect(pageErrors, `pageerror after CT→Templates: ${pageErrors.join("; ")}`).toEqual(
        [],
      );
      return;
    }
    if (await tplSectionError.isVisible()) {
      // Isolated in-panel error is acceptable; the Developer shell must remain.
      await expect(page.locator('[data-testid="perc-developer-shell"]')).toBeVisible();
      await expect(page.locator('[data-testid="route-error"]')).toHaveCount(0);
      return;
    }

    const firstRow = page.locator(catalogRowSelector("developer-tpl-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator("button");
    await expect(openBtn).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(page.locator('[data-testid="developer-tpl-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    const detailLoading = page.locator(
      '[data-testid="developer-tpl-detail-loading"]',
    );
    await expect(detailLoading).toBeHidden({ timeout: 30_000 }).catch(() => {});

    await expect(page.locator('[data-testid="perc-developer-shell"]')).toBeVisible();
    await expect(page.locator('[data-testid="route-error"]')).toHaveCount(0);
    await expect(page.getByText(/Unable to load Developer/i)).toHaveCount(0);
    expect(pageErrors, `pageerror after CT→Template: ${pageErrors.join("; ")}`).toEqual(
      [],
    );

    const tplAclTable = page.locator('[data-testid="developer-tpl-acl-table"]');
    const tplAclNoEntries = page.locator(
      '[data-testid="developer-tpl-acl-no-entries"]',
    );
    if (
      (await tplAclNoEntries.isVisible().catch(() => false)) ||
      (await tplAclTable.isVisible().catch(() => false))
    ) {
      await expect(tplAclTable).toBeVisible();
      await expect(tplAclTable).toHaveAttribute("data-acl-show-runtime", "true");
      await expect(
        page.locator('[data-testid="developer-tpl-acl-perm-header-RUNTIME_VISIBLE"]'),
      ).toContainText(/Visible/i);
    }
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

    const guidCell = page.locator('[data-testid="developer-site-detail-guid"]');
    await expect(guidCell).toBeVisible({ timeout: 15_000 });
    const guidText = (await guidCell.innerText()).trim();
    const mode = await assertLayeredObjectAcl(page, "developer-site-acl", "site");
    // Site list GUID is normalized (stringValue or host-type-uuid). When a GUID
    // is present, Object ACL must load — no-guid is only valid if the catalog
    // row truly omitted guid parts (#3203 / #2672).
    if (guidText && guidText !== "—" && guidText !== "-") {
      expect(
        ["table", "empty", "no-entries"],
        `site Object ACL must load with GUID "${guidText}" (got mode=${mode})`,
      ).toContain(mode);
    } else {
      expect(["table", "no-guid", "empty", "no-entries"]).toContain(mode);
    }
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
      ["table", "empty", "no-entries"],
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
      names.push(
        (
          (await openButtons.nth(i).getAttribute("data-df-name")) ||
          (await openButtons.nth(i).innerText())
        ).trim(),
      );
    }
    const byAuthor = names.find((n) => n === "By_Author");
    const peer = names.find((n) => n && n !== "By_Author");
    const toOpen = [byAuthor, peer].filter(Boolean);
    expect(
      toOpen.length,
      `need By_Author and/or a peer in catalog (got ${names.join(", ")})`,
    ).toBeGreaterThan(0);

    for (const name of toOpen) {
      // Exact data-df-name — not hasText substring (#3269 / #3200).
      const openExact = page.locator(
        catalogOpenByExactName("developer-df-open", "data-df-name", name),
      );
      await expect(openExact, `unique open for ${name}`).toHaveCount(1);
      await expect(
        page.locator(catalogRowByExactName("data-df-name", name)),
        `unique row for ${name}`,
      ).toHaveCount(1);
      await openExact.click();
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

  test("action menu detail GUID loads Object ACL (#3380)", async ({ page }) => {
    await page.goto(developerSectionUrl("action-menus"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-action-menus"]'),
    ).toBeVisible({ timeout: 15_000 });

    const opened = await openFirstCatalogDetail(page, {
      rowBase: "developer-am-row",
      panel: "developer-am-panel",
      empty: "developer-am-empty",
      error: "developer-am-error",
      detail: "developer-am-detail",
      detailLoading: "developer-am-detail-loading",
      detailError: "developer-am-detail-error",
      catalogLabel: "action menus",
    });
    if (!opened) return;

    const guidCell = page.locator('[data-testid="developer-am-detail-guid"]');
    await expect(guidCell).toBeVisible({ timeout: 15_000 });
    const guidText = (await guidCell.innerText()).trim();
    expect(
      guidText && guidText !== "—" && guidText !== "-",
      `action-menu detail GUID must be present (got "${guidText}")`,
    ).toBeTruthy();
    const mode = await assertLayeredObjectAcl(page, "developer-am-acl", "action-menu");
    expect(
      ["table", "empty", "no-entries"],
      `action-menu Object ACL must load with GUID "${guidText}" (got mode=${mode})`,
    ).toContain(mode);
  });

  test("view detail GUID loads Object ACL (#3380)", async ({ page }) => {
    await page.goto(developerSectionUrl("views"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-views"]'),
    ).toBeVisible({ timeout: 15_000 });

    const opened = await openFirstCatalogDetail(page, {
      rowBase: "developer-vw-row",
      panel: "developer-vw-panel",
      empty: "developer-vw-empty",
      error: "developer-vw-error",
      detail: "developer-vw-detail",
      detailLoading: "developer-vw-detail-loading",
      detailError: "developer-vw-detail-error",
      catalogLabel: "views",
    });
    if (!opened) return;

    const guidCell = page.locator('[data-testid="developer-vw-detail-guid"]');
    await expect(guidCell).toBeVisible({ timeout: 15_000 });
    const guidText = (await guidCell.innerText()).trim();
    expect(
      guidText && guidText !== "—" && guidText !== "-",
      `view detail GUID must be present (got "${guidText}")`,
    ).toBeTruthy();
    const mode = await assertLayeredObjectAcl(page, "developer-vw-acl", "view");
    expect(
      ["table", "empty", "no-entries"],
      `view Object ACL must load with GUID "${guidText}" (got mode=${mode})`,
    ).toContain(mode);
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

  test("preferences Runtime visibility persists after save and reload (#3204)", async ({
    page,
  }) => {
    const jsErrors = [];
    page.on("pageerror", (err) => jsErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      // GET /preferences/{name} 404 is the empty-store path; chrome 404s
      // (favicon / leftover hashed chunks) are not product defects.
      if (/404|Failed to load resource/i.test(text)) {
        return;
      }
      jsErrors.push(text);
    });

    await page.goto(developerSectionUrl("preferences"), {
      waitUntil: "networkidle",
    });
    await expect(
      page.locator('[data-testid="tab-developer-preferences"]'),
    ).toBeVisible({ timeout: 15_000 });

    const table = page.locator('[data-testid="developer-prefs-acl-table"]');
    const loading = page.locator('[data-testid="developer-prefs-acl-loading"]');
    await expect(loading).toBeHidden({ timeout: 30_000 }).catch(() => {});
    await expect(table).toBeVisible({ timeout: 20_000 });

    const defaultRuntime = page.locator(
      '[data-testid="developer-prefs-acl-table"] tr[data-acl-principal="Default"] input[type="checkbox"][data-testid$="-RUNTIME_VISIBLE"]',
    );
    await expect(defaultRuntime).toBeVisible({ timeout: 10_000 });

    const wasChecked = await defaultRuntime.isChecked();
    await defaultRuntime.click();
    await expect(defaultRuntime).toBeChecked({ checked: !wasChecked });

    const saveBtn = page.locator('[data-testid="developer-prefs-acl-save"]');
    await expect(saveBtn).toBeEnabled({ timeout: 10_000 });
    await saveBtn.click();
    await expect(
      page.locator('[data-testid="developer-prefs-acl-notice"]'),
    ).toBeVisible({ timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-prefs-acl-source"]'),
    ).toContainText(/saved/i);

    await page.goto(developerSectionUrl("preferences"), {
      waitUntil: "networkidle",
    });
    await expect(loading).toBeHidden({ timeout: 30_000 }).catch(() => {});
    await expect(table).toBeVisible({ timeout: 20_000 });

    const reloaded = page.locator(
      '[data-testid="developer-prefs-acl-table"] tr[data-acl-principal="Default"] input[type="checkbox"][data-testid$="-RUNTIME_VISIBLE"]',
    );
    await expect(reloaded).toBeVisible({ timeout: 10_000 });
    await expect(reloaded).toBeChecked({ checked: !wasChecked });
    await expect(
      page.locator('[data-testid="developer-prefs-acl-source"]'),
    ).toContainText(/saved/i);

    expect(
      jsErrors,
      `JS console/page errors on prefs persist path: ${jsErrors.join(" | ")}`,
    ).toEqual([]);
  });
});
