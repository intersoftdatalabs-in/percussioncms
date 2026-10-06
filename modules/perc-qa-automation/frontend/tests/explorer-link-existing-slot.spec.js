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
 * Explorer IA Relationships: link one existing item into the selected slot (#5266).
 *
 * Run from modules/perc-qa-automation/frontend after perc-devctl qa-up:
 * npm run test:surface -- --path tests/explorer-link-existing-slot.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { pickContentFolderIndex } = require("./helpers/explorer-new-copy");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const AA_FIRST = {
  relationshipId: 71,
  configName: "ActiveAssembly",
  category: "rs_activeassembly",
  dependentId: 4,
  label: "AA first",
  slotId: 5,
  sortRank: 0,
  templateId: 4,
  templateName: "Brief",
};
const AA_SIBLING = {
  relationshipId: 72,
  configName: "ActiveAssembly",
  category: "rs_activeassembly",
  dependentId: 5,
  label: "AA last",
  slotId: 5,
  sortRank: 1,
  templateId: 4,
  templateName: "Brief",
};
const FOLDER = {
  relationshipId: 82,
  configName: "Folder",
  category: "rs_folder",
  dependentId: 3,
  label: "Folder row",
  slotId: 5,
  templateId: 4,
  templateName: "Brief",
};
const TRANSLATION = {
  relationshipId: 73,
  configName: "Translation",
  category: "rs_translation",
  dependentId: 9,
  label: "Translation stays",
};
const TEMPLATES = {
  items: [
    { id: 4, name: "brief", label: "Brief" },
    { id: 8, name: "full", label: "Full story" },
  ],
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

async function idsInSlot(page, slotId) {
  const group = page.locator(
    `[data-testid="relationships-slot-group-${slotId}"]`,
  );
  if ((await group.count()) === 0) {
    return [];
  }
  return group
    .locator('[data-testid^="relationships-edge-"]')
    .evaluateAll((els) =>
      els.map((el) =>
        (el.getAttribute("data-testid") || "").replace("relationships-edge-", ""),
      ),
    );
}

function isSlotAddPost(url) {
  try {
    const path = new URL(url).pathname;
    return /\/assembly\/slot-relationships\/?$/.test(path);
  } catch {
    return false;
  }
}

test.describe("Explorer link an existing item into the selected slot (#5266)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test("confirm links one existing item; cancel, blank, folder, and 400/403/409 do not", async ({
    page,
  }) => {
    const jsErrors = attachPageErrors(page);
    let mode = "ok";
    const posts = [];
    await page.route("**/content-explorer/relationships/*/edges**", async (route) => {
      if (route.request().method() !== "GET") {
        await route.fallback();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          items: [AA_FIRST, AA_SIBLING, FOLDER, TRANSLATION],
        }),
      });
    });
    await page.route(
      "**/assembly/slot-relationships/allowed-templates**",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(TEMPLATES),
        });
      },
    );
    await page.route(
      (url) => isSlotAddPost(url.href),
      async (route) => {
        if (route.request().method() !== "POST") {
          await route.fallback();
          return;
        }
        const body = route.request().postDataJSON();
        posts.push({ url: route.request().url(), body });
        if (mode === "400" || mode === "403" || mode === "409") {
          await route.fulfill({
            status: Number(mode),
            contentType: "text/plain",
            body: "cannot",
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            relationshipId: 91,
            ownerId: body.ownerId,
            dependentId: body.dependentId,
            slotId: body.slotId,
            templateId: body.templateId,
            sortRank: 2,
          }),
        });
      },
    );

    const opened = await selectFirstContentItem(page);
    expect(opened).toBe(true);
    await openRelationships(page);
    await expect(page.locator('[data-testid="relationships-edge-71"]')).toBeVisible({
      timeout: 15_000,
    });
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="content-explorer-shell"]',
    });
    expect(await idsInSlot(page, 5)).toEqual(["71", "72"]);
    await expect(page.locator('[data-testid="relationships-link-slot-done"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-select-edge-82"]')).toBeVisible();
    await page.locator('[data-testid="relationships-select-edge-82"]').click();
    await page.locator('[data-testid="relationships-link-existing"]').click();
    expect(posts).toEqual([]);
    await expect(page.locator('[data-testid="relationships-link-slot-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-link-slot-dialog"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-link-slot-done"]')).toHaveCount(0);
    expect(await idsInSlot(page, 5)).toEqual(["71", "72"]);

    await page.locator('[data-testid="relationships-link-slot-5"]').click();
    await expect(page.locator('[data-testid="relationships-link-slot-dialog"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="relationships-link-slot-template"] option[value="4"]'),
    ).toHaveCount(1);
    await page.locator('[data-testid="relationships-link-slot-confirm"]').click();
    expect(posts).toEqual([]);
    await expect(page.locator('[data-testid="relationships-link-slot-done"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-link-slot-error"]')).toBeVisible();
    expect(await idsInSlot(page, 5)).toEqual(["71", "72"]);

    await page.locator('[data-testid="relationships-link-slot-target"]').fill("88");
    await page.locator('[data-testid="relationships-link-slot-template"]').selectOption("4");
    await page.locator('[data-testid="relationships-link-slot-cancel"]').click();
    expect(posts).toEqual([]);
    await expect(page.locator('[data-testid="relationships-link-slot-dialog"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-link-slot-done"]')).toHaveCount(0);
    expect(await idsInSlot(page, 5)).toEqual(["71", "72"]);

    for (const status of ["400", "403", "409"]) {
      mode = status;
      await page.locator('[data-testid="relationships-link-slot-5"]').click();
      await expect(page.locator('[data-testid="relationships-link-slot-dialog"]')).toBeVisible();
      await page.locator('[data-testid="relationships-link-slot-target"]').fill("88");
      await page.locator('[data-testid="relationships-link-slot-template"]').selectOption("8");
      await page.locator('[data-testid="relationships-link-slot-confirm"]').click();
      await expect(page.locator('[data-testid="relationships-link-slot-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="relationships-link-slot-done"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="relationships-edge-91"]')).toHaveCount(0);
      expect(await idsInSlot(page, 5)).toEqual(["71", "72"]);
    }

    mode = "ok";
    await page.locator('[data-testid="relationships-link-slot-5"]').click();
    await page.locator('[data-testid="relationships-link-slot-target"]').fill("88");
    await page.locator('[data-testid="relationships-link-slot-template"]').selectOption("4");
    await page.locator('[data-testid="relationships-link-slot-confirm"]').click();
    await expect(page.locator('[data-testid="relationships-link-slot-done"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-edge-91"]')).toHaveAttribute(
      "data-dependent-id",
      "88",
    );
    await expect(page.locator('[data-testid="relationships-edge-91"]')).toHaveAttribute(
      "data-slot-id",
      "5",
    );
    expect(await idsInSlot(page, 5)).toEqual(["71", "72", "91"]);
    expect(posts).toHaveLength(4);
    expect(posts[posts.length - 1].body.dependentId).toBe(88);
    expect(posts[posts.length - 1].body.slotId).toBe(5);
    expect(posts[posts.length - 1].body.templateId).toBe(4);
    expect(posts[posts.length - 1].url).toMatch(/\/slot-relationships\/?$/);
    expect(posts.every((post) => !String(post.url).includes("/edges"))).toBe(true);
    expect(unexpectedJsErrors(jsErrors)).toEqual([]);
  });

  test("a folder selection does not claim an item was linked into a slot", async ({
    page,
  }) => {
    const jsErrors = attachPageErrors(page);
    const root = page.locator(
      '[data-testid="explorer-tree"] [data-testid="tree-node-/Sites/"], [data-testid="explorer-tree"] [data-testid="tree-node-/Sites"]',
    );
    await expect(root.first()).toBeVisible({ timeout: 15_000 });
    await root.first().click();
    await listWaitReady(page);
    await openRelationships(page);
    await expect(page.locator('[data-testid="explorer-relationships-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-link-slot-done"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-view"]')).toHaveCount(0);
    expect(unexpectedJsErrors(jsErrors)).toEqual([]);
  });
});
