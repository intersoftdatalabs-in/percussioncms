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
 * Playwright: #5200 / parent #4530 — Content → Change page template.
 *
 * Tags: @explorer-change-page-template @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-change-page-template.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { folderListingPhase } = require("./helpers/explorer-set-community");
const { openContentMenu } = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-change-page-template", "@explorer", "@smoke"];

const TEST_IDS = Object.freeze({
  shell: "content-explorer-shell",
  menuItem: "explorer-change-page-template",
  dialog: "explorer-change-page-template-dialog",
  select: "explorer-change-page-template-select",
  save: "explorer-change-page-template-save",
  cancel: "explorer-change-page-template-cancel",
  status: "explorer-change-page-template-status",
  dialogError: "explorer-change-page-template-dialog-error",
  tree: "explorer-tree",
  detailList: "detail-list",
});

const FIELDS = {
  ItemEditorFields: {
    contentId: "551",
    contentType: "percPage",
    name: "Home",
    fields: [{ name: "templateid", value: "101" }],
  },
};

const TYPE_DETAIL = {
  ContentTypeDetail: {
    name: "percPage",
    allowedTemplates: [
      { name: "101", label: "Article" },
      { name: "202", label: "Blog" },
    ],
  },
};

function explorerUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

function isKnownNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
    String(text || ""),
  );
}

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownNoise(msg.text())) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

const DETAIL_ROWS =
  '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"]';

async function readListingPhase(page) {
  const listVisible =
    (await page.locator(`[data-testid="${TEST_IDS.detailList}"]`).count()) > 0;
  const rowCount = await page.locator(DETAIL_ROWS).count();
  const emptyVisible =
    (await page
      .locator('[data-testid="detail-list"] [data-testid="detail-list-empty"]')
      .count()) > 0;
  return folderListingPhase({ listVisible, rowCount, emptyVisible });
}

async function listingSignature(page) {
  const rows = page.locator(DETAIL_ROWS);
  const count = await rows.count();
  if (count === 0) {
    const empty = await page
      .locator('[data-testid="detail-list"] [data-testid="detail-list-empty"]')
      .count();
    return empty > 0 ? "empty" : "none";
  }
  const take = Math.min(count, 4);
  const ids = [];
  for (let i = 0; i < take; i += 1) {
    ids.push((await rows.nth(i).getAttribute("data-testid")) || "");
  }
  return `rows:${count}:${ids.join("|")}`;
}

async function listReady(page) {
  await expect
    .poll(async () => readListingPhase(page), { timeout: 20_000 })
    .not.toBe("loading");
}

async function activateForListing(page, target, opts = {}) {
  const before = await listingSignature(page);
  try {
    if (opts.dblclick) {
      await target.dblclick({ force: true });
    } else {
      await target.click({ force: true });
    }
    await expect
      .poll(
        async () => {
          const phase = await readListingPhase(page);
          const signature = await listingSignature(page);
          if (phase === "loading" || !signature.startsWith("rows:")) {
            return "pending";
          }
          return signature !== before ? "settled" : "pending";
        },
        { timeout: 15_000 },
      )
      .toBe("settled");
    return true;
  } catch {
    return false;
  }
}

function treeNodeLabel(root) {
  return root.locator('[role="treeitem"] span:not([data-testid^="tree-toggle-"])').first();
}

async function openFirstFolderRow(page) {
  const folderRow = page.locator(
    '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
  );
  if ((await folderRow.count()) === 0) {
    return false;
  }
  const icon = folderRow.first().locator('[data-testid^="detail-folder-icon-"]');
  if ((await icon.count()) > 0) {
    return activateForListing(page, icon.first());
  }
  return activateForListing(page, folderRow.first(), { dblclick: true });
}

async function rootAlreadyListed(page, root) {
  const selected = root.first().locator('[role="treeitem"][aria-selected="true"]');
  if ((await selected.count()) === 0) {
    return false;
  }
  return (await readListingPhase(page)) === "ready";
}

async function selectFirstItemUnder(page, rootName) {
  await expect(
    page
      .locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}"]`,
      )
      .first(),
  ).toBeVisible({ timeout: 30_000 });
  const root = page.locator(
    `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}"]`,
  );
  const row = root.first().locator('[role="treeitem"]').first();
  let openedRoot = await rootAlreadyListed(page, root);
  if (!openedRoot) {
    await row.click({ force: true });
    try {
      await expect
        .poll(async () => ((await rootAlreadyListed(page, root)) ? "ready" : "pending"), {
          timeout: 15_000,
        })
        .toBe("ready");
      openedRoot = true;
    } catch {
      openedRoot = false;
    }
  }
  if (!openedRoot) {
    return false;
  }
  for (let depth = 0; depth < 6; depth += 1) {
    const itemRow = page.locator(
      '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])',
    );
    if ((await itemRow.count()) > 0) {
      await itemRow.first().click({ force: true, timeout: 10_000 });
      return true;
    }
    const opened = await openFirstFolderRow(page);
    if (!opened) {
      break;
    }
  }
  return false;
}

async function stubTemplateReads(page) {
  await page.route("**/itemmanagement/item/fields/**", (route) => {
    if (route.request().method() !== "GET") {
      return route.continue();
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(FIELDS),
    });
  });
  await page.route("**/services/contenttypes/percPage**", (route) => {
    const url = route.request().url();
    if (!/\/contenttypes\/percPage(?:\?|$)/.test(url)) {
      return route.continue();
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(TYPE_DETAIL),
    });
  });
}

function selectedRow(page) {
  return page.locator(`${DETAIL_ROWS}[data-selected="true"]`);
}

function isAssetsFolderList(url) {
  let decoded = String(url || "");
  try {
    decoded = decodeURIComponent(decoded);
  } catch {
    /* Keep the raw URL when a segment is not valid encoding. */
  }
  const path = decoded.split("?")[0].replace(/\/+$/, "");
  // Explorer lists /Assets through //Folders/$System$/Assets.
  return /\/paginatedFolder\/(?:Folders\/\$System\$\/)?Assets$/.test(path);
}

/**
 * H2 /Assets is often empty. Show one image row that reuses a real content
 * id so selection does not look up a missing component summary.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} itemId
 */
async function stubOneAssetInAssets(page, itemId) {
  await page.route("**/pathmanagement/path/paginatedFolder/**", (route) => {
    if (!isAssetsFolderList(route.request().url())) {
      return route.continue();
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PagedItemList: {
          childrenCount: 1,
          startIndex: 0,
          childrenInPage: [
            {
              id: itemId,
              name: "logo.png",
              path: "/Assets/logo.png",
              folderPath: "/Assets",
              type: "percImageAsset",
              category: "asset",
              leaf: true,
            },
          ],
        },
      }),
    });
  });
}

test.describe("Explorer change page template (#5200 / #4530)", () => {
  test(
    "UI: the selection shows the new template only after save",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const puts = [];
      const jsErrors = await openExplorer(page);
      const found = await selectFirstItemUnder(page, "Sites");
      expect(found, "H2 Explorer has no selectable page under Sites").toBe(true);
      await stubTemplateReads(page);
      await page.route("**/pagemanagement/page/changeTemplate/**", (route) => {
        puts.push(route.request().url());
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: "{}",
        });
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const dialog = page.locator(`[data-testid="${TEST_IDS.dialog}"]`);
      await expect(dialog).toBeVisible();
      await expect(dialog).toHaveAttribute("data-current-template-id", "101");
      await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("202");
      expect(await selectedRow(page).getAttribute("data-page-template-id")).toBeNull();
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-page-template-id", "202");
      await expect(status).toHaveAttribute("data-page-template-name", "Blog");
      await expect(status).toContainText("Page template saved");
      await expect(status).toContainText("Blog");
      await expect(dialog).toHaveCount(0);
      await expect(selectedRow(page)).toHaveAttribute("data-page-template-id", "202");
      await expect(selectedRow(page)).toHaveAttribute("data-page-template-name", "Blog");
      expect(puts.some((url) => url.includes("/202"))).toBe(true);
      await expectNoSeriousA11yViolations(page, `[data-testid="${TEST_IDS.shell}"]`);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: cancel does not write and keeps the previous template",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      let posted = false;
      const jsErrors = await openExplorer(page);
      const found = await selectFirstItemUnder(page, "Sites");
      expect(found, "H2 Explorer has no selectable page under Sites").toBe(true);
      await stubTemplateReads(page);
      await page.route("**/pagemanagement/page/changeTemplate/**", (route) => {
        posted = true;
        return route.fulfill({ status: 500, body: "{}" });
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toBeVisible();
      await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("202");
      await page.locator(`[data-testid="${TEST_IDS.cancel}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveCount(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.status}"]`)).toHaveCount(0);
      expect(await selectedRow(page).getAttribute("data-page-template-id")).toBeNull();
      expect(posted).toBe(false);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: HTTP 400, 403, and 409 keep the previous template",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(180_000);
      const jsErrors = await openExplorer(page);
      const found = await selectFirstItemUnder(page, "Sites");
      expect(found, "H2 Explorer has no selectable page under Sites").toBe(true);
      let storedTemplateId = "101";
      await page.route("**/itemmanagement/item/fields/**", (route) => {
        if (route.request().method() !== "GET") {
          return route.continue();
        }
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemEditorFields: {
              ...FIELDS.ItemEditorFields,
              fields: [{ name: "templateid", value: storedTemplateId }],
            },
          }),
        });
      });
      await page.route("**/services/contenttypes/percPage**", (route) => {
        const url = route.request().url();
        if (!/\/contenttypes\/percPage(?:\?|$)/.test(url)) {
          return route.continue();
        }
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(TYPE_DETAIL),
        });
      });
      let nextStatus = 200;
      await page.route("**/pagemanagement/page/changeTemplate/**", (route) => {
        if (nextStatus === 200) {
          const parts = route.request().url().split("/").filter(Boolean);
          storedTemplateId = decodeURIComponent(parts[parts.length - 1] || "202");
        }
        return route.fulfill({
          status: nextStatus,
          contentType: "text/plain",
          body: nextStatus === 200 ? "{}" : "refused",
        });
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("202");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(selectedRow(page)).toHaveAttribute("data-page-template-id", "202");

      for (const status of [400, 403, 409]) {
        nextStatus = status;
        await openContentMenu(page);
        await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toBeVisible();
        await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveAttribute(
          "data-current-template-id",
          "202",
        );
        await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("101");
        await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.dialogError}"]`)).toContainText(
          String(status),
        );
        await expect(selectedRow(page)).toHaveAttribute("data-page-template-id", "202");
        await expect(selectedRow(page)).toHaveAttribute("data-page-template-name", "Blog");
        await page.locator(`[data-testid="${TEST_IDS.cancel}"]`).click();
      }
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: an asset is refused and nothing is written",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      let wrote = false;
      const jsErrors = await openExplorer(page);
      const pageFound = await selectFirstItemUnder(page, "Sites");
      expect(pageFound, "H2 Explorer has no page whose id can back an asset row").toBe(true);
      const realId = (await selectedRow(page).getAttribute("data-item-id")) || "";
      expect(realId, "selected page has no data-item-id").not.toEqual("");
      await stubOneAssetInAssets(page, realId);
      const found = await selectFirstItemUnder(page, "Assets");
      expect(found, "H2 Explorer has no selectable asset under Assets").toBe(true);
      await page.route("**/itemmanagement/item/fields/**", (route) => {
        wrote = true;
        return route.continue();
      });
      await page.route("**/pagemanagement/page/changeTemplate/**", (route) => {
        wrote = true;
        return route.fulfill({ status: 500, body: "{}" });
      });
      await openContentMenu(page);
      wrote = false;
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "asset");
      await expect(status).toContainText("Assets do not have a page template");
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveCount(0);
      expect(await selectedRow(page).getAttribute("data-page-template-id")).toBeNull();
      expect(wrote).toBe(false);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: empty selection and a folder do not claim success",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toHaveAttribute(
        "data-selected-item-id",
        "",
      );
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const emptyStatus = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(emptyStatus).toHaveAttribute("data-reason", "empty");
      await expect(emptyStatus).toContainText(
        "Select a page before changing its page template",
      );
      const root = page.locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
      );
      await expect(root.first()).toBeVisible({ timeout: 30_000 });
      const label = treeNodeLabel(root.first());
      await ((await label.count()) > 0 ? label : root.first()).click();
      await listReady(page);
      const folderRow = page.locator(
        '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
      );
      await expect(folderRow.first()).toBeVisible({ timeout: 20_000 });
      await folderRow.first().click({ force: true });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-reason", "folder");
      await expect(status).toContainText("Folders do not have a page template");
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveCount(0);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
