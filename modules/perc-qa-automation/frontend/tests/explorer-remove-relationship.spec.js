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
 * Explorer IA Relationships: remove one relationship from the selected item (#4969).
 *
 * Run from modules/perc-qa-automation/frontend after perc-devctl qa-up:
 * npm run test:surface -- --path tests/explorer-remove-relationship.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const EDGE = {
  relationshipId: 4242,
  configName: "Translation",
  category: "rs_translation",
  dependentId: 9,
  label: "Translation -> 9",
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

async function selectFirstContentItem(page) {
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

async function openRelationships(page) {
  await page.locator('[data-testid="explorer-menu-view"]').click();
  await page.locator('[data-testid="explorer-toggle-relationships"]').click();
}

test.describe("Explorer remove one relationship (#4969)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test("live edges endpoint answers for the selected item", async ({ page }) => {
    const jsErrors = attachPageErrors(page);
    const opened = await selectFirstContentItem(page);
    expect(opened).toBe(true);
    const edgesResponse = page.waitForResponse(
      (res) =>
        res.url().includes("/content-explorer/relationships/") &&
        res.url().includes("/edges") &&
        res.request().method() === "GET",
      { timeout: 20_000 },
    );
    await openRelationships(page);
    const response = await edgesResponse;
    expect(response.status()).toBe(200);
    await expect(page.locator('[data-testid="relationships-view"]')).toBeVisible();
    expect(unexpectedJsErrors(jsErrors)).toEqual([]);
  });

  test("confirm removes the stubbed relationship; cancel and 409 do not", async ({
    page,
  }) => {
    const jsErrors = attachPageErrors(page);
    let items = [EDGE];
    let mode = "ok";
    const deletes = [];
    await page.route("**/content-explorer/relationships/*/edges**", async (route) => {
      const req = route.request();
      if (req.method() === "DELETE") {
        deletes.push(req.url());
        if (mode === "conflict") {
          await route.fulfill({
            status: 409,
            contentType: "text/plain",
            body: "cannot",
          });
          return;
        }
        items = [];
        await route.fulfill({ status: 204, body: "" });
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ items }),
      });
    });

    const opened = await selectFirstContentItem(page);
    expect(opened).toBe(true);
    await openRelationships(page);
    const row = page.locator('[data-testid="relationships-edge-4242"]');
    await expect(row).toBeVisible({ timeout: 15_000 });

    await page.locator('[data-testid="relationships-remove-4242"]').click();
    await page.locator('[data-testid="relationships-remove-cancel"]').click();
    await expect(row).toBeVisible();
    expect(deletes).toEqual([]);
    await expect(page.locator('[data-testid="relationships-removed"]')).toHaveCount(0);

    mode = "conflict";
    await page.locator('[data-testid="relationships-remove-4242"]').click();
    await page.locator('[data-testid="relationships-remove-confirm"]').click();
    await expect(page.locator('[data-testid="relationships-remove-error"]')).toBeVisible();
    await expect(row).toBeVisible();
    await expect(page.locator('[data-testid="relationships-removed"]')).toHaveCount(0);

    mode = "ok";
    await page.locator('[data-testid="relationships-remove-4242"]').click();
    await page.locator('[data-testid="relationships-remove-confirm"]').click();
    await expect(page.locator('[data-testid="relationships-removed"]')).toBeVisible();
    await expect(row).toHaveCount(0);
    expect(unexpectedJsErrors(jsErrors)).toEqual([]);
  });

  test("a folder selection does not claim a relationship was removed", async ({ page }) => {
    const root = page.locator(
      '[data-testid="explorer-tree"] [data-testid="tree-node-/Sites/"], [data-testid="explorer-tree"] [data-testid="tree-node-/Sites"]',
    );
    await expect(root.first()).toBeVisible({ timeout: 15_000 });
    await root.first().click();
    await listWaitReady(page);
    await openRelationships(page);
    await expect(page.locator('[data-testid="explorer-relationships-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-removed"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-view"]')).toHaveCount(0);
  });
});
