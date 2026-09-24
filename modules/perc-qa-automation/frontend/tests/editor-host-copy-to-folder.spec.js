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
 * React Content Editor host — copy the open item into a chosen folder (#4793).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-copy-to-folder.spec.js}</p>
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
  expectedCopiedItemNames,
} = require("./helpers/explorer-copy-item");
const {
  TEST_IDS,
  editorSpaUrl,
  numericContentId,
  isFoldersCopyItemUrl,
} = require("./helpers/editor-host-copy-folder");

const TAGS = ["@explorer-content-editor", "@editor"];

/**
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

test.describe("React Content Editor copy to folder (#4793)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "edit mode copies the item into the chosen folder and stays on the original id",
    { tag: TAGS },
    async ({ page, request }) => {
      const headers = adminBasicAuthHeaders();
      const stamp = uniqueCopyItemName("edcp");
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
      const copyPosts = [];
      page.on("request", (req) => {
        if (req.method() === "POST" && isFoldersCopyItemUrl(req.url())) {
          copyPosts.push(req.url());
        }
      });
      try {
        await page.goto(
          editorSpaUrl(BASE_URL, `contentId=${encodeURIComponent(contentId)}&mode=edit`),
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.copyToFolder}"]`)).toBeVisible({
          timeout: 30_000,
        });
        await page.locator(`[data-testid="${TEST_IDS.copyToFolder}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.destInput}"]`)).toBeVisible();
        await page.locator(`[data-testid="${TEST_IDS.destInput}"]`).fill(dest.path);
        await page.locator(`[data-testid="${TEST_IDS.destOk}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.copyDone}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await expect(page.locator(`[data-testid="${TEST_IDS.copyDone}"]`)).toContainText(
          dest.path,
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText(
          contentId,
        );
        expect(copyPosts.length).toBeGreaterThan(0);
        const sourceKids = await listFolderChildren(
          request,
          BASE_URL,
          headers,
          source.path,
        );
        const destKids = await listFolderChildren(request, BASE_URL, headers, dest.path);
        expect(childNamed(sourceKids, asset.name)).toBe(true);
        const copiedNames = expectedCopiedItemNames(asset.name);
        const destHit = copiedNames.some((name) => childNamed(destKids, name));
        expect(
          destHit,
          `dest children missing copy of ${asset.name}: ${JSON.stringify(destKids)}`,
        ).toBe(true);
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
    "cancel does not copy; view mode has no control; missing folder is not success",
    { tag: TAGS },
    async ({ page, request }) => {
      const headers = adminBasicAuthHeaders();
      const stamp = uniqueCopyItemName("edcpc");
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
      const copyPosts = [];
      page.on("request", (req) => {
        if (req.method() === "POST" && isFoldersCopyItemUrl(req.url())) {
          copyPosts.push(req.url());
        }
      });
      try {
        await page.goto(
          editorSpaUrl(BASE_URL, `contentId=${encodeURIComponent(contentId)}&mode=view`),
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
          timeout: 30_000,
        });
        await expect(page.locator(`[data-testid="${TEST_IDS.copyToFolder}"]`)).toHaveCount(0);

        await page.goto(
          editorSpaUrl(BASE_URL, `contentId=${encodeURIComponent(contentId)}&mode=edit`),
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.copyToFolder}"]`)).toBeVisible({
          timeout: 30_000,
        });
        await page.locator(`[data-testid="${TEST_IDS.copyToFolder}"]`).click();
        await page.locator(`[data-testid="${TEST_IDS.destCancel}"]`).click();
        expect(copyPosts).toEqual([]);
        await expect(page.locator(`[data-testid="${TEST_IDS.copyDone}"]`)).toHaveCount(0);

        await page.locator(`[data-testid="${TEST_IDS.copyToFolder}"]`).click();
        await page
          .locator(`[data-testid="${TEST_IDS.destInput}"]`)
          .fill(`//Sites/${stamp}-missing`);
        await page.locator(`[data-testid="${TEST_IDS.destOk}"]`).click();
        await expect(page.locator(`[data-testid="${TEST_IDS.copyError}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await expect(page.locator(`[data-testid="${TEST_IDS.contentId}"]`)).toContainText(
          contentId,
        );
        await expect(page.locator(`[data-testid="${TEST_IDS.copyDone}"]`)).toHaveCount(0);
      } finally {
        await recycleFolderWithItems(request, BASE_URL, headers, source).catch(() => undefined);
      }
    },
  );
});
