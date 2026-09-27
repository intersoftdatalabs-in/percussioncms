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
 * Explorer IA Relationships: remove every owned non-folder relationship (#4988).
 *
 * Run from modules/perc-qa-automation/frontend after perc-devctl qa-up:
 * npm run test:surface -- --path tests/explorer-remove-all-relationships.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const OWNED = [
  {
    relationshipId: 4242,
    configName: "Translation",
    category: "rs_translation",
    dependentId: 9,
    label: "Translation -> 9",
  },
  {
    relationshipId: 4243,
    configName: "Active Assembly",
    category: "rs_aa",
    dependentId: 4,
    label: "AA -> 4",
  },
];

const FOLDER = {
  relationshipId: 4244,
  configName: "Folder",
  category: "rs_folder",
  dependentId: 1,
  label: "Folder membership",
};

function attachPageErrors(page) {
  const errors = [];
  page.on("pageerror", (err) => {
    errors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      errors.push(msg.text());
    }
  });
  return errors;
}

function unexpectedJsErrors(errors) {
  return (errors || []).filter(
    (t) =>
      !/ResizeObserver/i.test(t) &&
      !/Download the React DevTools/i.test(t) &&
      !/favicon/i.test(t) &&
      !/Failed to load resource/i.test(t),
  );
}

async function listWaitReady(page) {
  await page.locator('[data-testid="detail-list"]').waitFor({ timeout: 15_000 });
}

async function selectFirstContentItemOnce(page) {
  const root = page.locator(
    '[data-testid="explorer-tree"] [data-testid="tree-node-/Sites/"], [data-testid="explorer-tree"] [data-testid="tree-node-/Sites"]',
  );
  await expect(root.first()).toBeVisible({ timeout: 15_000 });
  await root.first().click();
  await listWaitReady(page);
  for (let remaining = 4; remaining >= 0; remaining -= 1) {
    const itemRow = page.locator(
      '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])',
    );
    if ((await itemRow.count()) > 0) {
      await itemRow.first().click({ force: true, timeout: 10_000 });
      return true;
    }
    if (remaining === 0) return false;
    const folderRow = page.locator(
      '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
    );
    if ((await folderRow.count()) === 0) return false;
    await folderRow.first().dblclick({ force: true, timeout: 10_000 });
    await listWaitReady(page);
  }
  return false;
}

async function selectFirstContentItem(page) {
  for (let attempt = 0; attempt < 2; attempt += 1) {
    if (await selectFirstContentItemOnce(page)) return true;
  }
  return false;
}

async function openRelationships(page) {
  await page.locator('[data-testid="explorer-menu-view"]').click();
  await page.locator('[data-testid="explorer-toggle-relationships"]').click();
}

test.describe("Explorer remove every owned relationship (#4988)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test("confirm removes owned rows; cancel, folder, and 409 do not claim success", async ({
    page,
  }) => {
    const jsErrors = attachPageErrors(page);
    let items = [...OWNED, FOLDER];
    let mode = "ok";
    const deletes = [];
    await page.route("**/content-explorer/relationships/*/edges**", async (route) => {
      const req = route.request();
      if (req.method() === "DELETE") {
        deletes.push(req.url());
        if (mode === "conflict" && req.url().includes("/edges/4243")) {
          await route.fulfill({
            status: 409,
            contentType: "text/plain",
            body: "cannot",
          });
          return;
        }
        const id = Number(req.url().split("/").pop());
        items = items.filter((row) => row.relationshipId !== id);
        await route.fulfill({ status: 204, body: "" });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ items }),
      });
    });

    expect(await selectFirstContentItem(page)).toBe(true);
    await openRelationships(page);
    await expect(page.locator('[data-testid="relationships-edge-4242"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="relationships-folder-4244"]')).toBeVisible();

    await page.locator('[data-testid="relationships-remove-all"]').click();
    await page.locator('[data-testid="relationships-remove-all-cancel"]').click();
    expect(deletes).toEqual([]);
    await expect(page.locator('[data-testid="relationships-edge-4242"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-removed-all"]')).toHaveCount(0);

    mode = "conflict";
    await page.locator('[data-testid="relationships-remove-all"]').click();
    await page.locator('[data-testid="relationships-remove-all-confirm"]').click();
    await expect(page.locator('[data-testid="relationships-remove-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-removed-all"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-edge-4243"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-folder-4244"]')).toBeVisible();

    mode = "ok";
    deletes.length = 0;
    await page.locator('[data-testid="relationships-remove-all"]').click();
    await page.locator('[data-testid="relationships-remove-all-confirm"]').click();
    await expect(page.locator('[data-testid="relationships-removed-all"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-edge-4242"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-edge-4243"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-folder-4244"]')).toBeVisible();
    expect(deletes.some((url) => url.endsWith("/4244"))).toBe(false);
    expect(unexpectedJsErrors(jsErrors)).toEqual([]);
  });
});
