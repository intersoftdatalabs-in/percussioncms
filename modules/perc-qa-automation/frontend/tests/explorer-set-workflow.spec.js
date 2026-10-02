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

async function listReady(page) {
  await page.locator(`[data-testid="${TEST_IDS.detailList}"]`).waitFor({ timeout: 20_000 });
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
    await icon.first().click();
  } else {
    await folderRow.first().dblclick({ force: true });
  }
  await listReady(page);
  return true;
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
    await root.first().click();
    await listReady(page);
    await page
      .locator('[data-testid="detail-list"] tbody tr')
      .first()
      .waitFor({ timeout: 20_000 })
      .catch(() => {});
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
