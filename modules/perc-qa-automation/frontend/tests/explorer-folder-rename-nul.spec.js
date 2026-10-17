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
 * Playwright surface: #5478 / parent #4530 — Explorer folder Rename refuses
 * a NUL (U+0000) on {@code spa.jsp?entry=explorer} without
 * {@code rxFolderMutations=1}. The dialog stays open and the tree keeps the
 * previous name. A normal name still renames only after HTTP 200.
 *
 * <p>Peer: {@code tests/explorer-rename-folder.spec.js}.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-folder-rename-nul.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { recycleFolder } = require("./helpers/folder-recycle-smoke");
const {
  RENAME_TEST_IDS,
  explorerProductRenameFolderUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  hasRxFolderMutationsQuery,
  isPathmanagementRenameFolderUrl,
  isRenameFolderSuccessStatus,
  uniqueRenameFolderName,
  unwrapPathItem,
  wrapRenameFolderItem,
  treeRootLocator,
  sitesTreeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-rename-folder");
const {
  expandExplorerTreeNode,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-folder-rename-nul", "@explorer", "@folder"];
const NUL_NAME = `bad${String.fromCharCode(0)}name`;

/**
 * Controlled input update, including U+0000. Code points cross the Playwright
 * bridge as numbers so a NUL is not dropped by JSON transport.
 *
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setInputValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload);
    const proto = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype,
      "value",
    );
    if (!proto || typeof proto.set !== "function") {
      throw new Error("input value setter missing");
    }
    const previous = el.value;
    proto.set.call(el, next);
    const tracker = el._valueTracker;
    if (tracker) {
      tracker.setValue(previous);
    }
    el.dispatchEvent(new Event("input", { bubbles: true }));
  }, codes);
}

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} url
 * @returns {Promise<number>}
 */
async function getStatus(request, url) {
  const res = await request.get(url, { headers: adminBasicAuthHeaders() });
  return res.status();
}

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} parentName
 * @param {string} seedName
 * @returns {Promise<{ path: string, name: string }>}
 */
async function seedDisposableFolder(request, parentName, seedName) {
  const headers = {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
  };
  const parent = String(parentName).replace(/^\/+|\/+$/g, "");
  const addUrl = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/addNewFolder/${parent}?name=${encodeURIComponent(seedName)}`;
  const addRes = await request.get(addUrl, { headers });
  expect(
    addRes.ok(),
    `addNewFolder under /${parent} must succeed (HTTP ${addRes.status()})`,
  ).toBeTruthy();
  const created = unwrapPathItem(await addRes.json());
  let name = String(created.name || seedName);
  let path = String(created.path || `/${parent}/${name}`);
  if (name !== seedName) {
    const renameRes = await request.post(
      `${BASE_URL}/Rhythmyx/services/pathmanagement/path/renameFolder`,
      {
        headers: { ...headers, "Content-Type": "application/json" },
        data: wrapRenameFolderItem(path, seedName),
      },
    );
    expect(
      isRenameFolderSuccessStatus(renameRes.status()),
      `seed renameFolder expected 200, got ${renameRes.status()}`,
    ).toBeTruthy();
    const renamed = unwrapPathItem(await renameRes.json());
    name = String(renamed.name || seedName);
    path = String(renamed.path || `/${parent}/${name}`);
  }
  return { path, name };
}

test.describe("Explorer folder rename refuses a NUL (#5478)", () => {
  test(
    "a NUL does not rename; a normal name renames only after success",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      let renamePosts = 0;
      page.on("request", (req) => {
        if (
          req.method() !== "OPTIONS" &&
          isPathmanagementRenameFolderUrl(req.url())
        ) {
          renamePosts += 1;
        }
      });

      const seedName = uniqueRenameFolderName("qa5478s");
      const siblingName = uniqueRenameFolderName("qa5478b");
      const newName = uniqueRenameFolderName("qa5478r");
      /** @type {Array<{ path: string, name: string }>} */
      const live = [];

      try {
        const assetsStatus = await getStatus(request, assetsFolderUrl(BASE_URL));
        const sitesStatus = await getStatus(request, sitesFolderUrl(BASE_URL));
        expect(
          assetsStatus === 200 || sitesStatus === 200,
          `H2 Explorer parent missing; Assets=${assetsStatus} Sites=${sitesStatus}`,
        ).toBe(true);
        const parentName = assetsStatus === 200 ? "Assets" : "Sites";
        const folder = await seedDisposableFolder(request, parentName, seedName);
        const sibling = await seedDisposableFolder(
          request,
          parentName,
          siblingName,
        );
        live.push(folder, sibling);

        await loginAsAdmin(page);
        const explorerUrl = explorerProductRenameFolderUrl(BASE_URL);
        expect(hasRxFolderMutationsQuery(explorerUrl)).toBe(false);
        await page.goto(explorerUrl, { waitUntil: "networkidle" });

        const shell = page.locator(`[data-testid="${RENAME_TEST_IDS.shell}"]`);
        await expect(shell).toBeVisible({ timeout: 20_000 });
        const parentNode =
          parentName === "Assets"
            ? treeRootLocator(page, "Assets").first()
            : sitesTreeRootLocator(page).first();
        await expect(parentNode).toBeVisible({ timeout: 20_000 });
        await parentNode.click({ force: true });
        await expandExplorerTreeNode(parentNode);
        const list = page.locator(
          `[data-testid="${RENAME_TEST_IDS.detailList}"]`,
        );
        await list.waitFor({ timeout: 15_000 });
        const seedRow = list.getByText(folder.name, { exact: true });
        const siblingRow = list.getByText(sibling.name, { exact: true });
        await expect(seedRow).toBeVisible({ timeout: 20_000 });
        await expect(siblingRow).toBeVisible({ timeout: 20_000 });
        await seedRow.click();

        const tree = page.locator(`[data-testid="${RENAME_TEST_IDS.tree}"]`);
        const renameBtn = page.locator(
          `[data-testid="${RENAME_TEST_IDS.actionRename}"]`,
        );
        await expect(renameBtn).toBeEnabled({ timeout: 10_000 });
        const postsBeforeUi = renamePosts;

        await renameBtn.click();
        const nameInput = page.getByTestId("folder-rename-name");
        await expect(nameInput).toBeVisible({ timeout: 10_000 });
        await setInputValue(nameInput, NUL_NAME);
        await page.getByTestId("folder-rename-submit").click();
        await expect(page.getByTestId("folder-rename-error")).toContainText(
          /cannot be stored/i,
        );
        await expect(page.getByTestId("explorer-folder-rename")).toBeVisible();
        await expect(tree.getByText(folder.name, { exact: true })).toBeVisible();
        expect(renamePosts, "NUL folder rename must not POST").toBe(
          postsBeforeUi,
        );

        await nameInput.fill("a/b");
        await page.getByTestId("folder-rename-submit").click();
        await expect(page.getByTestId("folder-rename-error")).toBeVisible();
        expect(renamePosts, "separator folder rename must not POST").toBe(
          postsBeforeUi,
        );

        await nameInput.fill("   ");
        await page.getByTestId("folder-rename-submit").click();
        await expect(page.getByTestId("folder-rename-error")).toBeVisible();
        expect(renamePosts, "blank folder rename must not POST").toBe(
          postsBeforeUi,
        );

        await nameInput.fill(sibling.name.toLowerCase());
        await page.getByTestId("folder-rename-submit").click();
        await expect(page.getByTestId("folder-rename-error")).toContainText(
          /already in this folder/i,
        );
        expect(renamePosts, "collision must not POST").toBe(postsBeforeUi);
        await expect(tree.getByText(folder.name, { exact: true })).toBeVisible();

        await nameInput.fill(folder.name);
        await page.getByTestId("folder-rename-submit").click();
        await expect(page.getByTestId("explorer-folder-rename")).toHaveCount(0);
        expect(renamePosts, "unchanged name must not POST").toBe(postsBeforeUi);

        await renameBtn.click();
        await expect(page.getByTestId("folder-rename-name")).toBeVisible();
        await page.getByTestId("folder-rename-cancel").click();
        await expect(page.getByTestId("explorer-folder-rename")).toHaveCount(0);
        expect(renamePosts, "cancel must not POST").toBe(postsBeforeUi);

        let forcedStatus = 0;
        await page.route(
          /\/pathmanagement\/path\/renameFolder(?:\?|$)/i,
          async (route) => {
            if (forcedStatus) {
              await route.fulfill({
                status: forcedStatus,
                contentType: "text/plain",
                body: "no",
              });
              return;
            }
            await route.continue();
          },
        );

        for (const status of [400, 403, 409]) {
          forcedStatus = status;
          const before = renamePosts;
          await renameBtn.click();
          await page.getByTestId("folder-rename-name").fill(`${newName}x${status}`);
          await page.getByTestId("folder-rename-submit").click();
          await expect(page.getByTestId("folder-rename-error")).toBeVisible({
            timeout: 15_000,
          });
          await expect(page.getByTestId("explorer-folder-rename")).toBeVisible();
          await expect(tree.getByText(folder.name, { exact: true })).toBeVisible();
          expect(
            renamePosts,
            `HTTP ${status} must not be treated as success`,
          ).toBeGreaterThan(before);
          await expect(
            tree.getByText(`${newName}x${status}`, { exact: true }),
          ).toHaveCount(0);
          await page.getByTestId("folder-rename-cancel").click();
        }

        forcedStatus = 0;
        await renameBtn.click();
        await page.getByTestId("folder-rename-name").fill(newName);
        const renameRespPromise = page.waitForResponse(
          (res) =>
            isPathmanagementRenameFolderUrl(res.url()) &&
            res.request().method() !== "OPTIONS",
          { timeout: 30_000 },
        );
        await page.getByTestId("folder-rename-submit").click();
        const renameResp = await renameRespPromise;
        expect(
          isRenameFolderSuccessStatus(renameResp.status()),
          `renameFolder expected 200, got ${renameResp.status()}`,
        ).toBeTruthy();
        const renamedItem = unwrapPathItem(await renameResp.json());
        if (renamedItem.path) {
          folder.path = String(renamedItem.path);
          folder.name = String(renamedItem.name || newName);
        } else {
          folder.name = newName;
        }
        await expect(list.getByText(newName, { exact: true })).toBeVisible({
          timeout: 20_000,
        });
        await expect(tree.getByText(newName, { exact: true })).toBeVisible({
          timeout: 15_000,
        });

        await expectNoSeriousA11yViolations(page, {
          scope: `[data-testid="${RENAME_TEST_IDS.shell}"]`,
        });
        const relatedConsole = jsErrors.filter(
          (t) => !isKnownExplorerSitesConsoleNoise(t),
        );
        expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
      } finally {
        const cleanupErrors = [];
        for (const item of live) {
          if (!item.path) {
            continue;
          }
          try {
            await recycleFolder(request, BASE_URL, adminBasicAuthHeaders(), {
              path: item.path,
            });
          } catch (err) {
            cleanupErrors.push(err instanceof Error ? err.message : String(err));
          }
        }
        if (cleanupErrors.length > 0) {
          throw new Error(cleanupErrors.join("\n"));
        }
      }
    },
  );
});
