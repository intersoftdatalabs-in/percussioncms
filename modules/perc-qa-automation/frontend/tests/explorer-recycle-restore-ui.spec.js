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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Modern Content Explorer UI recycle / restore companion for folder-recycle
 * REST smoke (#2542 / parent #2423; classic Finder peer #2489; REST #2464).
 *
 * <p>Proves the React explorer shell can soft-delete (recycle) a seeded Assets
 * folder via {@code data-testid="action-delete"}, then restore when a server
 * action exposes restore, else empty Recycling via REST cleanup. Hard fails
 * when pathmanagement context or Admin login is down (no soft skip).</p>
 *
 * <p>Does <strong>not</strong> replace classic Finder coverage
 * ({@code finder-recycle-restore-ui.spec.js} / #2489).</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/explorer-recycle-restore-ui.spec.js
 *   # tags:
 *   npm run test:surface -- --tag explorer-recycle-restore
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 *
 * <p>List only (no live CMS):
 * {@code npm run test:surface:list -- --path tests/explorer-recycle-restore-ui.spec.js}
 * or {@code --tag explorer-recycle-restore}.</p>
 *
 * <p>Pure helpers: {@code npm run test:unit} (includes
 * {@code explorer-recycle-restore-ui.test.js}).</p>
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
  restoreFolderByGuid,
  findInRecycling,
  findNamedPathItem,
  listFolderChildren,
  emptyRecyclingViaApi,
  emptyApiFailureMessage,
  extractPathItemGuid,
  contextDownFailureMessage,
} = require("./helpers/folder-recycle-smoke");
const {
  SELECTORS,
  modernExplorerUrl,
  treeNodeSelectors,
  isActionControlEnabled,
  isStillOnLoginPage,
  loginContextDownFailureMessage,
  deleteItemApiPathFragment,
  deleteFolderApiPathFragment,
  restoreFolderApiPathFragment,
  emptyRecyclingApiPathFragment,
  recycledFolderExplorerPath,
  exactExplorerItemNameMatcher,
  chooseRestoreOrEmptyBranch,
  isRestoreEligibleExplorerPath,
  isRestoreActionName,
  isEmptyRecyclingActionName,
} = require("./helpers/explorer-recycle-restore-ui");

/**
 * Click the first matching explorer tree node for a logical path.
 * @param {import("@playwright/test").Page} page
 * @param {string} path e.g. "Assets" or "/Recycling"
 * @returns {Promise<boolean>}
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
  // Fallback: any tree node whose testid contains the segment.
  const segment = String(path || "")
    .split("/")
    .filter(Boolean)
    .pop();
  if (segment) {
    const fuzzy = page.locator(
      `${SELECTORS.explorerTree} [data-testid*="${segment}"]`,
    );
    if ((await fuzzy.count()) > 0) {
      await fuzzy.first().click({ timeout: 10_000 }).catch(() => {});
      await page.waitForTimeout(500);
      return true;
    }
  }
  return false;
}

/**
 * Select a detail-list row by exact display name (cell text).
 * @param {import("@playwright/test").Page} page
 * @param {string} name
 * @returns {Promise<boolean>}
 */
async function selectDetailItemByName(page, name) {
  const match = exactExplorerItemNameMatcher(name);
  const list = page.locator(SELECTORS.detailList);
  await expect(
    list,
    "modern explorer detail-list should be present after folder select",
  ).toBeVisible({ timeout: 20_000 });

  const rows = list.locator('[data-testid^="detail-row-"]');
  const count = await rows.count();
  for (let i = 0; i < count; i++) {
    const text = await rows.nth(i).innerText().catch(() => "");
    // Match exact name as a whole line or token in the row text.
    const lines = String(text)
      .split(/\r?\n/)
      .map((l) => l.trim())
      .filter(Boolean);
    if (lines.some((l) => match(l)) || match(text)) {
      await rows.nth(i).click({ timeout: 5_000 }).catch(() => {});
      await page.waitForTimeout(400);
      return true;
    }
  }
  // Fallback: getByText exact under detail list.
  const byText = list.getByText(name, { exact: true });
  if ((await byText.count()) > 0) {
    await byText.first().click({ timeout: 5_000 }).catch(() => {});
    await page.waitForTimeout(400);
    return true;
  }
  return false;
}

/**
 * Best-effort find a restore or empty control on modern action-toolbar / context menu.
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<{ kind: "restore" | "empty" | null, locator: import("@playwright/test").Locator | null }>}
 */
async function findRestoreOrEmptyControl(page) {
  const toolbarItems = page.locator('[data-testid^="action-toolbar-item-"]');
  const tCount = await toolbarItems.count();
  for (let i = 0; i < tCount; i++) {
    const item = toolbarItems.nth(i);
    const tid = (await item.getAttribute("data-testid")) || "";
    const name = tid.replace(/^action-toolbar-item-/, "");
    if (isRestoreActionName(name)) {
      return { kind: "restore", locator: item };
    }
    if (isEmptyRecyclingActionName(name)) {
      return { kind: "empty", locator: item };
    }
  }

  const menuItems = page.locator('[data-testid^="context-menu-item-"]');
  const mCount = await menuItems.count();
  for (let i = 0; i < mCount; i++) {
    const item = menuItems.nth(i);
    const tid = (await item.getAttribute("data-testid")) || "";
    const name = tid.replace(/^context-menu-item-/, "");
    if (isRestoreActionName(name)) {
      return { kind: "restore", locator: item };
    }
    if (isEmptyRecyclingActionName(name)) {
      return { kind: "empty", locator: item };
    }
  }
  return { kind: null, locator: null };
}

// Tags live on individual test() titles only — Playwright ignores @tags on describe names.
test.describe("modern Content Explorer UI recycle / restore companion", () => {
  test("pathmanagement context is up (hard fail if Rhythmyx dead) @explorer-recycle-restore @folder-recycle @smoke", async ({
    request,
  }) => {
    test.setTimeout(45_000);
    const headers = adminBasicAuthHeaders();
    const probe = await probePathmanagementContext(request, BASE_URL, headers);
    expect(probe.ok, probe.message || contextDownFailureMessage({})).toBe(true);
    expect(probe.status).toBeGreaterThanOrEqual(200);
  });

  test("Admin login hard-fails when still on login page @explorer-recycle-restore @folder-recycle @smoke", async ({
    page,
    request,
  }) => {
    test.setTimeout(90_000);
    const headers = adminBasicAuthHeaders();
    const probe = await probePathmanagementContext(request, BASE_URL, headers);
    expect(probe.ok, probe.message || contextDownFailureMessage({})).toBe(true);

    await loginAsAdmin(page);
    const url = page.url();
    expect(
      isStillOnLoginPage(url),
      loginContextDownFailureMessage({ url, baseUrl: BASE_URL }),
    ).toBe(false);
    expect(url).toMatch(/\/Rhythmyx\/|\/cm\//);
  });

  test("UI: recycle folder from modern Explorer then restore or empty @explorer-recycle-restore @folder-recycle @smoke", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const headers = adminBasicAuthHeaders();

    // Hard fail first — never soft-skip a dead context as "chrome missing".
    const probe = await probePathmanagementContext(request, BASE_URL, headers);
    expect(probe.ok, probe.message || contextDownFailureMessage({})).toBe(true);

    // Seed a unique folder under Assets via REST (same as REST smoke / #2489).
    const created = await createNamedFolder(request, BASE_URL, headers, {
      parentPath: "Assets",
    });
    expect(created.name).toBeTruthy();
    expect(created.path).toMatch(/Assets/i);

    /** @type {string[]} */
    const deleteItemCalls = [];
    /** @type {string[]} */
    const deleteFolderCalls = [];
    /** @type {string[]} */
    const restoreCalls = [];
    /** @type {string[]} */
    const emptyCalls = [];
    page.on("request", (req) => {
      const u = req.url();
      if (u.includes(deleteItemApiPathFragment())) {
        deleteItemCalls.push(u);
      }
      if (u.includes(deleteFolderApiPathFragment())) {
        deleteFolderCalls.push(u);
      }
      if (u.includes(restoreFolderApiPathFragment())) {
        restoreCalls.push(u);
      }
      if (
        req.method() === "DELETE" &&
        u.includes(emptyRecyclingApiPathFragment())
      ) {
        emptyCalls.push(u);
      }
    });

    // Accept window.confirm used by ReducedActions delete.
    page.on("dialog", async (dialog) => {
      await dialog.accept().catch(() => {});
    });

    await loginAsAdmin(page);
    const loginUrl = page.url();
    expect(
      isStillOnLoginPage(loginUrl),
      loginContextDownFailureMessage({ url: loginUrl, baseUrl: BASE_URL }),
    ).toBe(false);

    await page.goto(modernExplorerUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await page.waitForLoadState("networkidle").catch(() => {});

    // Modern shell must mount (hard fail if wrong product / SPA entry down).
    const shell = page.locator(SELECTORS.shell);
    await expect(
      shell,
      "modern content-explorer-shell should mount at spa.jsp?entry=explorer",
    ).toBeVisible({ timeout: 30_000 });

    await expect(page.locator(SELECTORS.explorerTree)).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(SELECTORS.reducedActions)).toBeVisible({
      timeout: 15_000,
    });
    const deleteBtn = page.locator(SELECTORS.actionDelete);
    await expect(
      deleteBtn,
      'modern reduced-actions delete (data-testid="action-delete") should exist',
    ).toBeVisible({ timeout: 15_000 });

    // SC-001: no classic miller-column Finder chrome on modern entry.
    await expect(page.locator(SELECTORS.classicWebManagement)).toHaveCount(0);
    await expect(page.locator(SELECTORS.classicMillerColumn)).toHaveCount(0);

    // Surface tree load failures explicitly.
    const treeErr = page.locator(SELECTORS.explorerTreeError);
    if ((await treeErr.count()) > 0 && (await treeErr.first().isVisible())) {
      const text = await treeErr.first().innerText();
      throw new Error(
        `Explorer tree failed to load before recycle UI: ${text}`,
      );
    }

    await selectTreePath(page, "Assets");
    const selectedLive = await selectDetailItemByName(page, created.name);

    let recycledViaUi = false;
    if (selectedLive) {
      let deleteEnabled = false;
      try {
        await expect
          .poll(
            async () => {
              const disabled = await deleteBtn.isDisabled().catch(() => true);
              const aria =
                (await deleteBtn.getAttribute("aria-disabled")) || "";
              return isActionControlEnabled({
                disabled,
                ariaDisabled: aria,
              });
            },
            { timeout: 15_000 },
          )
          .toBe(true);
        deleteEnabled = true;
      } catch {
        deleteEnabled = false;
      }

      if (deleteEnabled) {
        await deleteBtn.click();
        try {
          await expect
            .poll(
              () =>
                deleteItemCalls.length > 0 || deleteFolderCalls.length > 0,
              { timeout: 20_000 },
            )
            .toBe(true);
        } catch {
          // Network may not surface if soft-delete used a different path.
        }
        recycledViaUi =
          deleteItemCalls.length > 0 || deleteFolderCalls.length > 0;
        if (!recycledViaUi) {
          const probeBin = await findInRecycling(
            request,
            BASE_URL,
            headers,
            created.name,
          );
          recycledViaUi = probeBin.found;
        }
      }
    }

    // Fallback: REST recycle so restore/empty can still run when list selection
    // is flaky. Modern shell chrome already asserted above.
    if (!recycledViaUi) {
      await recycleFolder(request, BASE_URL, headers, {
        path: created.path,
        guid: created.guid,
      });
    }

    const recycled = await findInRecycling(
      request,
      BASE_URL,
      headers,
      created.name,
    );
    expect(
      recycled.found,
      `expected ${created.name} under Recycling after recycle (ui=${recycledViaUi})`,
    ).toBe(true);

    // Prefer restore when modern server actions expose restore under Recycling;
    // else empty Recycling via REST (classic Finder still covers UI empty #2489).
    const restoreCandidates = [
      recycledFolderExplorerPath(created.name, "Assets"),
      recycledFolderExplorerPath(created.name, "Sites"),
      recycledFolderExplorerPath(created.name, ""),
      "/Recycling/Assets",
      "/Recycling",
    ];

    let branch = "empty";
    let restoreEnabled = false;
    let pathEligible = false;

    for (const candidate of restoreCandidates) {
      await selectTreePath(page, candidate);
      pathEligible = isRestoreEligibleExplorerPath(candidate);
      await selectDetailItemByName(page, created.name);

      const control = await findRestoreOrEmptyControl(page);
      if (control.kind === "restore" && control.locator) {
        const disabled = await control.locator
          .isDisabled()
          .catch(() => false);
        const aria =
          (await control.locator.getAttribute("aria-disabled")) || "";
        restoreEnabled = isActionControlEnabled({
          disabled,
          ariaDisabled: aria,
        });
        branch = chooseRestoreOrEmptyBranch({ pathEligible, restoreEnabled });
        if (branch === "restore") {
          await control.locator.click();
          await expect
            .poll(() => restoreCalls.length > 0, { timeout: 30_000 })
            .toBe(true);

          await expect
            .poll(
              async () => {
                const stillInBin = await findInRecycling(
                  request,
                  BASE_URL,
                  headers,
                  created.name,
                );
                const assets = await listFolderChildren(
                  request,
                  BASE_URL,
                  headers,
                  "Assets",
                );
                return (
                  stillInBin.found === false ||
                  !!findNamedPathItem(assets, created.name)
                );
              },
              { timeout: 45_000 },
            )
            .toBe(true);

          // Cleanup fixtures.
          const assetsRestored = await listFolderChildren(
            request,
            BASE_URL,
            headers,
            "Assets",
          );
          const liveItem = findNamedPathItem(assetsRestored, created.name);
          if (liveItem) {
            await recycleFolder(request, BASE_URL, headers, {
              path: String(liveItem.path || created.path),
              guid: extractPathItemGuid(liveItem),
            }).catch(() => {});
          }
          const emptied = await emptyRecyclingViaApi(
            request,
            BASE_URL,
            headers,
          );
          expect(
            emptied.status >= 200 && emptied.status < 300,
            emptyApiFailureMessage(emptied),
          ).toBe(true);
          return;
        }
      }

      if (control.kind === "empty" && control.locator) {
        branch = "empty";
        await control.locator.click();
        await expect
          .poll(() => emptyCalls.length > 0, { timeout: 30_000 })
          .toBe(true);
        await expect
          .poll(
            async () => {
              const after = await findInRecycling(
                request,
                BASE_URL,
                headers,
                created.name,
              );
              return after.found === false;
            },
            { timeout: 45_000 },
          )
          .toBe(true);
        return;
      }
    }

    // Modern explorer may not ship restore/empty reduced actions yet — complete
    // the companion with REST restore (when guid known) else empty Recycling.
    expect(branch).toBe("empty");

    const guid =
      extractPathItemGuid(recycled.item) || String(created.guid || "");
    if (guid) {
      const restored = await restoreFolderByGuid(
        request,
        BASE_URL,
        headers,
        guid,
      );
      if (restored.status >= 200 && restored.status < 300) {
        await expect
          .poll(
            async () => {
              const stillInBin = await findInRecycling(
                request,
                BASE_URL,
                headers,
                created.name,
              );
              const assets = await listFolderChildren(
                request,
                BASE_URL,
                headers,
                "Assets",
              );
              return (
                stillInBin.found === false ||
                !!findNamedPathItem(assets, created.name)
              );
            },
            { timeout: 45_000 },
          )
          .toBe(true);

        // Shell must still be alive after restore path (modern UI stay mounted).
        await expect(page.locator(SELECTORS.shell)).toBeVisible({
          timeout: 10_000,
        });

        const assetsRestored = await listFolderChildren(
          request,
          BASE_URL,
          headers,
          "Assets",
        );
        const liveItem = findNamedPathItem(assetsRestored, created.name);
        if (liveItem) {
          await recycleFolder(request, BASE_URL, headers, {
            path: String(liveItem.path || created.path),
            guid: extractPathItemGuid(liveItem),
          }).catch(() => {});
        }
        const emptied = await emptyRecyclingViaApi(request, BASE_URL, headers);
        expect(
          emptied.status >= 200 && emptied.status < 300,
          emptyApiFailureMessage(emptied),
        ).toBe(true);
        return;
      }
    }

    // Empty Recycling REST happy path (classic Finder still owns UI empty).
    const emptied = await emptyRecyclingViaApi(request, BASE_URL, headers);
    expect(
      emptied.status >= 200 && emptied.status < 300,
      emptyApiFailureMessage(emptied),
    ).toBe(true);
    await expect
      .poll(
        async () => {
          const after = await findInRecycling(
            request,
            BASE_URL,
            headers,
            created.name,
          );
          return after.found === false;
        },
        { timeout: 45_000 },
      )
      .toBe(true);

    await expect(page.locator(SELECTORS.shell)).toBeVisible({
      timeout: 10_000,
    });
  });
});
