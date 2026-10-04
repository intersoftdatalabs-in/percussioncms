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
 * Playwright: #5132 / parent #4530 — Content → Set allowed publish sites.
 *
 * Tags: @explorer-set-folder-allowed-sites @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-set-folder-allowed-sites.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { openContentMenu } = require("./helpers/explorer-sites-list-create");
const {
  TEST_IDS,
  explorerSetFolderAllowedSitesUrl,
  isKnownExplorerSetFolderAllowedSitesConsoleNoise,
} = require("./helpers/explorer-set-folder-allowed-sites");

const TAGS = ["@explorer-set-folder-allowed-sites", "@explorer", "@smoke"];

const FOLDER_ROWS =
  '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])';
const ITEM_ROWS =
  '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])';

function catalogBody() {
  return JSON.stringify({
    FolderAllowedSitesCatalog: {
      choices: [
        { id: "301", name: "Enterprise" },
        { id: "302", name: "Corporate" },
      ],
    },
  });
}

function propertiesBody(allowedSites) {
  const body = {
    FolderProperties: {
      id: "16777215-101-703",
      name: "CI",
      permission: { accessLevel: "ADMIN" },
    },
  };
  if (allowedSites != null) {
    body.FolderProperties.allowedSites = allowedSites;
  }
  return JSON.stringify(body);
}

function postedSites(body) {
  const match = String(body || "").match(/"allowedSites"\s*:\s*"([^"]*)"/);
  return match ? match[1] : null;
}

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (
      msg.type() === "error" &&
      !isKnownExplorerSetFolderAllowedSitesConsoleNoise(msg.text())
    ) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerSetFolderAllowedSitesUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

function sitesRoot(page) {
  return page.locator(
    `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
  );
}

const ROOT_FOLDER_NAMES = new Set([
  "Sites",
  "Folders",
  "Assets",
  "Design",
  "Search",
  "Recycling",
]);

/**
 * Sites root listing replaces the root folders. Clicking the first row
 * before that swap selects Sites, which has no folder id (#5132).
 */
async function showFolderRows(page) {
  await expect(sitesRoot(page).first()).toBeVisible({ timeout: 30_000 });
  await sitesRoot(page).first().click();
  const folderRow = page.locator(FOLDER_ROWS);
  await expect
    .poll(
      async () => {
        const count = await folderRow.count();
        if (count < 1) {
          return "";
        }
        const name = (await folderRow.first().getAttribute("data-item-name")) || "";
        const id = (await folderRow.first().getAttribute("data-item-id")) || "";
        if (!id || ROOT_FOLDER_NAMES.has(name)) {
          return "";
        }
        return id;
      },
      { timeout: 20_000 },
    )
    .not.toBe("");
  return folderRow;
}

async function selectFirstFolder(page) {
  const folderRow = await showFolderRows(page);
  const row = folderRow.first();
  const id = (await row.getAttribute("data-item-id")) || "";
  await row.locator('[data-testid^="detail-cell-"]').last().click();
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toHaveAttribute(
    "data-selected-item-id",
    id,
    { timeout: 10_000 },
  );
  return row;
}

/**
 * Stub catalog + folder properties. A successful save updates the list the
 * next GET returns. An empty allowedSites value clears the stored list.
 *
 * @param {import("@playwright/test").Page} page
 * @param {{ saveStatus?: number, onSave?: (body: string) => void, initial?: string }} [opts]
 */
async function stubAllowedSites(page, opts = {}) {
  let current = opts.initial ?? "301";
  const saveStatus = opts.saveStatus ?? 200;
  await page.route("**/pathmanagement/path/folderAllowedSitesCatalog**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: catalogBody(),
    }),
  );
  await page.route("**/pathmanagement/path/folderProperties/**", (route) => {
    if (route.request().method() !== "GET") {
      return route.continue();
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: propertiesBody(current),
    });
  });
  await page.route("**/pathmanagement/path/saveFolderProperties**", async (route) => {
    const body = route.request().postData() || "";
    if (opts.onSave) {
      opts.onSave(body);
    }
    if (saveStatus === 200) {
      const next = postedSites(body);
      if (next != null) {
        current = next;
      }
    }
    return route.fulfill({
      status: saveStatus,
      contentType: "application/json",
      body: saveStatus === 200 ? "{}" : JSON.stringify({ error: saveStatus }),
    });
  });
}

test.describe("Explorer set allowed publish sites on the selected folder (#5132 / #4530)", () => {
  test(
    "UI: save shows the allowed sites only after refresh reads them",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      const jsErrors = await openExplorer(page);
      await selectFirstFolder(page);
      await stubAllowedSites(page, {
        onSave: (body) => posts.push(body),
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const dialog = page.locator(`[data-testid="${TEST_IDS.dialog}"]`);
      await expect(dialog).toBeVisible();
      await expect(dialog).toHaveAttribute("data-current-sites", "301");
      await expect(page.locator(`[data-testid="${TEST_IDS.site301}"]`)).toBeChecked();
      await expect(page.locator(`[data-testid="${TEST_IDS.status}"]`)).toHaveCount(0);
      await page.locator(`[data-testid="${TEST_IDS.site302}"]`).check();
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-allowed-sites", "301,302");
      await expect(status).toHaveAttribute("data-allowed-site-names", "Enterprise, Corporate");
      await expect(status).toContainText("Allowed publish sites saved");
      await expect(status).toContainText("Enterprise");
      await expect(status).toContainText("Corporate");
      await expect(dialog).toHaveCount(0);
      expect(posts.some((body) => postedSites(body) === "301,302")).toBe(true);
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const again = page.locator(`[data-testid="${TEST_IDS.dialog}"]`);
      await expect(again).toBeVisible();
      await expect(again).toHaveAttribute("data-current-sites", "301,302");
      await expectNoSeriousA11yViolations(page, `[data-testid="${TEST_IDS.shell}"]`);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: a confirmed empty list clears allowed publish sites only after refresh",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const posts = [];
      const jsErrors = await openExplorer(page);
      await selectFirstFolder(page);
      await stubAllowedSites(page, {
        onSave: (body) => posts.push(body),
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toBeVisible();
      await page.locator(`[data-testid="${TEST_IDS.site301}"]`).uncheck();
      await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(status).toHaveAttribute("data-allowed-sites", "");
      await expect(status).toContainText("Allowed publish sites cleared");
      await expect(status).toContainText("all sites");
      await expect(status).not.toContainText("Allowed publish sites saved");
      expect(posts.some((body) => postedSites(body) === "")).toBe(true);
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
      await selectFirstFolder(page);
      await stubAllowedSites(page, {
        onSave: () => {
          posted = true;
        },
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
    "UI: no selection, a page, and an asset do not claim success",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      await expect(sitesRoot(page).first()).toBeVisible({ timeout: 30_000 });
      await sitesRoot(page).first().click();
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const empty = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(empty).toHaveAttribute("data-kind", "error");
      await expect(empty).toHaveAttribute("data-reason", "empty");
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveCount(0);

      let foundItem = false;
      for (let depth = 0; depth < 6 && !foundItem; depth += 1) {
        const itemRow = page.locator(ITEM_ROWS);
        if ((await itemRow.count()) > 0) {
          await itemRow.first().click({ force: true });
          foundItem = true;
          break;
        }
        const folders = page.locator(FOLDER_ROWS);
        await expect(folders.first()).toBeVisible({ timeout: 20_000 });
        const before = (await folders.first().getAttribute("data-testid")) || "";
        const icon = folders.first().locator('[data-testid^="detail-folder-icon-"]');
        if ((await icon.count()) > 0) {
          await icon.first().click({ force: true });
        } else {
          await folders.first().dblclick({ force: true });
        }
        await expect
          .poll(
            async () => {
              if ((await page.locator(ITEM_ROWS).count()) > 0) {
                return "item";
              }
              const next = page.locator(FOLDER_ROWS).first();
              if ((await next.count()) === 0) {
                return "pending";
              }
              const id = (await next.getAttribute("data-testid")) || "";
              return id !== before ? "moved" : "pending";
            },
            { timeout: 20_000 },
          )
          .not.toBe("pending");
      }
      expect(foundItem, "H2 Explorer has no selectable page or asset").toBe(true);
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const blocked = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(blocked).toHaveAttribute("data-kind", "error");
      const reason = await blocked.getAttribute("data-reason");
      expect(["page", "asset", "not-folder"]).toContain(reason);
      await expect(blocked).not.toContainText("Allowed publish sites saved");
      await expect(blocked).not.toContainText("Allowed publish sites cleared");
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveCount(0);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: multi-select does not call the server",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      let posted = false;
      const jsErrors = await openExplorer(page);
      const folderRow = await showFolderRows(page);
      await expect
        .poll(async () => folderRow.count(), { timeout: 20_000 })
        .toBeGreaterThan(1);
      await stubAllowedSites(page, {
        onSave: () => {
          posted = true;
        },
      });
      await folderRow.nth(0).locator('input[type="checkbox"]').check();
      await folderRow.nth(1).locator('input[type="checkbox"]').check();
      await expect(folderRow.nth(0).locator('input[type="checkbox"]')).toBeChecked();
      await expect(folderRow.nth(1).locator('input[type="checkbox"]')).toBeChecked();
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "multi");
      await expect(page.locator(`[data-testid="${TEST_IDS.dialog}"]`)).toHaveCount(0);
      expect(posted).toBe(false);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: HTTP 400, 403, and 409 stay on the dialog",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      await selectFirstFolder(page);
      let saveStatus = 400;
      await page.route("**/pathmanagement/path/folderAllowedSitesCatalog**", (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: catalogBody(),
        }),
      );
      await page.route("**/pathmanagement/path/folderProperties/**", (route) => {
        if (route.request().method() !== "GET") {
          return route.continue();
        }
        return route.fulfill({
          status: 200,
          contentType: "application/json",
          body: propertiesBody("301"),
        });
      });
      await page.route("**/pathmanagement/path/saveFolderProperties**", (route) =>
        route.fulfill({
          status: saveStatus,
          contentType: "application/json",
          body: "{}",
        }),
      );
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      for (const status of [400, 403, 409]) {
        saveStatus = status;
        await page.locator(`[data-testid="${TEST_IDS.site302}"]`).check();
        await page.locator(`[data-testid="${TEST_IDS.save}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.dialogError}"]`)).toContainText(
          String(status),
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.status}"]`)).toHaveCount(0);
      }
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
