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
 * Explorer IA Relationships: create one new item in the selected slot (#5267).
 *
 * Run from modules/perc-qa-automation/frontend after perc-devctl qa-up:
 * npm run test:surface -- --path tests/explorer-create-item-in-slot.spec.js
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
const TYPES = {
  items: [{ id: 3, name: "percPage", label: "Page" }],
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

function pathOf(url) {
  try {
    return new URL(url).pathname;
  } catch {
    return "";
  }
}

function isItemCreatePost(url) {
  return /\/itemmanagement\/item\/create\/?$/.test(pathOf(url));
}

function isSlotAddPost(url) {
  return /\/assembly\/slot-relationships\/?$/.test(pathOf(url));
}

async function fillCreate(page) {
  await page.locator('[data-testid="relationships-create-slot-type"]').selectOption("percPage");
  await page.locator('[data-testid="relationships-create-slot-folder"]').fill("/Sites/Enterprise");
  await page
    .locator('[data-testid="relationships-create-slot-template"]')
    .selectOption("4");
}

test.describe("Explorer create a new item in the selected slot (#5267)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
    await page.goto(
      `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`,
    );
    await page.waitForLoadState("networkidle");
  });

  test("confirm creates one item and opens the editor only after the link; cancel, missing fields, and 400/403/409 do not", async ({
    page,
  }) => {
    const jsErrors = attachPageErrors(page);
    let createMode = "ok";
    let linkMode = "ok";
    const order = [];
    const editorDocs = [];
    await page.context().route(/entry=editor/, async (route) => {
      editorDocs.push(route.request().url());
      await route.fulfill({
        status: 200,
        contentType: "text/html",
        body: "<!doctype html><title>editor</title>",
      });
    });
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
      "**/assembly/slot-relationships/allowed-types**",
      async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(TYPES),
        });
      },
    );
    await page.route(
      (url) => isItemCreatePost(url.href),
      async (route) => {
        if (route.request().method() !== "POST") {
          await route.fallback();
          return;
        }
        const body = route.request().postDataJSON();
        order.push("create");
        if (createMode === "400" || createMode === "403" || createMode === "409") {
          await route.fulfill({
            status: Number(createMode),
            contentType: "text/plain",
            body: "cannot",
          });
          return;
        }
        const fields = body.ItemCreateRequest || body;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            ItemCreateResult: {
              itemId: "1-101-99",
              name: "New page",
              folderPath: fields.folderPath,
              contentType: fields.contentType,
            },
          }),
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
        order.push("link");
        if (linkMode === "409") {
          await route.fulfill({
            status: 409,
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
    await expect(page.locator('[data-testid="relationships-create-slot-done"]')).toHaveCount(0);
    await page.locator('[data-testid="relationships-select-edge-82"]').click();
    await page.locator('[data-testid="relationships-create-in-slot"]').click();
    expect(order).toEqual([]);
    await expect(page.locator('[data-testid="relationships-create-slot-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-create-slot-dialog"]')).toHaveCount(0);
    expect(editorDocs).toEqual([]);

    await page.locator('[data-testid="relationships-create-slot-5"]').click();
    await expect(page.locator('[data-testid="relationships-create-slot-dialog"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="relationships-create-slot-type"] option[value="percPage"]'),
    ).toHaveCount(1);
    await page.locator('[data-testid="relationships-create-slot-confirm"]').click();
    expect(order).toEqual([]);
    await expect(page.locator('[data-testid="relationships-create-slot-done"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-create-slot-error"]')).toBeVisible();

    await page.locator('[data-testid="relationships-create-slot-type"]').selectOption("percPage");
    await page.locator('[data-testid="relationships-create-slot-folder"]').fill("/Sites/Enterprise");
    await page.locator('[data-testid="relationships-create-slot-template"]').selectOption("4");
    await page.locator('[data-testid="relationships-create-slot-cancel"]').click();
    expect(order).toEqual([]);
    await expect(page.locator('[data-testid="relationships-create-slot-dialog"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-create-slot-done"]')).toHaveCount(0);
    expect(await idsInSlot(page, 5)).toEqual(["71", "72"]);
    expect(editorDocs).toEqual([]);

    for (const status of ["400", "403", "409"]) {
      createMode = status;
      linkMode = "ok";
      await page.locator('[data-testid="relationships-create-slot-5"]').click();
      await expect(page.locator('[data-testid="relationships-create-slot-dialog"]')).toBeVisible();
      await expect(
        page.locator('[data-testid="relationships-create-slot-type"] option[value="percPage"]'),
      ).toHaveCount(1);
      await fillCreate(page);
      await page.locator('[data-testid="relationships-create-slot-confirm"]').click();
      await expect(page.locator('[data-testid="relationships-create-slot-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="relationships-create-slot-done"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="relationships-edge-91"]')).toHaveCount(0);
      expect(await idsInSlot(page, 5)).toEqual(["71", "72"]);
      expect(order.filter((step) => step === "link")).toEqual([]);
      expect(editorDocs).toEqual([]);
    }

    createMode = "ok";
    linkMode = "409";
    await page.locator('[data-testid="relationships-create-slot-confirm"]').click();
    await expect(page.locator('[data-testid="relationships-create-slot-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-create-slot-done"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-edge-91"]')).toHaveCount(0);
    expect(order.filter((step) => step === "link")).toEqual(["link"]);
    expect(editorDocs).toEqual([]);

    linkMode = "ok";
    const popupPromise = page.waitForEvent("popup");
    await page.locator('[data-testid="relationships-create-slot-confirm"]').click();
    const popup = await popupPromise;
    await expect(page.locator('[data-testid="relationships-create-slot-done"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-edge-91"]')).toHaveAttribute(
      "data-dependent-id",
      "99",
    );
    await expect(page.locator('[data-testid="relationships-edge-91"]')).toHaveAttribute(
      "data-slot-id",
      "5",
    );
    expect(await idsInSlot(page, 5)).toEqual(["71", "72", "91"]);
    await expect(popup).toHaveURL(/entry=editor/, { timeout: 15_000 });
    await expect(popup).toHaveURL(/contentId=99/);
    expect(order.slice(-2)).toEqual(["create", "link"]);
    expect(order.filter((step) => step === "create").length).toBe(5);
    expect(editorDocs.some((url) => /contentId=99/.test(url))).toBe(true);
    expect(unexpectedJsErrors(jsErrors)).toEqual([]);
    if (!popup.isClosed()) {
      await popup.close();
    }
  });

  test("a folder selection does not claim an item was created in a slot", async ({
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
    await expect(page.locator('[data-testid="relationships-create-slot-done"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="relationships-view"]')).toHaveCount(0);
    expect(unexpectedJsErrors(jsErrors)).toEqual([]);
  });
});
