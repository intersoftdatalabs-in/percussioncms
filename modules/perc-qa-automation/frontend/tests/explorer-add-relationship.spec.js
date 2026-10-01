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
 * Explorer IA Relationships: add one owned relationship (#5036).
 *
 * Run from modules/perc-qa-automation/frontend after perc-devctl qa-up:
 * npm run test:surface -- --path tests/explorer-add-relationship.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { pickContentFolderIndex } = require("./helpers/explorer-new-copy");

const CREATED = {
  relationshipId: 5151,
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

async function detailRowMeta(rows) {
  return rows.evaluateAll((els) =>
    els.map((el) => ({
      id: el.getAttribute("data-testid") || "",
      name: (el.getAttribute("data-item-name") || "").trim(),
    })),
  );
}

async function detailSignature(page) {
  const rows = page.locator(
    '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"]',
  );
  const meta = await detailRowMeta(rows);
  return meta.map((row) => row.id).join("|");
}

/**
 * Folder open does not replace the detail list synchronously. Wait until
 * the row ids change so the walk is not scored against the parent listing
 * (#5048, same failure mode as #5028).
 */
async function openDetailFolder(page, row, beforeSignature) {
  const icon = row.locator('[data-testid^="detail-folder-icon-"]');
  if ((await icon.count()) > 0) {
    await icon.first().click({ force: true });
  } else {
    await row.dblclick({ force: true });
  }
  await expect
    .poll(async () => detailSignature(page), { timeout: 15_000 })
    .not.toBe(beforeSignature);
  await listWaitReady(page);
}

async function selectFirstContentItem(page) {
  const root = page.locator(
    '[data-testid="explorer-tree"] [data-testid="tree-node-/Sites/"], [data-testid="explorer-tree"] [data-testid="tree-node-/Sites"]',
  );
  await expect(root.first()).toBeVisible({ timeout: 15_000 });
  await root.first().click({ force: true });
  await listWaitReady(page);
  const list = page.locator('[data-testid="detail-list"]');
  const seenFolderIds = new Set();
  for (let depth = 0; depth < 8; depth += 1) {
    const itemRow = list.locator(
      'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"]):not([data-item-type="site"])',
    );
    const folders = list.locator(
      'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
    );
    try {
      await expect
        .poll(async () => (await itemRow.count()) + (await folders.count()), {
          timeout: 20_000,
        })
        .toBeGreaterThan(0);
    } catch {
      return false;
    }
    if ((await itemRow.count()) > 0) {
      await itemRow.first().click({ force: true, timeout: 10_000 });
      return true;
    }
    const folderMeta = await detailRowMeta(folders);
    const chosen = pickContentFolderIndex(folderMeta, "page", seenFolderIds);
    if (chosen < 0) {
      return false;
    }
    const id = folderMeta[chosen].id;
    if (id) seenFolderIds.add(id);
    const beforeSignature = await detailSignature(page);
    await openDetailFolder(page, folders.nth(chosen), beforeSignature);
  }
  return false;
}

async function openRelationships(page) {
  await page.locator('[data-testid="explorer-menu-view"]').click();
  await page.locator('[data-testid="explorer-toggle-relationships"]').click();
}

test.describe("Explorer add one relationship (#5036)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test("confirm adds the stubbed relationship; cancel and 409 do not", async ({
    page,
  }) => {
    const jsErrors = attachPageErrors(page);
    let items = [];
    let mode = "ok";
    const posts = [];
    await page.route("**/content-explorer/relationships/*/edges**", async (route) => {
      const req = route.request();
      if (req.method() === "POST") {
        posts.push(req.url());
        if (mode === "conflict") {
          await route.fulfill({
            status: 409,
            contentType: "text/plain",
            body: "cannot",
          });
          return;
        }
        items = [CREATED];
        await route.fulfill({
          status: 201,
          contentType: "application/json",
          body: JSON.stringify(CREATED),
        });
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
    await expect(page.locator('[data-testid="relationships-add"]')).toBeVisible({
      timeout: 15_000,
    });

    await page.locator('[data-testid="relationships-add"]').click();
    await page.locator('[data-testid="relationships-add-target"]').fill("9");
    await page.locator('[data-testid="relationships-add-cancel"]').click();
    expect(posts).toEqual([]);
    await expect(page.locator('[data-testid="relationships-added"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-edge-5151"]')).toHaveCount(0);

    mode = "conflict";
    await page.locator('[data-testid="relationships-add"]').click();
    await page.locator('[data-testid="relationships-add-target"]').fill("9");
    await page.locator('[data-testid="relationships-add-confirm"]').click();
    await expect(page.locator('[data-testid="relationships-add-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-added"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-edge-5151"]')).toHaveCount(0);

    mode = "ok";
    await page.locator('[data-testid="relationships-add"]').click();
    await page.locator('[data-testid="relationships-add-target"]').fill("9");
    await page.locator('[data-testid="relationships-add-confirm"]').click();
    await expect(page.locator('[data-testid="relationships-added"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-edge-5151"]')).toBeVisible();
    expect(unexpectedJsErrors(jsErrors)).toEqual([]);
  });

  test("a folder selection does not claim a relationship was added", async ({ page }) => {
    const root = page.locator(
      '[data-testid="explorer-tree"] [data-testid="tree-node-/Sites/"], [data-testid="explorer-tree"] [data-testid="tree-node-/Sites"]',
    );
    await expect(root.first()).toBeVisible({ timeout: 15_000 });
    await root.first().click();
    await listWaitReady(page);
    await openRelationships(page);
    await expect(page.locator('[data-testid="explorer-relationships-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-added"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-view"]')).toHaveCount(0);
  });
});
