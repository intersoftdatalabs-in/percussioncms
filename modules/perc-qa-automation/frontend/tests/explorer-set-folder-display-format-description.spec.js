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
 * Playwright: #5840 / parent #4530 — Content → Set folder display format
 * shows the catalog description for the selected format.
 *
 * Tags: @explorer-set-folder-display-format-description @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-set-folder-display-format-description.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { openContentMenu } = require("./helpers/explorer-sites-list-create");
const {
  TEST_IDS,
  explorerSetFolderDisplayFormatUrl,
  isKnownExplorerSetFolderDisplayFormatConsoleNoise,
} = require("./helpers/explorer-set-folder-display-format");

const TAGS = ["@explorer-set-folder-display-format-description", "@explorer", "@smoke"];
const DESCRIPTION_TEST_ID = "explorer-set-folder-display-format-description";

const FOLDER_ROWS =
  '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])';

function catalogBody() {
  return JSON.stringify({
    FolderDisplayFormatCatalog: {
      choices: [
        { id: "3", name: "Default" },
        { id: "12", name: "Simple", description: "One row per page." },
      ],
    },
  });
}

function propertiesBody(displayFormatId, displayFormatName) {
  return JSON.stringify({
    FolderProperties: {
      id: "16777215-101-703",
      name: "CI",
      displayFormatId,
      displayFormatName,
      permission: { accessLevel: "ADMIN" },
    },
  });
}

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (
      msg.type() === "error" &&
      !isKnownExplorerSetFolderDisplayFormatConsoleNoise(msg.text())
    ) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerSetFolderDisplayFormatUrl(BASE_URL), {
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

async function selectFirstFolder(page) {
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
  const row = folderRow.first();
  const id = (await row.getAttribute("data-item-id")) || "";
  await row.locator('[data-testid^="detail-cell-"]').last().click();
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toHaveAttribute(
    "data-selected-item-id",
    id,
    { timeout: 10_000 },
  );
}

async function stubCatalog(page) {
  await page.route("**/pathmanagement/path/folderDisplayFormatCatalog**", (route) =>
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
      body: propertiesBody("3", "Default"),
    });
  });
}

test.describe("Explorer set folder display format description (#5840 / #4530)", () => {
  test(
    "UI: dialog shows the selected format description and hides the line when absent",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      await selectFirstFolder(page);
      await stubCatalog(page);
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.menuItem}"]`).click();
      const dialog = page.locator(`[data-testid="${TEST_IDS.dialog}"]`);
      await expect(dialog).toBeVisible();
      const description = page.locator(`[data-testid="${DESCRIPTION_TEST_ID}"]`);
      // Initial selection is the first non-current choice ("12"), which is described.
      await expect(description).toBeVisible();
      await expect(description).toHaveText("One row per page.");
      await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("3");
      await expect(description).toHaveCount(0);
      await page.locator(`[data-testid="${TEST_IDS.select}"]`).selectOption("12");
      await expect(description).toBeVisible();
      await expect(description).toHaveText("One row per page.");
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
      await selectFirstFolder(page);
      await stubCatalog(page);
      await page.route("**/pathmanagement/path/saveFolderProperties**", (route) => {
        posted = true;
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
      await expect(
        page.locator(`[data-testid="${DESCRIPTION_TEST_ID}"]`),
      ).toBeVisible();
      await page.locator(`[data-testid="${TEST_IDS.cancel}"]`).click();
      await expect(dialog).toHaveCount(0);
      expect(posted).toBe(false);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
