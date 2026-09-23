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
 * React Content Editor host — move the open item to another folder (#4774).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-move.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { listFolderChildren } = require("./helpers/empty-recycling");
const {
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
  uniqueCopyItemName,
} = require("./helpers/explorer-copy-item");
const {
  TEST_IDS,
  editorSpaUrl,
  numericContentId,
  isFoldersMoveItemUrl,
} = require("./helpers/editor-host-move");

const TAGS = ["@explorer-content-editor", "@editor"];

/**
 * The H2 matrix image may not serve itemmanagement field JSON. Stub only the
 * editor load so Move enables; path lookup and {@code POST /folders/move/item}
 * stay live.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} contentId
 * @param {string} name
 */
async function stubEditorLoad(page, contentId, name) {
  const fields = {
    ItemEditorFields: {
      contentId,
      contentType: "percPage",
      name,
      checkoutUser: "Admin",
      fields: [{ name: "sys_title", value: name }],
    },
  };
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fields),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ContentTypeDetail: {
          name: "percPage",
          fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
        },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: contentId,
          stateName: "Draft",
          transitionTriggers: [],
        },
      }),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        SlotCanvas: { ownerId: Number(contentId) || 0, slots: [] },
      }),
    }),
  );
  await page.route("**/content-explorer/relationships/**/local**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ count: 0, links: [] }),
    }),
  );
}

/**
 * Numeric content ids are not the relationship GUID ({@code host-type-uuid}).
 * Path lookup by the editor's content id 404s for assets on this cell.
 * Return the seeded item path; the move POST stays live.
 *
 * @param {import("@playwright/test").Page} page
 * @param {{ path: string, name: string, guid?: string }} asset
 */
async function stubItemPath(page, asset) {
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: {
          id: asset.guid || "",
          name: asset.name,
          path: asset.path,
          type: "asset",
        },
      }),
    }),
  );
}

function childNamed(children, name) {
  return (children || []).some(
    (row) => String(row.name || row.Name || "").trim() === name,
  );
}

test.describe("React Content Editor move to folder (#4774)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "edit mode moves the item and stays on the same content id",
    { tag: TAGS },
    async ({ page, request }) => {
      const headers = adminBasicAuthHeaders();
      const stamp = uniqueCopyItemName("edmv");
      const source = await seedDisposableEmptyFolder(request, BASE_URL, headers, {
        parentPath: "Assets",
        name: `${stamp}-src`,
      });
      const dest = await seedDisposableEmptyFolder(request, BASE_URL, headers, {
        parentPath: "Assets",
        name: `${stamp}-dst`,
      });
      const asset = await seedDisposableAsset(request, BASE_URL, headers, {
        parentPath: source.path,
        name: `${stamp}-item`,
      });
      const contentId = numericContentId(asset.guid);
      await stubEditorLoad(page, contentId, asset.name);
      await stubItemPath(page, asset);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });

      /** @type {string[]} */
      const movePosts = [];
      page.on("request", (req) => {
        if (req.method() === "POST" && isFoldersMoveItemUrl(req.url())) {
          movePosts.push(req.url());
        }
      });
      try {
        await page.goto(
          editorSpaUrl(BASE_URL, `contentId=${encodeURIComponent(contentId)}&mode=edit`),
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.move}"]`)).toBeVisible({
          timeout: 30_000,
        });
        await page.locator(`[data-testid="${TEST_IDS.move}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.destInput}"]`)).toBeVisible();
        await page.locator(`[data-testid="${TEST_IDS.destInput}"]`).fill(dest.path);
        await page.locator(`[data-testid="${TEST_IDS.destOk}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.moveDone}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText(
          contentId,
        );
        expect(movePosts.length).toBeGreaterThan(0);
        const sourceKids = await listFolderChildren(
          request,
          BASE_URL,
          headers,
          source.path,
        );
        const destKids = await listFolderChildren(request, BASE_URL, headers, dest.path);
        expect(childNamed(sourceKids, asset.name)).toBe(false);
        expect(childNamed(destKids, asset.name)).toBe(true);
        expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${TEST_IDS.host}"]`,
        });
      } finally {
        await recycleFolderWithItems(request, BASE_URL, headers, source).catch(() => undefined);
        await recycleFolderWithItems(request, BASE_URL, headers, dest).catch(() => undefined);
      }
    },
  );

  test(
    "cancel does not move; view mode has no move control; missing folder is 404",
    { tag: TAGS },
    async ({ page, request }) => {
      const headers = adminBasicAuthHeaders();
      const stamp = uniqueCopyItemName("edmvc");
      const source = await seedDisposableEmptyFolder(request, BASE_URL, headers, {
        parentPath: "Assets",
        name: `${stamp}-src`,
      });
      const asset = await seedDisposableAsset(request, BASE_URL, headers, {
        parentPath: source.path,
        name: `${stamp}-item`,
      });
      const contentId = numericContentId(asset.guid);
      await stubEditorLoad(page, contentId, asset.name);
      await stubItemPath(page, asset);
      /** @type {string[]} */
      const movePosts = [];
      page.on("request", (req) => {
        if (req.method() === "POST" && isFoldersMoveItemUrl(req.url())) {
          movePosts.push(req.url());
        }
      });
      try {
        await page.goto(
          editorSpaUrl(BASE_URL, `contentId=${encodeURIComponent(contentId)}&mode=view`),
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
          timeout: 30_000,
        });
        await expect(page.locator(`[data-testid="${TEST_IDS.move}"]`)).toHaveCount(0);

        await page.goto(
          editorSpaUrl(BASE_URL, `contentId=${encodeURIComponent(contentId)}&mode=edit`),
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.move}"]`)).toBeVisible({
          timeout: 30_000,
        });
        await page.locator(`[data-testid="${TEST_IDS.move}"]`).click();
        await page.locator(`[data-testid="${TEST_IDS.destCancel}"]`).click();
        expect(movePosts).toEqual([]);
        await expect(page.locator(`[data-testid="${TEST_IDS.moveDone}"]`)).toHaveCount(0);

        await page.locator(`[data-testid="${TEST_IDS.move}"]`).click();
        await page
          .locator(`[data-testid="${TEST_IDS.destInput}"]`)
          .fill(`//Sites/${stamp}-missing`);
        await page.locator(`[data-testid="${TEST_IDS.destOk}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.moveError}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText(
          contentId,
        );
        const sourceKids = await listFolderChildren(
          request,
          BASE_URL,
          headers,
          source.path,
        );
        expect(childNamed(sourceKids, asset.name)).toBe(true);
      } finally {
        await recycleFolderWithItems(request, BASE_URL, headers, source).catch(() => undefined);
      }
    },
  );
});
