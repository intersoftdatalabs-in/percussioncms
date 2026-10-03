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
 * Playwright: #5077 / parent #4530 — Content → Set community.
 *
 * Tags: @explorer-set-community @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-set-community.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerSetCommunityUrl,
  isKnownExplorerSetCommunityConsoleNoise,
  folderListingPhase,
  listingNavigationSettled,
  isPaginatedFolderListingUrl,
} = require("./helpers/explorer-set-community");
const { openContentMenu } = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-set-community", "@explorer", "@smoke"];

const CATALOG = {
  ItemCommunityChoices: {
    itemId: "1",
    currentCommunityId: "10",
    choices: [
      { id: "10", name: "Default" },
      { id: "20", name: "Enterprise" },
    ],
  },
};

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerSetCommunityConsoleNoise(msg.text())) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerSetCommunityUrl(BASE_URL), { waitUntil: "domcontentloaded" });
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

/**
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} target
 * @param {{ dblclick?: boolean }} [opts]
 * @returns {Promise<boolean>}
 */
async function activateForListing(page, target, opts = {}) {
  const before = await listingSignature(page);
  let listingResponseSeen = false;
  const onResponse = (res) => {
    try {
      if (isPaginatedFolderListingUrl(res.url(), res.request().method())) {
        listingResponseSeen = true;
      }
    } catch {
      // Ignore responses whose request is already disposed.
    }
  };
  page.on("response", onResponse);
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
          return listingNavigationSettled(before, phase, signature, listingResponseSeen)
            ? "settled"
            : "pending";
        },
        { timeout: 20_000 },
      )
      .toBe("settled");
    return true;
  } catch {
    return false;
  } finally {
    page.off("response", onResponse);
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

async function selectFirstContentItem(page) {
  await expect(
    page
      .locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
      )
      .first(),
  ).toBeVisible({ timeout: 30_000 });
  const roots = ["Sites", "Assets"];
  for (const rootName of roots) {
    const root = page.locator(
      `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}"]`,
    );
    if ((await root.count()) === 0) {
      continue;
    }
    const label = treeNodeLabel(root.first());
    const openedRoot = await activateForListing(
      page,
      (await label.count()) > 0 ? label : root.first(),
    );
    if (!openedRoot) {
      continue;
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
  }
  return false;
}

function stubCatalog(page, status = 200) {
  return page.route("**/services/itemmanagement/item/community/allowed/**", (route) => {
    if (status !== 200) {
      return route.fulfill({ status, contentType: "application/json", body: "{}" });
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(CATALOG),
    });
  });
}

test.describe("Explorer set community on the selected item (#5077 / #4530)", () => {
  test(
    "UI: save shows the new community only after the server accepts it",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      const jsErrors = await openExplorer(page);
      const found = await selectFirstContentItem(page);
      expect(found, "H2 Explorer has no selectable page or asset").toBe(true);
      await stubCatalog(page);
      await page.route("**/services/itemmanagement/item/community/change/**", (route) => {
        posts.push(route.request().url());
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemCommunityChoices: {
              currentCommunityId: "20",
              choices: CATALOG.ItemCommunityChoices.choices,
            },
          }),
        });
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const dialog = page.locator(`[data-testid="${TEST_IDS.dialog}"]`);
      await expect(dialog).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.status}"]`)).toHaveCount(0);
      await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("20");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-community-id", "20");
      await expect(status).toHaveAttribute("data-community-name", "Enterprise");
      await expect(status).toContainText("Community saved");
      await expect(status).toContainText("Enterprise");
      await expect(dialog).toHaveCount(0);
      expect(posts.some((url) => url.includes("/20"))).toBe(true);
      await expectNoSeriousA11yViolations(page, `[data-testid="${TEST_IDS.shell}"]`);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: cancel does not save",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      let posted = false;
      const jsErrors = await openExplorer(page);
      const found = await selectFirstContentItem(page);
      expect(found, "H2 Explorer has no selectable page or asset").toBe(true);
      await stubCatalog(page);
      await page.route("**/services/itemmanagement/item/community/change/**", (route) => {
        posted = true;
        return route.fulfill({ status: 500, body: "{}" });
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toBeVisible();
      await page.locator(`[data-testid="${TEST_IDS.cancel}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveCount(0);
      await expect(page.locator(`[data-testid="${TEST_IDS.status}"]`)).toHaveCount(0);
      expect(posted).toBe(false);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: empty selection does not claim success",
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
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "empty");
      await expect(status).toContainText("Select a page or asset before setting its community");
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveCount(0);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: a folder is named and is not success",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
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
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "folder");
      await expect(status).toContainText("Folders are not assigned a community:");
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveCount(0);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: HTTP 403 stays on the dialog and is not success",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      const found = await selectFirstContentItem(page);
      expect(found, "H2 Explorer has no selectable page or asset").toBe(true);
      await stubCatalog(page);
      await page.route("**/services/itemmanagement/item/community/change/**", (route) =>
        route.fulfill({ status: 403, contentType: "text/plain", body: "forbidden" }),
      );
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("20");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.dialogError}"]`)).toContainText("403");
      await expect(page.locator(`[data-testid="${TEST_IDS.status}"]`)).toHaveCount(0);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
