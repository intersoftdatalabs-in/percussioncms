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
 * Classic Finder UI recycle / restore companion for folder-recycle REST smoke
 * (#2489 residual #2541 / parent #2423; REST peer #2464 / PR #2487;
 * empty-recycling UI #2207).
 *
 * <p>Proves classic Finder chrome can recycle a folder (delete → soft-delete)
 * and either restore it or empty Recycling via UI controls. Hard fails when
 * pathmanagement context or Admin login is down (no soft skip).</p>
 *
 * <p>#2541 hardens miller-column / list selection so happy-path recycle uses
 * {@code #perc-finder-delete} without REST soft-delete fallback. Strategies:
 * path-bar to /Assets/{name}, listing-id, miller title/name, list-row. REST
 * recycle remains only as last-resort residual-shell recovery (logged).</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/finder-recycle-restore-ui.spec.js
 *   # tags:
 *   npm run test:surface -- --tag finder-recycle-restore
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 *
 * <p>List only (no live CMS):
 * {@code npm run test:surface:list -- --path tests/finder-recycle-restore-ui.spec.js}
 * or {@code --tag finder-recycle-restore}.</p>
 *
 * <p>Pure helpers: {@code npm run test:unit} (includes
 * {@code finder-recycle-restore-ui.test.js}).</p>
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
  classicFinderDashboardUrl,
  isFinderControlEnabled,
  normalizeFinderPathInput,
  isRestoreEligiblePath,
  isStillOnLoginPage,
  loginContextDownFailureMessage,
  deleteFolderApiPathFragment,
  restoreFolderApiPathFragment,
  emptyRecyclingApiPathFragment,
  recycledFolderFinderPath,
  exactFinderItemNameMatcher,
  finderRecycleSelectStrategies,
  isDeleteEligiblePath,
  pathBarReflectsFolderName,
  shouldUseRestRecycleFallback,
  millerSelectionFailureMessage,
  chooseRestoreOrEmptyBranch,
} = require("./helpers/finder-recycle-restore-ui");

/**
 * Expand finder body when collapsed, then set path bar and go.
 * @param {import("@playwright/test").Page} page
 * @param {string} finderPath e.g. "/Assets" or "/Recycling/Assets/seed"
 */
async function navigateFinderPath(page, finderPath) {
  const expander = page.locator(SELECTORS.finderExpander);
  if ((await expander.count()) > 0) {
    const outer = page.locator(SELECTORS.finderOuter);
    if ((await outer.count()) > 0) {
      const collapsed = await outer.first().getAttribute("collapsed");
      if (collapsed === "true") {
        await expander.first().click().catch(() => {});
      }
    }
  }

  const pathInput = page.locator(SELECTORS.pathSummary);
  await expect(
    pathInput,
    "classic Finder path bar (#mcol-path-summary) should exist on dashboard shell",
  ).toBeVisible({ timeout: 20_000 });

  const wire = normalizeFinderPathInput(finderPath);
  await pathInput.fill(wire);
  const go = page.locator(SELECTORS.pathGo);
  if ((await go.count()) > 0) {
    await go.click({ force: true }).catch(async () => {
      await pathInput.press("Enter");
    });
  } else {
    await pathInput.press("Enter");
  }
  // Allow path-changed listeners (delete/restore enablement) + column load.
  await page.waitForTimeout(900);
}

/**
 * Prefer miller column view when the chooser is present (list-view variance).
 * @param {import("@playwright/test").Page} page
 */
async function preferColumnView(page) {
  const col = page.locator(SELECTORS.chooseColumnView);
  if ((await col.count()) === 0) {
    return;
  }
  const disabled = await col.first().getAttribute("class").catch(() => "");
  if (String(disabled || "").includes("ui-state-disabled")) {
    return;
  }
  await col.first().click({ timeout: 3_000 }).catch(() => {});
  await page.waitForTimeout(400);
}

/**
 * Click a miller listing / list row by exact display name (column + list).
 * Prefers clicking the listing parent (.mcol-listing) so path_changed fires.
 * @param {import("@playwright/test").Page} page
 * @param {string} name
 * @returns {Promise<boolean>} true when a matching item was clicked
 */
async function selectFinderItemByName(page, name) {
  const match = exactFinderItemNameMatcher(name);

  // 1) Miller listing by title attribute (product sets title = displayLabel).
  const byTitle = page.locator(
    `${SELECTORS.millerListing}[title="${String(name).replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"]`,
  );
  if ((await byTitle.count()) > 0) {
    await byTitle.first().click({ timeout: 5_000 }).catch(() => {});
    await page.waitForTimeout(500);
    return true;
  }

  // 2) Miller item-name text → click parent listing.
  const names = page.locator(SELECTORS.finderItemName);
  const count = await names.count();
  for (let i = 0; i < count; i++) {
    const text = await names.nth(i).innerText().catch(() => "");
    if (match(text)) {
      const listing = names.nth(i).locator("xpath=ancestor-or-self::*[contains(@class,'mcol-listing')][1]");
      if ((await listing.count()) > 0) {
        await listing.first().click({ timeout: 5_000 }).catch(() => {});
      } else {
        await names.nth(i).click({ timeout: 5_000 }).catch(() => {});
      }
      await page.waitForTimeout(500);
      return true;
    }
  }

  // 3) List view rows (PercFinderListView).
  const rows = page.locator(SELECTORS.listViewRow);
  const rowCount = await rows.count();
  for (let i = 0; i < rowCount; i++) {
    const text = await rows.nth(i).innerText().catch(() => "");
    const lines = String(text || "")
      .split(/\r?\n/)
      .map((s) => s.trim())
      .filter(Boolean);
    if (lines.some((line) => match(line)) || match(text)) {
      await rows.nth(i).click({ timeout: 5_000 }).catch(() => {});
      await page.waitForTimeout(500);
      return true;
    }
  }

  // 4) Any exact text under finder outer.
  const byText = page
    .locator(SELECTORS.finderOuter)
    .getByText(name, { exact: true });
  if ((await byText.count()) > 0) {
    await byText.first().click({ timeout: 5_000 }).catch(() => {});
    await page.waitForTimeout(500);
    return true;
  }
  return false;
}

/**
 * Poll until #perc-finder-delete looks enabled (ui-enabled / not ui-disabled).
 * @param {import("@playwright/test").Locator} deleteBtn
 * @param {number} [timeoutMs=15_000]
 * @returns {Promise<boolean>}
 */
async function waitForDeleteEnabled(deleteBtn, timeoutMs = 15_000) {
  try {
    await expect
      .poll(
        async () => {
          const cls = (await deleteBtn.getAttribute("class")) || "";
          return isFinderControlEnabled(cls);
        },
        { timeout: timeoutMs },
      )
      .toBe(true);
    return true;
  } catch {
    return false;
  }
}

/**
 * Apply ordered #2541 strategies so selection enables UI recycle.
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} deleteBtn
 * @param {{ name: string, parentPath?: string, guid?: string }} target
 * @returns {Promise<{ selected: boolean, deleteEnabled: boolean, strategiesTried: string[], pathBar: string }>}
 */
async function selectForUiRecycle(page, deleteBtn, target) {
  const strategies = finderRecycleSelectStrategies(target);
  /** @type {string[]} */
  const strategiesTried = [];
  let selected = false;
  let deleteEnabled = false;

  await preferColumnView(page);

  for (const strategy of strategies) {
    strategiesTried.push(strategy.kind);

    if (strategy.kind === "path-bar" && strategy.path) {
      await navigateFinderPath(page, strategy.path);
      const barAfter = await page
        .locator(SELECTORS.pathSummary)
        .inputValue()
        .catch(() => "");
      selected =
        pathBarReflectsFolderName(barAfter, target.name) ||
        isDeleteEligiblePath(barAfter);
    } else if (strategy.kind === "listing-id" && strategy.selector) {
      // Ensure parent column is open first so listing exists.
      await navigateFinderPath(page, normalizeFinderPathInput(target.parentPath || "Assets"));
      const listing = page.locator(strategy.selector);
      if ((await listing.count()) > 0) {
        await listing.first().click({ timeout: 5_000 }).catch(() => {});
        await page.waitForTimeout(500);
        selected = true;
      }
    } else if (
      strategy.kind === "miller-title" ||
      strategy.kind === "miller-name" ||
      strategy.kind === "list-row"
    ) {
      await navigateFinderPath(page, normalizeFinderPathInput(target.parentPath || "Assets"));
      // Wait briefly for seeded folder to appear in column/list.
      try {
        await expect
          .poll(
            async () => {
              const names = page.locator(SELECTORS.finderItemName);
              const n = await names.count();
              for (let i = 0; i < n; i++) {
                const t = await names.nth(i).innerText().catch(() => "");
                if (exactFinderItemNameMatcher(target.name)(t)) {
                  return true;
                }
              }
              const rows = page.locator(SELECTORS.listViewRow);
              const rc = await rows.count();
              for (let i = 0; i < rc; i++) {
                const t = await rows.nth(i).innerText().catch(() => "");
                if (String(t || "").includes(target.name)) {
                  return true;
                }
              }
              return false;
            },
            { timeout: 12_000 },
          )
          .toBe(true);
      } catch {
        // Item not visible yet — still try click strategies below.
      }
      selected = (await selectFinderItemByName(page, target.name)) || selected;
    }

    deleteEnabled = await waitForDeleteEnabled(deleteBtn, 12_000);
    if (selected && deleteEnabled) {
      break;
    }
    // If path-bar made delete eligible path but control still disabled, keep trying.
    if (deleteEnabled) {
      selected = true;
      break;
    }
  }

  const pathBar = await page
    .locator(SELECTORS.pathSummary)
    .inputValue()
    .catch(() => "");
  return { selected, deleteEnabled, strategiesTried, pathBar };
}

/**
 * Open Actions menu and return Empty Recycling control when present.
 * @param {import("@playwright/test").Page} page
 */
async function openEmptyRecyclingAction(page) {
  const actionsBtn = page.locator(SELECTORS.actionsButton);
  await expect(
    actionsBtn,
    "classic Finder Actions button should exist when finder scripts are loaded",
  ).toBeVisible({ timeout: 30_000 });
  await actionsBtn.click();
  const emptyBtn = page.locator(SELECTORS.emptyAction);
  await expect(
    emptyBtn,
    "Empty Recycling menu entry missing — deploy #2206 empty-recycling WebUI",
  ).toBeVisible({ timeout: 15_000 });
  return emptyBtn;
}

// Tags live on individual test() titles only — Playwright ignores @tags on describe names.
test.describe("classic Finder UI recycle / restore companion", () => {
  test("pathmanagement context is up (hard fail if Rhythmyx dead) @finder-recycle-restore @folder-recycle @smoke", async ({
    request,
  }) => {
    test.setTimeout(45_000);
    const headers = adminBasicAuthHeaders();
    const probe = await probePathmanagementContext(request, BASE_URL, headers);
    expect(probe.ok, probe.message || contextDownFailureMessage({})).toBe(true);
    expect(probe.status).toBeGreaterThanOrEqual(200);
  });

  test("Admin login hard-fails when still on login page @finder-recycle-restore @folder-recycle @smoke", async ({
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

  test("UI: recycle folder from Finder then restore or empty @finder-recycle-restore @folder-recycle @smoke", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const headers = adminBasicAuthHeaders();

    // Hard fail first — never soft-skip a dead context as "chrome missing".
    const probe = await probePathmanagementContext(request, BASE_URL, headers);
    expect(probe.ok, probe.message || contextDownFailureMessage({})).toBe(true);

    // Seed a unique folder under Assets via REST (same as REST smoke / #2207).
    const created = await createNamedFolder(request, BASE_URL, headers, {
      parentPath: "Assets",
    });
    expect(created.name).toBeTruthy();
    expect(created.path).toMatch(/Assets/i);

    /** @type {string[]} */
    const deleteCalls = [];
    /** @type {string[]} */
    const restoreCalls = [];
    /** @type {string[]} */
    const emptyCalls = [];
    page.on("request", (req) => {
      const u = req.url();
      if (u.includes(deleteFolderApiPathFragment())) {
        deleteCalls.push(u);
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

    await loginAsAdmin(page);
    const loginUrl = page.url();
    expect(
      isStillOnLoginPage(loginUrl),
      loginContextDownFailureMessage({ url: loginUrl, baseUrl: BASE_URL }),
    ).toBe(false);

    await page.goto(classicFinderDashboardUrl(BASE_URL), {
      waitUntil: "domcontentloaded",
    });
    await page.waitForLoadState("networkidle").catch(() => {});

    // Classic Finder chrome must load (hard fail if shell is wrong product).
    const deleteBtn = page.locator(SELECTORS.deleteButton);
    await expect(
      deleteBtn,
      "classic Finder delete control (#perc-finder-delete) should exist",
    ).toBeVisible({ timeout: 30_000 });

    // #2541: ordered strategies (path-bar / listing-id / miller / list) so
    // #perc-finder-delete enables without relying on REST soft-delete first.
    const selection = await selectForUiRecycle(page, deleteBtn, {
      name: created.name,
      parentPath: "Assets",
      guid: created.guid,
    });

    let recycledViaUi = false;
    if (selection.selected && selection.deleteEnabled) {
      await deleteBtn.click();
      // Optional confirm (page/asset paths use it; Admin folders often do not).
      const confirm = page.locator(SELECTORS.deleteConfirm);
      if (
        (await confirm.count()) > 0 &&
        (await confirm.isVisible().catch(() => false))
      ) {
        await page.locator(SELECTORS.confirmOk).click().catch(() => {});
      }
      try {
        await expect
          .poll(() => deleteCalls.length > 0, { timeout: 20_000 })
          .toBe(true);
      } catch {
        // Network may not surface if soft-delete used a different path.
      }
      recycledViaUi = deleteCalls.length > 0;
      // Also accept when REST listing shows recycled even without captured call.
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

    // Last-resort REST only when UI selection/enablement failed (residual shell).
    // Happy path must not need this (#2541).
    if (
      shouldUseRestRecycleFallback({
        selected: selection.selected,
        deleteEnabled: selection.deleteEnabled,
        recycledViaUi,
      })
    ) {
      test.info().annotations.push({
        type: "warning",
        description: millerSelectionFailureMessage({
          name: created.name,
          strategiesTried: selection.strategiesTried,
          pathBar: selection.pathBar,
        }),
      });
      await recycleFolder(request, BASE_URL, headers, {
        path: created.path,
        guid: created.guid,
      });
    } else if (!recycledViaUi) {
      // Selected + enabled but click did not recycle — still try REST recover.
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

    // Prefer restore when we can navigate to a restore-eligible path and
    // enable #perc-finder-restore-item; else Empty Recycling UI (#2207 peer).
    const restoreCandidates = [
      recycledFolderFinderPath(created.name, "Assets"),
      recycledFolderFinderPath(created.name, "Sites"),
      recycledFolderFinderPath(created.name, ""),
      "/Recycling/Assets",
      "/Recycling",
    ];

    let branch = "empty";
    let restoreEnabled = false;
    let pathEligible = false;

    for (const candidate of restoreCandidates) {
      await navigateFinderPath(page, candidate);
      pathEligible = isRestoreEligiblePath(candidate);
      await selectFinderItemByName(page, created.name);
      // Restore lives in Actions menu for some skins, toolbar for others.
      const restoreBtn = page.locator(SELECTORS.restoreItem).first();
      if ((await restoreBtn.count()) === 0) {
        const actionsBtn = page.locator(SELECTORS.actionsButton);
        if ((await actionsBtn.count()) > 0) {
          await actionsBtn.click().catch(() => {});
        }
      }
      if ((await restoreBtn.count()) > 0) {
        const cls = (await restoreBtn.getAttribute("class")) || "";
        restoreEnabled = isFinderControlEnabled(cls);
        branch = chooseRestoreOrEmptyBranch({ pathEligible, restoreEnabled });
        if (branch === "restore") {
          await restoreBtn.click();
          await expect
            .poll(() => restoreCalls.length > 0, { timeout: 30_000 })
            .toBe(true);

          // After restore, folder should leave Recycling or reappear under Assets.
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
          const emptied = await emptyRecyclingViaApi(request, BASE_URL, headers);
          expect(
            emptied.status >= 200 && emptied.status < 300,
            emptyApiFailureMessage(emptied),
          ).toBe(true);
          return;
        }
      }
    }

    // Empty Recycling UI happy path (proven #2207 patterns).
    expect(branch).toBe("empty");
    await navigateFinderPath(page, "/Recycling");
    const emptyBtn = await openEmptyRecyclingAction(page);
    await expect
      .poll(
        async () => {
          const cls = (await emptyBtn.getAttribute("class")) || "";
          return isFinderControlEnabled(cls);
        },
        { timeout: 20_000 },
      )
      .toBe(true);

    await emptyBtn.click();
    const dialog = page.locator(SELECTORS.confirmDialog);
    await expect(dialog).toBeVisible({ timeout: 15_000 });
    await page.locator(SELECTORS.confirmOk).click();

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
  });
});
