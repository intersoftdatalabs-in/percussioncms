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
 * Playwright: #5076 / parent #4530 — Content → Set workflow.
 *
 * Tags: @explorer-set-workflow @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-set-workflow.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerSetWorkflowUrl,
  isKnownExplorerSetWorkflowConsoleNoise,
  folderListingPhase,
  listingNavigationSettled,
  unchangedListingUsable,
  isPaginatedFolderListingUrl,
} = require("./helpers/explorer-set-workflow");
const { openContentMenu } = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-set-workflow", "@explorer", "@smoke"];

const CATALOG = {
  ItemWorkflowChoices: {
    itemId: "1",
    currentWorkflowId: "4",
    choices: [
      { id: "4", name: "Simple" },
      { id: "7", name: "Local workflow" },
    ],
  },
};

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerSetWorkflowConsoleNoise(msg.text())) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerSetWorkflowUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

const DETAIL_ROWS =
  '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"]';
const FOLDER_ROWS =
  '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])';
const ITEM_ROWS =
  '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])';
/** Grace before an unchanged ready listing counts as this click (#5096). */
const UNCHANGED_LISTING_GRACE_MS = 2_000;

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
 * Click or double-click a folder control and wait until paginatedFolder
 * replaces the previous paint. Resolves false when the listing never settles
 * so the caller can try another root instead of treating a loading list as empty.
 *
 * Empty is accepted only after a paginatedFolder GET observed for this click.
 * The idle {@code !folderPath} paint is also {@code detail-list-empty} (#5089).
 * In-flight GETs that started before the click do not count, and an already
 * open ready listing is usable when this click starts no GET (#5096).
 *
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} target
 * @param {{ dblclick?: boolean, requireChange?: boolean }} [opts]
 * @returns {Promise<boolean>}
 */
async function activateForListing(page, target, opts = {}) {
  try {
    await listReady(page);
  } catch {
    return false;
  }
  const before = await listingSignature(page);
  const startedRequests = new Set();
  let requestStarted = false;
  let listingResponseSeen = false;
  const onRequest = (req) => {
    try {
      if (isPaginatedFolderListingUrl(req.url(), req.method())) {
        startedRequests.add(req);
        requestStarted = true;
      }
    } catch {
      // Ignore requests that are already disposed.
    }
  };
  const onResponse = (res) => {
    try {
      const req = res.request();
      if (startedRequests.has(req) && isPaginatedFolderListingUrl(res.url(), req.method())) {
        listingResponseSeen = true;
      }
    } catch {
      // Ignore responses whose request is already disposed.
    }
  };
  page.on("request", onRequest);
  page.on("response", onResponse);
  const startedAt = Date.now();
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
          if (listingNavigationSettled(before, phase, signature, listingResponseSeen)) {
            return "settled";
          }
          if (
            Date.now() - startedAt >= UNCHANGED_LISTING_GRACE_MS &&
            unchangedListingUsable(requestStarted, phase, signature, before)
          ) {
            return "settled";
          }
          return "pending";
        },
        { timeout: 20_000 },
      )
      .toBe("settled");
    if (opts.requireChange) {
      const after = await listingSignature(page);
      if (after === before) {
        return false;
      }
    }
    return true;
  } catch {
    return false;
  } finally {
    page.off("request", onRequest);
    page.off("response", onResponse);
  }
}

/**
 * Label span inside a tree node. The disclosure toggle stops propagation,
 * so a center click on the node row can expand without selecting.
 *
 * @param {import("@playwright/test").Locator} root
 * @returns {import("@playwright/test").Locator}
 */
function treeNodeLabel(root) {
  return root.locator('[role="treeitem"] span:not([data-testid^="tree-toggle-"])').first();
}

/**
 * Folder-icon (or row double-click) target. Prefer the Sites row when the
 * repository root is showing; otherwise the first folder (#5096).
 *
 * @param {import("@playwright/test").Page} page
 * @param {number} [index] used when Sites is not the row to open
 * @returns {Promise<{ target: import("@playwright/test").Locator, dblclick: boolean } | null>}
 */
async function folderOpenTarget(page, index = 0) {
  const sites = page.locator(
    `${FOLDER_ROWS}[data-item-name="Sites"], ${FOLDER_ROWS}[data-testid="detail-row-/Sites/"], ${FOLDER_ROWS}[data-testid="detail-row-/Sites"]`,
  );
  const folders = page.locator(FOLDER_ROWS);
  const count = await folders.count();
  if (count === 0 || index >= count) {
    return null;
  }
  const row = index === 0 && (await sites.count()) > 0 ? sites.first() : folders.nth(index);
  const icon = row.locator('[data-testid^="detail-folder-icon-"]');
  if ((await icon.count()) > 0) {
    return { target: icon.first(), dblclick: false };
  }
  return { target: row, dblclick: true };
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {number} [index]
 * @returns {Promise<boolean>}
 */
async function openFolderAt(page, index = 0) {
  const choice = await folderOpenTarget(page, index);
  if (!choice) {
    return false;
  }
  return activateForListing(page, choice.target, {
    dblclick: choice.dblclick,
    requireChange: true,
  });
}

/**
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<boolean>}
 */
async function clickFirstItem(page) {
  const itemRow = page.locator(ITEM_ROWS);
  if ((await itemRow.count()) === 0) {
    return false;
  }
  await itemRow.first().click({ force: true, timeout: 10_000 });
  return true;
}

/**
 * Descend the detail list that is already on screen. A tree click that
 * paints site rows on the poll timeout must not be discarded for Assets
 * (#5096).
 *
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<boolean>}
 */
async function walkListingForItem(page) {
  for (let depth = 0; depth < 6; depth += 1) {
    if (await clickFirstItem(page)) {
      return true;
    }
    const before = await listingSignature(page);
    let opened = await openFolderAt(page, 0);
    let after = await listingSignature(page);
    // A poll can time out as the new rows paint. Keep that listing (#5096).
    if (after !== before && after.startsWith("rows:")) {
      opened = true;
    } else if (!opened || after === before) {
      const folderCount = await page.locator(FOLDER_ROWS).count();
      opened = false;
      const tries = Math.min(folderCount, 3);
      for (let i = 1; i < tries && !opened; i += 1) {
        opened = await openFolderAt(page, i);
        after = await listingSignature(page);
        if (after !== before && (opened || after.startsWith("rows:"))) {
          opened = true;
          break;
        }
        opened = false;
      }
    }
    if (!opened) {
      return false;
    }
  }
  return false;
}

async function selectFirstContentItem(page) {
  await expect(
    page
      .locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
      )
      .first(),
  ).toBeVisible({ timeout: 30_000 });
  try {
    await listReady(page);
  } catch {
    // An empty or slow first paint can still expose folder rows below.
  }
  if (await walkListingForItem(page)) {
    return true;
  }
  const roots = ["Sites", "Assets"];
  for (const rootName of roots) {
    const root = page.locator(
      `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}"]`,
    );
    if ((await root.count()) === 0) {
      continue;
    }
    const label = treeNodeLabel(root.first());
    await activateForListing(page, (await label.count()) > 0 ? label : root.first());
    // Use rows even when the tree click timed out as they appeared (#5096).
    if (await walkListingForItem(page)) {
      return true;
    }
  }
  return false;
}

function stubCatalog(page, status = 200) {
  return page.route("**/services/itemmanagement/workflow/allowedWorkflows/**", (route) => {
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

test.describe("Explorer set workflow on the selected item (#5076 / #4530)", () => {
  test(
    "UI: save shows the new workflow only after the server accepts it",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      const jsErrors = await openExplorer(page);
      const found = await selectFirstContentItem(page);
      expect(found, "H2 Explorer has no selectable page or asset").toBe(true);
      await stubCatalog(page);
      await page.route("**/services/itemmanagement/workflow/changeWorkflow/**", (route) => {
        posts.push(route.request().url());
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemStateTransition: { workflowId: "7", stateName: "Draft", transitionTriggers: [] },
          }),
        });
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const dialog = page.locator(`[data-testid="${TEST_IDS.dialog}"]`);
      await expect(dialog).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.status}"]`)).toHaveCount(0);
      await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("7");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-workflow-id", "7");
      await expect(status).toHaveAttribute("data-workflow-name", "Local workflow");
      await expect(dialog).toHaveCount(0);
      expect(posts.some((url) => url.includes("/7"))).toBe(true);
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
      await page.route("**/services/itemmanagement/workflow/changeWorkflow/**", (route) => {
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
    "UI: a folder is named and is not success",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      const root = page.locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
      );
      await expect(root.first()).toBeVisible({ timeout: 30_000 });
      await root.first().click();
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
      await expect(status).toContainText("Folders are not assigned a workflow:");
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
      await page.route("**/services/itemmanagement/workflow/changeWorkflow/**", (route) =>
        route.fulfill({ status: 403, contentType: "text/plain", body: "forbidden" }),
      );
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("7");
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.dialogError}"]`)).toContainText("403");
      await expect(page.locator(`[data-testid="${TEST_IDS.status}"]`)).toHaveCount(0);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
