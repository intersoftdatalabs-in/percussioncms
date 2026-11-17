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
 * Explorer empty-trash restore round-trip with confirm (#6470 / parent #4530).
 *
 * Recycles a seeded Assets folder, restores it through the reduced-actions
 * Restore confirm dialog (which names the recycled row), verifies the folder
 * is back under Assets, recycles it again, then empties the bin through the
 * empty-recycle confirm dialog (bin listing + per-item report) and verifies
 * the seeded row leaves Recycling.
 *
 * H2 QA bin listings can 500 (rollback-only), so the Recycling listing is
 * stubbed with the seeded row (same pattern as
 * explorer-recycle-purge-one.spec.js); the restore/empty mutations are real.
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/explorer-empty-trash-restore-roundtrip.spec.js
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const {
  probePathmanagementContext,
  createNamedFolder,
  recycleFolder,
  listFolderChildren,
  findNamedPathItem,
  extractPathItemGuid,
  emptyRecyclingViaApi,
  contextDownFailureMessage,
} = require("./helpers/folder-recycle-smoke");
const {
  modernExplorerUrl,
  treeNodeSelectors,
} = require("./helpers/explorer-recycle-restore-ui");

const TAGS = ["@explorer-empty-trash-restore", "@explorer", "@smoke"];

/**
 * Click the first matching explorer tree node for a logical path.
 * @param {import("@playwright/test").Page} page
 * @param {string} path e.g. "Assets" or "/Recycling"
 */
async function selectTreePath(page, path) {
  const selectors = treeNodeSelectors(path);
  for (const sel of selectors) {
    const node = page.locator(sel);
    if ((await node.count()) > 0) {
      const row = node.first().locator('[role="treeitem"]').first();
      if ((await row.count()) > 0) {
        await row.click({ timeout: 10_000 }).catch(async () => {
          await node.first().click({ timeout: 10_000 });
        });
      } else {
        await node.first().click({ timeout: 10_000 });
      }
      await page.waitForTimeout(500);
      return true;
    }
  }
  return false;
}

test.describe("Explorer empty-trash restore round-trip with confirm (#6470)", () => {
  test(
    "recycle restores through the confirm dialog, then empty reports per item",
    { tag: TAGS },
    async ({ page, request }) => {
      test.setTimeout(240_000);
      const headers = adminBasicAuthHeaders();
      const probe = await probePathmanagementContext(request, BASE_URL, headers);
      expect(probe.ok, contextDownFailureMessage(probe)).toBeTruthy();

      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") jsErrors.push(msg.text());
      });

      const seeded = await createNamedFolder(request, BASE_URL, headers, {
        parentPath: "Assets",
        name: `qa6470rt${Date.now()}`,
      });
      expect(seeded && seeded.name, "seed folder").toBeTruthy();
      if (!seeded.guid) {
        const assets = await listFolderChildren(request, BASE_URL, headers, "Assets");
        const live = findNamedPathItem(assets, seeded.name);
        seeded.guid = extractPathItemGuid(live);
        if (live && live.path) {
          seeded.path = String(live.path);
        }
      }
      expect(seeded.guid, "seed folder guid").toBeTruthy();
      await recycleFolder(request, BASE_URL, headers, seeded);

      // Stub the Recycling listing with the seeded row (H2 QA bin listings
      // can 500); the restore/empty mutations below stay real.
      let showRecycled = true;
      await page.route("**/pathmanagement/path/**", async (route) => {
        const url = route.request().url();
        if (
          !/Recycling/i.test(url) ||
          route.request().method() !== "GET" ||
          !/\/(?:folder|paginatedFolder)\//i.test(url)
        ) {
          await route.continue();
          return;
        }
        const kids = showRecycled
          ? [
              {
                id: seeded.guid,
                name: seeded.name,
                path: `/Recycling/Assets/${seeded.name}`,
                folderPath: "//Folders/$System$/Recycling/Assets",
                type: "Folder",
                leaf: false,
                accessLevel: "WRITE",
              },
            ]
          : [];
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            PathItem: kids,
            PagedItemList: {
              childrenInPage: kids,
              childrenCount: kids.length,
              startIndex: 0,
            },
          }),
        });
      });

      await loginAsAdmin(page);
      await page.goto(modernExplorerUrl(BASE_URL), {
        waitUntil: "domcontentloaded",
      });
      await page.getByTestId("content-explorer-shell").waitFor({ timeout: 60_000 });
      await page.getByTestId("explorer-nav").waitFor({ timeout: 60_000 });

      // Restore leg: Recycling -> select the seeded row -> Restore confirm
      // dialog names the row -> OK -> the restore PUT fires.
      expect(await selectTreePath(page, "/Recycling"), "Recycling tree node").toBeTruthy();
      const seedRow = page.getByTestId(`detail-row-${seeded.guid}`);
      await expect(seedRow).toBeVisible({ timeout: 20_000 });
      await seedRow.click();
      const restoreBtn = page.getByTestId("action-restore");
      await expect(restoreBtn).toBeEnabled({ timeout: 15_000 });
      const restored = page.waitForResponse(
        (res) =>
          res.url().includes("/recycle/restore/") &&
          res.request().method() === "PUT" &&
          res.status() === 200,
        { timeout: 60_000 },
      );
      await restoreBtn.click();
      const restoreDialog = page.getByTestId("explorer-restore-confirm");
      await restoreDialog.waitFor({ timeout: 15_000 });
      await expect(restoreDialog).toContainText(seeded.name);
      await page.getByTestId("explorer-restore-ok").click();
      await restored;

      // Round-trip: the restored folder is back under Assets (real listing).
      showRecycled = false;
      await expect
        .poll(async () => {
          const assets = await listFolderChildren(request, BASE_URL, headers, "Assets");
          return Boolean(findNamedPathItem(assets, seeded.name));
        }, { timeout: 60_000 })
        .toBe(true);
      expect(await selectTreePath(page, "Assets"), "Assets tree node").toBeTruthy();
      await expect(
        page.getByText(seeded.name, { exact: true }).first(),
      ).toBeVisible({ timeout: 30_000 });

      // Empty leg: recycle again, then empty the bin through the dialog.
      const assetsAfter = await listFolderChildren(request, BASE_URL, headers, "Assets");
      const liveAgain = findNamedPathItem(assetsAfter, seeded.name);
      expect(liveAgain, "restored folder listed under Assets").toBeTruthy();
      await recycleFolder(request, BASE_URL, headers, {
        path: String(liveAgain.path || seeded.path),
        guid: extractPathItemGuid(liveAgain) || seeded.guid,
      });
      showRecycled = true;

      expect(await selectTreePath(page, "/Recycling"), "Recycling tree node").toBeTruthy();
      await expect(seedRow).toBeVisible({ timeout: 20_000 });
      const emptyBtn = page.getByTestId("action-empty-recycle");
      await expect(emptyBtn).toBeEnabled({ timeout: 15_000 });
      const emptied = page.waitForResponse(
        (res) =>
          res.url().includes("/folders/recycle/empty") &&
          res.request().method() === "POST" &&
          res.status() === 200,
        { timeout: 60_000 },
      );
      page.on("request", (req) => {
        if (
          req.method() === "POST" &&
          req.url().includes("/folders/recycle/empty")
        ) {
          showRecycled = false;
        }
      });
      await emptyBtn.click();
      const binList = page.getByTestId("explorer-empty-recycle-list");
      await binList.waitFor({ timeout: 20_000 });
      await expect(binList).toContainText(seeded.name);
      await page.getByTestId("explorer-empty-recycle-ok").click();
      await emptied;
      const report = page.getByTestId("explorer-empty-recycle-report");
      await expect(report).toContainText(/Removed 1 item\(s\)/, {
        timeout: 30_000,
      });
      await expect(report).toContainText(seeded.name, { timeout: 10_000 });
      await page.getByTestId("explorer-empty-recycle-close").click();
      await expect(seedRow).toHaveCount(0, { timeout: 30_000 });

      // Best-effort cleanup so later runs start from an empty bin.
      await emptyRecyclingViaApi(request, BASE_URL, headers).catch(() => {});

      const unexpected = jsErrors.filter(
        (line) => !/favicon|Failed to load resource/i.test(line),
      );
      expect(unexpected, unexpected.join("\n")).toEqual([]);
    },
  );
});
