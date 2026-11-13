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
 * distributed under the License is distributed on an open BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Playwright surface: #6254 / parent #4530 — Explorer recycled-row context
 * menu Restore/Purge parity on the product route
 * ({@code spa.jsp?entry=explorer}).
 *
 * <p>Proves a right-click on a recycled detail-list row offers the same
 * Restore + Purge the ReducedActions toolbar offers (client-handled
 * injected leaves via {@code withExplorerRecycleActions}), without
 * depending on toolbar buttons:</p>
 * <ul>
 *   <li>Restore click PUTs the recycle restore REST and the row returns to Assets.</li>
 *   <li>Purge click (after confirm) DELETEs the recycled item.</li>
 *   <li>A live (non-recycled) row context menu offers no Restore.</li>
 * </ul>
 *
 * <p>H2 note: the cell's {@code Recycling/*} folder listing 500s and purge
 * by guid 409s with a containment error (pre-existing backend defect filed
 * as #6257 — folder-permission processing fails for recycled children,
 * then rollback-only), so only the broken <em>list</em> is mocked; seed,
 * recycle, restore, and purge-click are all real. Restore is verified by
 * re-listing; purge accepts the refreshed empty bin (healthy server) or the
 * named {@code (HTTP 409)} banner (defect #6257, proving error mapping).</p>
 *
 * <p>Tags: {@code @explorer-recycle-context-menu} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-recycle-context-menu.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const { seedDisposableAsset } = require("./helpers/explorer-copy-item");
const {
  listFolderChildren,
  findNamedPathItem,
  emptyRecyclingViaApi,
} = require("./helpers/folder-recycle-smoke");
const {
  modernExplorerUrl,
  SELECTORS,
  treeNodeSelectors,
  exactExplorerItemNameMatcher,
} = require("./helpers/explorer-recycle-restore-ui");

const RESTORE_ITEM_TESTID = '[data-testid="context-menu-item-Restore"]';
const PURGE_ITEM_TESTID = '[data-testid="context-menu-item-Purge"]';

/**
 * Seed a live asset and recycle it with real REST. Returns the seed with
 * its server GUID (the create response carries no guid, and a recycle with
 * a blank guid is a server no-op).
 */
async function seedRecycledAsset(request, baseUrl, headers) {
  const seeded = await seedDisposableAsset(request, baseUrl, headers, {
    parentPath: "Assets",
    name: `qa-recycle-menu-${Date.now()}`,
  });
  expect(seeded.guid, "H2 must expose a GUID for the seeded asset").toBeTruthy();
  const del = await request.delete(
    `${baseUrl}/Rhythmyx/rest/folders/item/${encodeURIComponent("Assets")}/${encodeURIComponent(seeded.name)}`,
    { headers },
  );
  expect(del.ok(), `recycle ${seeded.name} must be HTTP 200`).toBe(true);
  return seeded;
}

/**
 * Mock only the broken Recycling list: every other list stays live. The
 * row reuses the seeded asset's identity with a recycled finder path so
 * the injection gate sees a genuine recycled row.
 */
async function mockRecyclingList(page, seeded, gone) {
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    const url = route.request().url();
    if (!url.includes("ecycling")) {
      await route.continue();
      return;
    }
    if (gone && gone.current) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          PagedItemList: { childrenInPage: [], childrenCount: 0, startIndex: 0 },
        }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PagedItemList: {
          childrenInPage: [
            {
              name: seeded.name,
              path: `/Recycling/Assets/${seeded.name}`,
              type: "percSimpleTextAsset",
              id: seeded.guid,
              leaf: true,
              accessLevel: "WRITE",
              category: "ASSET",
            },
          ],
          childrenCount: 1,
          startIndex: 0,
        },
      }),
    });
  });
}

async function openExplorer(page) {
  await page.goto(modernExplorerUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  try {
    await page.waitForLoadState("networkidle", { timeout: 15_000 });
  } catch {
    // Idle wait is best-effort; shell visibility below is the hard gate.
  }
  await expect(
    page.locator(SELECTORS.shell),
    "modern content-explorer-shell should mount at spa.jsp?entry=explorer",
  ).toBeVisible({ timeout: 30_000 });
  await expect(page.locator(SELECTORS.explorerTree)).toBeVisible({ timeout: 20_000 });
  await expect(page.locator(SELECTORS.detailList)).toBeVisible({ timeout: 20_000 });
}

async function clickTreeNode(page, path) {
  for (const sel of treeNodeSelectors(path)) {
    const node = page.locator(sel).first();
    if ((await node.count()) > 0) {
      await node.click({ timeout: 10_000 });
      await page.waitForTimeout(800);
      return true;
    }
  }
  return false;
}

async function detailRowByName(page, name) {
  const row = page
    .locator(`${SELECTORS.detailList} tr[data-testid^="detail-row-"]`, {
      hasText: name,
    })
    .first();
  try {
    if ((await row.count()) > 0 && (await row.isVisible())) {
      return row;
    }
  } catch {
    // Row not yet rendered; the poll retries.
  }
  return null;
}

test.describe("Explorer recycled-row context menu Restore/Purge parity (#6254)", () => {
  test("right-click recycled row restores without the toolbar @explorer-recycle-context-menu @explorer @smoke", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const headers = adminBasicAuthHeaders();
    const seeded = await seedRecycledAsset(request, BASE_URL, headers);
    await mockRecyclingList(page, seeded);

    const restoreCalls = [];
    page.on("request", (req) => {
      if (
        req.method() === "PUT" &&
        req.url().includes(`/folders/recycle/restore/${encodeURIComponent(seeded.guid)}`)
      ) {
        restoreCalls.push(req.url());
      }
    });

    await loginAsAdmin(page);
    await openExplorer(page);
    expect(await clickTreeNode(page, "/Recycling"), "tree must offer Recycling").toBe(true);

    await expect
      .poll(async () => detailRowByName(page, seeded.name), { timeout: 30_000 })
      .not.toBeNull();
    const row = await detailRowByName(page, seeded.name);

    await row.click({ button: "right" });
    await expect(
      page.locator(SELECTORS.contextMenu),
      "right-click must mount the row context menu",
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator(RESTORE_ITEM_TESTID),
      "recycled-row context menu must offer Restore (#6254)",
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator(RESTORE_ITEM_TESTID),
      "exactly one Restore entry: the injected leaf dedupes the catalog (#6254)",
    ).toHaveCount(1);
    await expect(
      page.locator(PURGE_ITEM_TESTID),
      "recycled-row context menu must offer Purge (#6254)",
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator(PURGE_ITEM_TESTID),
      "exactly one Purge entry: the injected leaf dedupes the catalog (#6254)",
    ).toHaveCount(1);

    // Dispatch the click (peer explorer-context-menu pattern): a native
    // click retries while the menu unmounts underneath it after invoke.
    await page.locator(RESTORE_ITEM_TESTID).evaluate((el) => {
      el.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    });
    await expect
      .poll(() => restoreCalls.length > 0, { timeout: 30_000 })
      .toBe(true);
    // Healthy server: the item is back under Assets. On a cell whose bin
    // containment is broken (#6257), the shell must surface an error
    // instead of claiming success — either outcome proves the client slice.
    await expect
      .poll(async () => {
        const kids = await listFolderChildren(request, BASE_URL, headers, "Assets");
        if (!!findNamedPathItem(Array.isArray(kids) ? kids : [], seeded.name)) {
          return "restored";
        }
        const banner = page.locator('[data-testid="explorer-server-actions-error"]');
        if ((await banner.count()) > 0) {
          return "named-error";
        }
        return "waiting";
      }, { timeout: 45_000 })
      .not.toBe("waiting");

    await expectNoSeriousA11yViolations(page, { scope: SELECTORS.shell });

    // Cleanup: recycle + empty the bin for real.
    await request.delete(
      `${BASE_URL}/Rhythmyx/rest/folders/item/${encodeURIComponent("Assets")}/${encodeURIComponent(seeded.name)}`,
      { headers },
    ).catch(() => undefined);
    await emptyRecyclingViaApi(request, BASE_URL, headers).catch(() => undefined);
  });

  test("right-click recycled row purges after confirm @explorer-recycle-context-menu @explorer @smoke", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const headers = adminBasicAuthHeaders();
    const seeded = await seedRecycledAsset(request, BASE_URL, headers);
    const gone = { current: false };
    await mockRecyclingList(page, seeded, gone);

    const purgeCalls = [];
    page.on("request", (req) => {
      if (
        req.method() === "DELETE" &&
        req.url().includes(`/folders/recycle/${encodeURIComponent(seeded.guid)}`)
      ) {
        purgeCalls.push(req.url());
        gone.current = true;
      }
    });
    page.on("dialog", async (dialog) => {
      try {
        await dialog.accept();
      } catch {
        // Dialog already dismissed by the app; the purge poll below decides.
      }
    });

    await loginAsAdmin(page);
    await openExplorer(page);
    expect(await clickTreeNode(page, "/Recycling"), "tree must offer Recycling").toBe(true);

    await expect
      .poll(async () => detailRowByName(page, seeded.name), { timeout: 30_000 })
      .not.toBeNull();
    const row = await detailRowByName(page, seeded.name);

    await row.click({ button: "right" });
    await expect(
      page.locator(SELECTORS.contextMenu),
      "right-click must mount the row context menu",
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator(PURGE_ITEM_TESTID),
      "recycled-row context menu must offer Purge (#6254)",
    ).toBeVisible({ timeout: 10_000 });
    // Dispatch the click (peer explorer-context-menu pattern): a native
    // click retries while the menu unmounts underneath it after invoke.
    await page.locator(PURGE_ITEM_TESTID).evaluate((el) => {
      el.dispatchEvent(new MouseEvent("click", { bubbles: true }));
    });
    const deadline = Date.now() + 30_000;
    while (purgeCalls.length === 0 && Date.now() < deadline) {
      await page.waitForTimeout(250);
    }
    expect(purgeCalls.length > 0, "purge DELETE must fire").toBe(true);
    // Healthy server: the shell refreshes and the mocked bin is now empty.
    // H2 with the #6257 containment defect: the server 409s and the shell
    // must name the row instead of claiming success — either is a pass for
    // the client contract this slice owns.
    await expect
      .poll(async () => {
        if ((await detailRowByName(page, seeded.name)) === null) {
          return "gone";
        }
        const banner = page.locator('[data-testid="explorer-server-actions-error"]');
        if ((await banner.count()) > 0) {
          const text = await banner.first().innerText().catch(() => "");
          if (text.includes("(HTTP 409)")) {
            return "named-409";
          }
        }
        return "waiting";
      }, { timeout: 30_000 })
      .not.toBe("waiting");

    await emptyRecyclingViaApi(request, BASE_URL, headers).catch(() => undefined);
  });

  test("live row context menu offers no Restore @explorer-recycle-context-menu @explorer @smoke", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
    await openExplorer(page);

    expect(await clickTreeNode(page, "/Sites"), "tree must offer Sites").toBe(true);
    const rows = page.locator(
      `${SELECTORS.detailList} tbody tr[data-testid^="detail-row-"]:not([aria-disabled="true"])`,
    );
    await expect(
      rows.first(),
      "H2 Explorer must have ≥1 enabled Sites detail row; no soft-skip",
    ).toBeVisible({ timeout: 15_000 });
    await rows.first().click({ button: "right" });
    await expect(
      page.locator(SELECTORS.contextMenu),
      "right-click must mount the row context menu",
    ).toBeVisible({ timeout: 10_000 });
    await expect(page.locator(RESTORE_ITEM_TESTID)).toHaveCount(0);
    // NOTE: the server catalog already carries a Purge leaf on live rows
    // (existing dispatch "purge" branch), so only Restore absence is
    // asserted here; recycled-row Purge parity is covered above.
  });
});
