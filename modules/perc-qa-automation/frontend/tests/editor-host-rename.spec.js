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
 * React Content Editor host — rename the open item (#4791 / parent #4532).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-rename.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  editorSpaUrl,
  isFoldersRenameItemUrl,
  isRenameFolderItemRequest,
} = require("./helpers/editor-host-rename");
const {
  assetsFolderUrl,
  uniqueCopyItemName,
  seedDisposableEmptyFolder,
  seedDisposableAsset,
  recycleFolderWithItems,
} = require("./helpers/explorer-rename-item");

const PAGE_TYPE = {
  ContentTypeDetail: {
    name: "percPage",
    fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
  },
};

function fieldsBody(name) {
  return JSON.stringify({
    ItemEditorFields: {
      contentId: "42",
      contentType: "percPage",
      name,
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: name }],
    },
  });
}

function contentIdFromGuid(guid) {
  const match = String(guid || "").match(/(\d+)\s*$/);
  return match ? match[1] : "";
}

async function stubEditor(page, opts) {
  const renameCalls = opts.renameCalls;
  const renameStatus = opts.renameStatus == null ? 200 : opts.renameStatus;
  let renamed = false;
  await page.route("**/services/itemmanagement/workflow/checkOut/**", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: fieldsBody(renamed ? "Home2" : "Home"),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(PAGE_TYPE),
    }),
  );
  await page.route("**/services/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemStateTransition: {
          itemId: "42",
          stateName: "Draft",
          transitionTriggers: [],
        },
      }),
    }),
  );
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: { id: "42", name: "Home", path: "//Assets/Home" },
      }),
    }),
  );
  await page.route("**/rest/folders/rename/item", async (route) => {
    const raw = route.request().postData() || "";
    let body = {};
    try {
      body = JSON.parse(raw);
    } catch {
      body = {};
    }
    renameCalls.push({ url: route.request().url(), body });
    if (renameStatus === 200) {
      renamed = true;
    }
    await route.fulfill({
      status: renameStatus,
      contentType: "application/json",
      body: renameStatus === 200 ? JSON.stringify({ Status: { code: 200 } }) : "{}",
    });
  });
}

test.describe("React Content Editor rename open item", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "edit mode renames and shows the new name after reload",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      const renameCalls = [];
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") {
          return;
        }
        const text = msg.text();
        if (text.startsWith("Failed to load resource:")) {
          return;
        }
        pageErrors.push(text);
      });
      await stubEditor(page, { renameCalls });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.rename}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await page.locator(`[data-testid="${TEST_IDS.renameName}"]`).fill("Home2");
      await page.locator(`[data-testid="${TEST_IDS.rename}"]`).click();
      await expect.poll(() => renameCalls.length).toBe(1);
      expect(isFoldersRenameItemUrl(renameCalls[0].url)).toBe(true);
      expect(isRenameFolderItemRequest(renameCalls[0].body)).toBe(true);
      expect(renameCalls[0].body.RenameFolderItemRequest.itemPath).toBe("//Assets/Home");
      expect(renameCalls[0].body.RenameFolderItemRequest.newName).toBe("Home2");
      await expect(page.locator(`[data-testid="${TEST_IDS.renamed}"]`)).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.title}"]`)).toHaveValue("Home2");
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
      await expect(page.locator(`[data-testid="${TEST_IDS.title}"]`)).toHaveValue("Home2", {
        timeout: 20_000,
      });
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.host}"]`,
      });
    },
  );

  test(
    "view mode hides rename",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      await stubEditor(page, { renameCalls: [] });
      await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=view"));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.rename}"]`)).toHaveCount(0);
    },
  );

  test(
    "HTTP 403 and 404 are visible failures",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      for (const status of [403, 404]) {
        const renameCalls = [];
        await stubEditor(page, { renameCalls, renameStatus: status });
        await page.goto(editorSpaUrl(BASE_URL, "contentId=42&mode=edit"));
        await expect(page.locator(`[data-testid="${TEST_IDS.rename}"]`)).toBeVisible({
          timeout: 20_000,
        });
        await page.locator(`[data-testid="${TEST_IDS.renameName}"]`).fill("Nope");
        await page.locator(`[data-testid="${TEST_IDS.rename}"]`).click();
        await expect.poll(() => renameCalls.length).toBe(1);
        await expect(page.locator(`[data-testid="${TEST_IDS.renameError}"]`)).toBeVisible();
        await expect(page.locator(`[data-testid="${TEST_IDS.renamed}"]`)).toHaveCount(0);
        await expect(page.locator(`[data-testid="${TEST_IDS.title}"]`)).toHaveValue("Home");
      }
    },
  );

  test(
    "H2: persisted rename survives a real reload",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page, request }) => {
      test.setTimeout(120_000);
      const headers = adminBasicAuthHeaders();
      const assets = await request.get(assetsFolderUrl(BASE_URL), { headers });
      expect(assets.status(), "H2 Assets parent").toBe(200);
      const stamp = Date.now();
      const folderName = uniqueCopyItemName("qa4791fld", stamp);
      const itemName = uniqueCopyItemName("qa4791itm", stamp);
      const newName = uniqueCopyItemName("qa4791ren", stamp);
      let liveFolder = null;
      try {
        liveFolder = await seedDisposableEmptyFolder(request, BASE_URL, headers, {
          parentPath: "Assets",
          name: folderName,
        });
        const source = await seedDisposableAsset(request, BASE_URL, headers, {
          parentPath: String(liveFolder.path || `/Assets/${folderName}`),
          name: itemName,
        });
        const contentId = contentIdFromGuid(source.guid);
        expect(contentId, `numeric content id from ${source.guid}`).toMatch(/^\d+$/);
        const pageErrors = [];
        page.on("pageerror", (err) => pageErrors.push(String(err)));
        // Item fields GET is rollback-only for a freshly created asset on this
        // H2 cell. Reflect the persisted path-item name into that read so the
        // host still reloads what rename/item stored.
        await page.route("**/services/itemmanagement/item/fields/**", async (route) => {
          const lookup = await request.get(
            `${BASE_URL}/Rhythmyx/services/pathmanagement/path/item/id/${contentId}`,
            { headers },
          );
          const body = await lookup.json().catch(() => ({}));
          const item = body.PathItem || body.pathItem || {};
          const name = String(item.name || item.Name || itemName);
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({
              ItemEditorFields: {
                contentId,
                contentType: String(source.contentType || "percSimpleTextAsset"),
                name,
                checkoutUser: "admin",
                fields: [{ name: "sys_title", value: name }],
              },
            }),
          });
        });
        await page.route("**/services/contenttypes/**", (route) =>
          route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(PAGE_TYPE),
          }),
        );
        await page.goto(editorSpaUrl(BASE_URL, `contentId=${contentId}&mode=edit`));
        const nameBox = page.locator(`[data-testid="${TEST_IDS.renameName}"]`);
        await expect(nameBox).toBeVisible({ timeout: 30_000 });
        await expect(nameBox).toHaveValue(itemName, { timeout: 20_000 });
        await nameBox.fill(newName);
        const renameResp = page.waitForResponse(
          (res) =>
            isFoldersRenameItemUrl(res.url()) &&
            res.request().method() === "POST",
        );
        await page.locator(`[data-testid="${TEST_IDS.rename}"]`).click();
        const posted = await renameResp;
        expect(posted.status(), await posted.text()).toBe(200);
        await expect(page.locator(`[data-testid="${TEST_IDS.renamed}"]`)).toBeVisible();
        await expect(page.locator(`[data-testid="${TEST_IDS.title}"]`)).toHaveValue(newName);
        await page.goto(editorSpaUrl(BASE_URL, `contentId=${contentId}&mode=edit`));
        await expect(page.locator(`[data-testid="${TEST_IDS.title}"]`)).toHaveValue(newName, {
          timeout: 30_000,
        });
        expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      } finally {
        if (liveFolder && liveFolder.path) {
          await recycleFolderWithItems(request, BASE_URL, headers, liveFolder).catch(
            () => undefined,
          );
        }
      }
    },
  );
});
