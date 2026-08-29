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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Preview-first Content Editor host — chrome-less field form, plus
 * Explorer Open/Edit of a selected page on H2 (#3638 / parent #3102).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-content-editor.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS: PREVIEW_IDS,
  explorerEntryUrl,
  encodeCmsRelPath,
  isListedPageRow,
  isPreviewableRow,
  unwrapPathItems,
  resolveExplorerListPath,
  listedPageSiteNames,
  foldSiteName,
  detailRowHasExactName,
  detailRowMatchesFoldedSite,
} = require("./helpers/explorer-preview-view");
const {
  TEST_IDS,
  shouldSkipListedPageEditor,
  noListedItemSkipMessage,
  isProductEditorUrl,
  isLeftoverContentEditorUrl,
  isKeywordTrimCrash,
  isEditorStayVisible,
} = require("./helpers/explorer-content-editor");

function editorSpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const params = new URLSearchParams(query.startsWith("?") ? query.slice(1) : query);
  params.set("entry", "editor");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

const PATH_FOLDER = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folder`;
const PATH_PAGED = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/paginatedFolder`;

async function listWaitReady(page) {
  await page.locator(`[data-testid="${PREVIEW_IDS.list}"]`).waitFor({
    timeout: 15_000,
  });
}

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} cmsPath
 * @returns {Promise<object[]>}
 */
async function fetchFolderChildren(request, cmsPath) {
  const headers = adminBasicAuthHeaders();
  const rel = encodeCmsRelPath(cmsPath);
  if (!rel) {
    return [];
  }
  const paged = await request.get(
    `${PATH_PAGED}/${rel}?startIndex=0&maxResults=50`,
    { headers },
  );
  if (paged.status() === 200) {
    const kids = unwrapPathItems(await paged.json());
    if (kids.length > 0) {
      return kids;
    }
  }
  const folder = await request.get(`${PATH_FOLDER}/${rel}`, { headers });
  if (folder.status() === 200) {
    return unwrapPathItems(await folder.json());
  }
  return [];
}

/**
 * Walk Sites → site → Pages for a listed page/asset (peer #3627).
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<object|null>}
 */
async function findListedPageViaRest(request) {
  const sites = await fetchFolderChildren(request, "Sites");
  const candidateFolders = [];
  for (const site of sites) {
    const listPath = resolveExplorerListPath(site);
    if (listPath) {
      candidateFolders.push(listPath.replace(/^\/+/, ""));
      candidateFolders.push(`${listPath.replace(/^\/+/, "")}/Pages`);
    }
    const name = site && site.name ? String(site.name) : "";
    if (name) {
      candidateFolders.push(`Sites/${name}`);
      candidateFolders.push(`Sites/${name}/Pages`);
    }
  }

  const seen = new Set();
  for (const folder of candidateFolders) {
    if (!folder || seen.has(folder)) continue;
    seen.add(folder);
    const kids = await fetchFolderChildren(request, folder);
    const page = kids.find((k) => isListedPageRow(k) || isPreviewableRow(k));
    if (page) return page;
    for (const kid of kids.slice(0, 12)) {
      const kidType = `${kid.type || ""} ${kid.category || ""}`.toLowerCase();
      const looksFolder =
        kidType.includes("folder") ||
        kidType.includes("site") ||
        String(kid.path || "").endsWith("/");
      if (!looksFolder) continue;
      const nestedPath = resolveExplorerListPath(kid);
      const nestedRel = nestedPath
        ? nestedPath.replace(/^\/+/, "")
        : `${folder}/${kid.name || ""}`;
      if (!nestedRel || seen.has(nestedRel)) continue;
      seen.add(nestedRel);
      const nested = await fetchFolderChildren(request, nestedRel);
      const nestedPage = nested.find(
        (k) => isListedPageRow(k) || isPreviewableRow(k),
      );
      if (nestedPage) return nestedPage;
    }
  }
  return null;
}

/**
 * Prefer the folder-icon open control (peer #3328); fall back to dblclick.
 * @param {import("@playwright/test").Locator} row
 */
async function openDetailFolderRow(row) {
  const icon = row.locator('[data-testid^="detail-folder-icon-"]').first();
  try {
    await icon.waitFor({ state: "attached", timeout: 1_000 });
    await icon.click();
  } catch {
    await row.dblclick({ force: true });
  }
  const page = row.page();
  await listWaitReady(page);
}

/**
 * Open the Pages child when present. Match the Name cell (#3463).
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} list
 */
async function openPagesFolderIfPresent(page, list) {
  const pagesRow = list
    .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]')
    .filter({ hasText: "Pages" })
    .first();
  try {
    await pagesRow.waitFor({ state: "attached", timeout: 3_000 });
  } catch {
    return;
  }
  const text = ((await pagesRow.innerText().catch(() => "")) || "").trim();
  if (!detailRowHasExactName(text, "Pages") && !/\bPages\b/.test(text)) {
    return;
  }
  await openDetailFolderRow(pagesRow);
}

/**
 * Open Sites → owning site → Pages when present (peer #3627).
 * @param {import("@playwright/test").Page} page
 * @param {object} [listed]
 */
async function openSitesThenPages(page, listed) {
  const tree = page.locator(`[data-testid="${PREVIEW_IDS.tree}"]`);
  const sitesNode = tree
    .locator(
      '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"]',
    )
    .first();
  await expect(sitesNode).toBeVisible({ timeout: 15_000 });
  await sitesNode.click({ force: true });
  await listWaitReady(page);

  const toggle = sitesNode.locator('[aria-hidden="true"]').first();
  if ((await toggle.count()) > 0) {
    await toggle.click();
  }
  const siteTreeNodes = tree.locator(
    '[data-testid^="tree-node-/Sites/"]:not([data-testid="tree-node-/Sites/"])',
  );
  await expect(siteTreeNodes.first()).toBeVisible({ timeout: 15_000 });

  const wanted = new Set(listedPageSiteNames(listed).map((n) => foldSiteName(n)));
  const listedName = listed && listed.name ? String(listed.name) : "";
  const siteCount = await siteTreeNodes.count();
  let clicked = false;
  for (let i = 0; i < siteCount; i += 1) {
    const node = siteTreeNodes.nth(i);
    const testid = (await node.getAttribute("data-testid")) || "";
    const folded = foldSiteName(testid);
    const nameMatch =
      wanted.size === 0 || [...wanted].some((n) => folded.includes(n));
    if (!nameMatch) continue;
    await node.click({ force: true });
    clicked = true;
    break;
  }
  if (!clicked) {
    await siteTreeNodes.first().click({ force: true });
  }
  await listWaitReady(page);

  const list = page.locator(`[data-testid="${PREVIEW_IDS.list}"]`);
  const siteRows = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
  );
  let openedSite = false;
  const siteHint = String(listedPageSiteNames(listed)[0] || "Corporate").replace(
    /[_\s-]+/g,
    ".*",
  );
  const matchingSite = siteRows.filter({ hasText: new RegExp(siteHint, "i") }).first();
  try {
    await matchingSite.waitFor({ state: "attached", timeout: 5_000 });
    await openDetailFolderRow(matchingSite);
    openedSite = true;
  } catch {
    openedSite = false;
  }
  if (!openedSite) {
    const firstSite = siteRows.first();
    try {
      await firstSite.waitFor({ state: "attached", timeout: 5_000 });
      await openDetailFolderRow(firstSite);
    } catch {
      // Tree click may already have opened the site listing.
    }
  }

  await openPagesFolderIfPresent(page, list);

  const candidates = [
    list.locator('tbody tr[data-testid^="detail-row-"][data-row-kind="item"]').first(),
    list
      .locator('tbody tr[data-testid^="detail-row-"][data-previewable="true"]')
      .first(),
  ];
  if (listedName) {
    candidates.push(
      list
        .locator('tbody tr[data-testid^="detail-row-"]')
        .filter({ hasText: listedName })
        .first(),
    );
  }
  if (/\bHome\b/i.test(listedName)) {
    candidates.push(
      list
        .locator('tbody tr[data-testid^="detail-row-"]')
        .filter({ hasText: "Home" })
        .first(),
    );
  }
  for (const loc of candidates) {
    try {
      await loc.waitFor({ state: "attached", timeout: 8_000 });
      return;
    } catch {
      // try the next locator
    }
  }
  if (wanted.size > 0 && listed) {
    throw new Error(
      `REST listed page ${listedName || listed.id} but Explorer list has no ` +
        `item row after opening site among ${[...wanted].join(", ")} — do not skip`,
    );
  }
}

test.describe("modern React Content Editor — first slice", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "editor entry is chrome-less and does not open CM1 editor",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("checkoutedit.xml") ||
          u.includes("contenteditorurls.html") ||
          u.includes("sys_ceSupport") ||
          /view=editor/.test(u)
        ) {
          blocked.push(u);
        }
      });

      await page.goto(editorSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="editor-host"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-overlay"]')).toBeVisible();
      await expect(page.locator('[data-testid="perc-spa-app"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="editor-error"]')).toBeVisible();
      expect(blocked, `CM1 / Data Flow editor must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
      });
    },
  );

  test(
    "Explorer Edit does not open CM1 ?view=editor",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        if (isLeftoverContentEditorUrl(req.url())) {
          blocked.push(req.url());
        }
      });
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 20_000,
      });
      const edit = page.locator('[data-testid="action-toolbar-item-Edit"]');
      if (await edit.isVisible()) {
        await edit.click();
      }
      expect(blocked, `CM1 editor must not be requested: ${blocked.join(" ")}`).toEqual([]);
    },
  );

  test(
    "Explorer New Item does not open leftover Content Editor HTML",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("rx_ce") ||
          u.includes("contenteditorurls.html") ||
          u.includes("checkoutedit.xml")
        ) {
          blocked.push(u);
        }
      });
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 20_000,
      });
      const neu = page.locator('[data-testid="action-toolbar-item-New"]');
      if (await neu.isVisible()) {
        await neu.click();
      }
      expect(blocked, `Data Flow CE HTML must not be requested: ${blocked.join(" ")}`).toEqual([]);
    },
  );

  async function openPercPageTemplatePicker(page, createBodies) {
    await page.route("**/services/contenttypes/**", async (route) => {
      const url = route.request().url();
      if (!/percPage|\/Page(?:\?|$)/i.test(url)) {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ContentTypeDetail: {
            name: "percPage",
            allowedTemplates: [
              { name: "t1", label: "Home", guid: { stringValue: "tpl-home" } },
              { name: "t2", label: "Interior", guid: { stringValue: "tpl-in" } },
            ],
          },
        }),
      });
    });
    await page.route("**/services/itemmanagement/item/create**", async (route) => {
      createBodies.push(route.request().postData() || "");
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemCreateResult: {
            itemId: "1-101-1",
            folderPath: "//Sites/Demo",
            name: "New-percPage.html",
            contentType: "percPage",
          },
        }),
      });
    });

    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
    await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
      timeout: 20_000,
    });

    const tree = page.locator('[data-testid="explorer-tree"]');
    if ((await tree.count()) > 0) {
      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
        )
        .first();
      if ((await sitesNode.count()) > 0) {
        await sitesNode.click({ force: true, timeout: 10_000 }).catch(() => {});
      }
    }

    const neu = page.locator('[data-testid="action-toolbar-item-New"]');
    if ((await neu.count()) === 0 || !(await neu.isVisible())) {
      return { skipped: "New menu is not visible" };
    }
    await neu.click();
    const percPage = page.locator(
      '[data-testid="action-toolbar-item-percPage"], [data-testid="action-toolbar-item-Page"]',
    );
    if ((await percPage.count()) === 0) {
      return { skipped: "percPage is not listed under New" };
    }
    await percPage.first().click();
    const picker = page.locator('[data-testid="explorer-template-picker"]');
    await expect(picker).toBeVisible({ timeout: 10_000 });
    return { picker };
  }

  test(
    "Explorer New percPage offers a template picker and does not create on cancel",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const createBodies = [];
      const opened = await openPercPageTemplatePicker(page, createBodies);
      if (opened.skipped) {
        test.skip(true, opened.skipped);
        return;
      }
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-template-picker"]',
      });
      await page.locator('[data-testid="explorer-template-picker-cancel"]').click();
      await expect(opened.picker).toHaveCount(0);
      expect(createBodies, "Cancel must not POST create").toEqual([]);
    },
  );

  test(
    "Explorer New percPage posts the picked templateId",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const createBodies = [];
      const opened = await openPercPageTemplatePicker(page, createBodies);
      if (opened.skipped) {
        test.skip(true, opened.skipped);
        return;
      }
      await page.locator('[data-testid="explorer-template-picker-select"]').selectOption("tpl-in");
      await page.locator('[data-testid="explorer-template-picker-ok"]').click();
      await expect.poll(() => createBodies.length).toBe(1);
      expect(createBodies[0]).toMatch(/"templateId"\s*:\s*"tpl-in"/);
    },
  );

  test(
    "Open/Edit selected page lands React editor; folders stay non-editable",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      const leftover = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("request", (req) => {
        if (isLeftoverContentEditorUrl(req.url())) {
          leftover.push(req.url());
        }
      });

      await page.goto(explorerEntryUrl(BASE_URL, { cacheBuster: Date.now() }));

      const listed = await findListedPageViaRest(page.request);

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });
      await expect(page.locator(`[data-testid="${TEST_IDS.reducedActions}"]`)).toBeVisible();
      await listWaitReady(page);

      await openSitesThenPages(page, listed);

      const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
      await expect(list).toBeVisible({ timeout: 15_000 });

      const listedName = listed && listed.name ? String(listed.name) : "";
      let pageRow = listedName
        ? list
            .locator('tbody tr[data-testid^="detail-row-"]')
            .filter({ hasText: listedName })
            .first()
        : list
            .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="item"]')
            .first();
      try {
        await pageRow.waitFor({ state: "attached", timeout: 5_000 });
      } catch {
        pageRow = list
          .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="item"]')
          .first();
        try {
          await pageRow.waitFor({ state: "attached", timeout: 5_000 });
        } catch {
          if (
            shouldSkipListedPageEditor({
              listedPage: listed,
              itemRowCount: 0,
            })
          ) {
            test.skip(true, noListedItemSkipMessage());
            return;
          }
          throw new Error(
            `REST listed page ${listedName || (listed && listed.id) || "unknown"} ` +
              `but Explorer detail list has no page/item row (H2 — do not skip)`,
          );
        }
      }

      await pageRow.click({ force: true });
      const openBtn = page.locator(`[data-testid="${TEST_IDS.open}"]`);
      await expect(openBtn).toBeEnabled({ timeout: 10_000 });

      const popupPromise = page.waitForEvent("popup", { timeout: 15_000 });
      const editorDocPromise = page
        .waitForResponse(
          (res) =>
            isProductEditorUrl(res.url()) &&
            res.request().resourceType() === "document",
          { timeout: 15_000 },
        )
        .catch(() => null);
      await openBtn.click();
      let popup = await popupPromise.catch(() => null);
      if (!popup) {
        const edit = page.locator(`[data-testid="${TEST_IDS.edit}"]`);
        await expect(edit).toBeVisible({ timeout: 10_000 });
        const editPopup = page.waitForEvent("popup", { timeout: 15_000 });
        await edit.click();
        popup = await editPopup;
      }
      const editorDoc = await editorDocPromise;
      if (editorDoc) {
        expect(
          editorDoc.status(),
          `React editor document should be HTTP 200; got ${editorDoc.status()} ${editorDoc.url()}`,
        ).toBe(200);
      }

      let popupUrl = popup ? popup.url() : "";
      if (popup && (!popupUrl || /about:blank/i.test(popupUrl))) {
        await popup.waitForLoadState("domcontentloaded").catch(() => {});
        popupUrl = popup.url();
      }
      expect(
        isProductEditorUrl(popupUrl),
        `Open/Edit popup URL should be spa.jsp?entry=editor; got ${popupUrl}`,
      ).toBe(true);
      if (popup && !popup.isClosed()) {
        await expect(
          popup.locator(`[data-testid="${TEST_IDS.editorHost}"]`),
        ).toBeVisible({ timeout: 20_000 });
        await popup.close().catch(() => {});
      }

      const selectableFolder = list
        .locator(
          'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
        )
        .first();
      let folderAttached = false;
      try {
        await selectableFolder.waitFor({ state: "attached", timeout: 2_000 });
        folderAttached = true;
      } catch {
        folderAttached = false;
      }
      if (folderAttached) {
        await selectableFolder.click({ force: true });
        await expect(page.locator(`[data-testid="${TEST_IDS.edit}"]`)).toHaveCount(
          0,
          { timeout: 5_000 },
        );
        const folderPopup = page
          .waitForEvent("popup", { timeout: 2_000 })
          .then((win) => win)
          .catch(() => null);
        await openBtn.click();
        const maybeEditor = await folderPopup;
        if (maybeEditor && !maybeEditor.isClosed()) {
          const folderUrl = maybeEditor.url();
          expect(
            isProductEditorUrl(folderUrl),
            `Folder Open must not land the React editor; got ${folderUrl}`,
          ).toBe(false);
          await maybeEditor.close().catch(() => {});
        }
      }

      expect(
        leftover,
        `Data Flow CE HTML must not be requested: ${leftover.join(" ")}`,
      ).toEqual([]);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Explorer right-click Edit stays on the React editor (#3968)",
    { tag: ["@explorer-content-editor", "@explorer"], timeout: 180_000 },
    async ({ page }) => {
      test.setTimeout(180_000);
      const pageErrors = [];
      const leftover = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("request", (req) => {
        if (isLeftoverContentEditorUrl(req.url())) {
          leftover.push(req.url());
        }
      });

      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "domcontentloaded" });

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });
      await listWaitReady(page);

      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"]',
        )
        .first();
      await expect(sitesNode).toBeVisible({ timeout: 15_000 });
      await sitesNode.click({ force: true });
      await listWaitReady(page);

      const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
      await expect(list).toBeVisible({ timeout: 15_000 });
      const siteFolder = list
        .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]')
        .first();
      await expect(siteFolder).toBeVisible({ timeout: 15_000 });
      await openDetailFolderRow(siteFolder);
      await openPagesFolderIfPresent(page, list);

      const itemRows = list.locator(
        'tbody tr[data-testid^="detail-row-"]:not([data-row-kind="folder"])',
      );
      if (!(await itemRows.first().isVisible().catch(() => false))) {
        const nestedFolder = list
          .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]')
          .first();
        if (await nestedFolder.isVisible().catch(() => false)) {
          await openDetailFolderRow(nestedFolder);
        }
      }
      const pageRow = itemRows.first();
      await expect(pageRow).toBeVisible({ timeout: 20_000 });

      await pageRow.click({ button: "right", force: true });
      const contextMenu = page.locator('[data-testid="context-menu"]');
      await contextMenu.waitFor({ state: "visible", timeout: 5_000 }).catch(() => {});
      const contextEdit = page.locator(`[data-testid="${TEST_IDS.contextEdit}"]`);
      const toolbarEdit = page.locator(`[data-testid="${TEST_IDS.edit}"]`);
      const popupPromise = page.waitForEvent("popup", { timeout: 20_000 });
      if (await contextEdit.isVisible().catch(() => false)) {
        await contextEdit.click();
      } else {
        await pageRow.click({ force: true });
        await expect(toolbarEdit).toBeVisible({ timeout: 10_000 });
        await toolbarEdit.click();
      }
      const popup = await popupPromise;
      popup.on("pageerror", (err) => pageErrors.push(String(err)));
      popup.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      let popupUrl = popup.url();
      if (!popupUrl || /about:blank/i.test(popupUrl)) {
        await popup.waitForLoadState("domcontentloaded").catch(() => {});
        popupUrl = popup.url();
      }
      expect(
        isProductEditorUrl(popupUrl),
        `right-click Edit popup URL should be spa.jsp?entry=editor; got ${popupUrl}`,
      ).toBe(true);

      const host = popup.locator(`[data-testid="${TEST_IDS.editorHost}"]`);
      const overlay = popup.locator(`[data-testid="${TEST_IDS.editorOverlay}"]`);
      await expect(host).toBeVisible({ timeout: 20_000 });
      await expect(overlay).toBeVisible({ timeout: 10_000 });

      const form = popup.locator(`[data-testid="${TEST_IDS.editorForm}"]`);
      const editorError = popup.locator(`[data-testid="${TEST_IDS.editorError}"]`);
      const loading = popup.locator(`[data-testid="${TEST_IDS.editorLoading}"]`);
      const empty = popup.locator(`[data-testid="${TEST_IDS.editorEmpty}"]`);
      await expect(form.or(editorError).or(loading).or(empty)).toBeVisible({
        timeout: 30_000,
      });

      const stay = isEditorStayVisible({
        host: await host.isVisible(),
        overlay: await overlay.isVisible(),
        form: await form.isVisible().catch(() => false),
        error: await editorError.isVisible().catch(() => false),
        loading: await loading.isVisible().catch(() => false),
        empty: await empty.isVisible().catch(() => false),
      });
      expect(stay, "Edit must stay on the editor host, not a blank page").toBe(true);

      const trimCrashes = pageErrors.filter((msg) => isKeywordTrimCrash(msg));
      expect(
        trimCrashes,
        `KeywordFieldWidget .trim crash: ${trimCrashes.join(" | ")}`,
      ).toEqual([]);
      expect(
        leftover,
        `Data Flow CE HTML must not be requested: ${leftover.join(" ")}`,
      ).toEqual([]);

      if (popup && !popup.isClosed()) {
        await popup.close().catch(() => {});
      }
    },
  );
});
