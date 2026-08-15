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
 * Playwright surface: #2733 / #3456 — Explorer preview for a listed page.
 *
 * <p>Verifies product shell chrome for Preview + Refresh, then opens
 * product preview for a listed page row. Folders stay Preview-disabled.
 * Soft-skip only when REST listing has no page-type child (listing not
 * on the tip). Do not skip solely because the Sites root list is empty
 * of pages when {@code /Pages} lists children (#3457 on tip).</p>
 *
 * <p>Tags: {@code @explorer-preview-view} {@code @preview} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-preview-view.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
  noListedPageSkipMessage,
  isListedPageRow,
  unwrapPathItems,
  resolveExplorerListPath,
  isProductPagePreviewUrl,
  listedPageSiteNames,
  foldSiteName,
} = require("./helpers/explorer-preview-view");

const PATH_FOLDER = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folder`;
const PATH_PAGED = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/paginatedFolder`;

async function listWaitReady(page) {
  await page.locator(`[data-testid="${TEST_IDS.list}"]`).waitFor({
    timeout: 15_000,
  });
}

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} cmsPath
 * @returns {Promise<object[]>}
 */
async function fetchFolderChildren(request, cmsPath) {
  const headers = adminBasicAuthHeaders();
  const rel = String(cmsPath || "")
    .replace(/^\/+/, "")
    .replace(/\/+$/, "");
  const paged = await request.get(
    `${PATH_PAGED}/${rel}?startIndex=0&maxResults=50`,
    { headers },
  );
  if (paged.status() === 200) {
    return unwrapPathItems(await paged.json());
  }
  const folder = await request.get(`${PATH_FOLDER}/${rel}`, { headers });
  if (folder.status() === 200) {
    return unwrapPathItems(await folder.json());
  }
  return [];
}

/**
 * Walk Sites → site → Pages (and one more folder level) for a listed page.
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<object|null>}
 */
async function findListedPageViaRest(request) {
  const sites = await fetchFolderChildren(request, "Sites");
  const candidateFolders = [];
  for (const site of sites) {
    const listPath = resolveExplorerListPath(site);
    if (listPath) {
      candidateFolders.push(listPath.replace(/^\/+/, ""));
      candidateFolders.push(`${listPath.replace(/^\/+/, "")}/Pages`);
    }
    const name = site && site.name ? String(site.name) : "";
    if (name) {
      candidateFolders.push(`Sites/${name}`);
      candidateFolders.push(`Sites/${name}/Pages`);
    }
  }

  const seen = new Set();
  for (const folder of candidateFolders) {
    if (!folder || seen.has(folder)) continue;
    seen.add(folder);
    const kids = await fetchFolderChildren(request, folder);
    const page = kids.find((k) => isListedPageRow(k));
    if (page) return page;
    for (const kid of kids.slice(0, 12)) {
      const kidType = `${kid.type || ""} ${kid.category || ""}`.toLowerCase();
      const looksFolder =
        kidType.includes("folder") ||
        kidType.includes("site") ||
        String(kid.path || "").endsWith("/");
      if (!looksFolder) continue;
      const nestedPath = resolveExplorerListPath(kid);
      const nestedRel = nestedPath
        ? nestedPath.replace(/^\/+/, "")
        : `${folder}/${kid.name || ""}`;
      if (!nestedRel || seen.has(nestedRel)) continue;
      seen.add(nestedRel);
      const nested = await fetchFolderChildren(request, nestedRel);
      const nestedPage = nested.find((k) => isListedPageRow(k));
      if (nestedPage) return nestedPage;
    }
  }
  return null;
}

/**
 * Open Sites → the site that owns {@code listed} (not merely the first
 * folder row) → Pages when present. Finder names use underscores;
 * repository folderPath does not (#3326).
 * @param {import("@playwright/test").Page} page
 * @param {object} [listed]
 */
async function openSitesThenPages(page, listed) {
  const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
  const sitesNode = tree
    .locator(
      '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"]',
    )
    .first();
  await expect(sitesNode).toBeVisible({ timeout: 15_000 });
  await sitesNode.click({ force: true });
  await listWaitReady(page);
  await page.waitForLoadState("networkidle").catch(() => {});

  const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
  const siteRows = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
  );
  await expect(siteRows.first()).toBeVisible({ timeout: 15_000 });

  const wanted = new Set(listedPageSiteNames(listed).map((n) => foldSiteName(n)));
  const listedName = listed && listed.name ? String(listed.name) : "";
  const siteCount = await siteRows.count();
  let opened = false;
  for (let i = 0; i < siteCount; i += 1) {
    if (i > 0) {
      await sitesNode.click({ force: true });
      await listWaitReady(page);
      await page.waitForLoadState("networkidle").catch(() => {});
    }
    const row = siteRows.nth(i);
    const rowText = ((await row.innerText().catch(() => "")) || "").trim();
    const foldedRow = foldSiteName(rowText);
    const nameMatch =
      wanted.size === 0 || [...wanted].some((n) => n && foldedRow.includes(n));
    if (!nameMatch) continue;
    await row.dblclick({ force: true });
    await listWaitReady(page);
    await page.waitForLoadState("networkidle").catch(() => {});

    const pagesRow = list
      .locator('tbody tr[data-testid^="detail-row-"]')
      .filter({ hasText: /^Pages$/ })
      .first();
    if ((await pagesRow.count()) > 0) {
      await pagesRow.dblclick({ force: true });
      await listWaitReady(page);
      await page.waitForLoadState("networkidle").catch(() => {});
    }

    const previewable = list.locator(
      'tbody tr[data-testid^="detail-row-"][data-previewable="true"]',
    );
    const byName = listedName
      ? list
          .locator('tbody tr[data-testid^="detail-row-"]')
          .filter({ hasText: listedName })
      : previewable;
    if ((await previewable.count()) > 0 || (await byName.count()) > 0) {
      opened = true;
      break;
    }
  }
  if (!opened && wanted.size > 0) {
    throw new Error(
      `REST listed page ${listedName || listed.id} but UI did not open a ` +
        `matching site among ${[...wanted].join(", ")}`,
    );
  }
  if (!opened) {
    await siteRows.first().dblclick({ force: true });
    await listWaitReady(page);
    await page.waitForLoadState("networkidle").catch(() => {});
    const pagesRow = list
      .locator('tbody tr[data-testid^="detail-row-"]')
      .filter({ hasText: /^Pages$/ })
      .first();
    if ((await pagesRow.count()) > 0) {
      await pagesRow.dblclick({ force: true });
      await listWaitReady(page);
      await page.waitForLoadState("networkidle").catch(() => {});
    }
  }
}

test.describe("modern React Content Explorer — preview + view residual (#2733 / #3456)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(explorerEntryUrl(BASE_URL, { cacheBuster: Date.now() }));
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts preview action and refresh view control",
    { tag: ["@explorer-preview-view", "@preview", "@smoke"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      await expect(
        page.locator(`[data-testid="${TEST_IDS.reducedActions}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.preview}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.viewTools}"]`),
      ).toBeVisible();

      const refresh = page.locator(`[data-testid="${TEST_IDS.refresh}"]`);
      await expect(refresh).toBeVisible();
      await expect(refresh).toBeEnabled();
      await expect(refresh).toHaveAttribute("aria-label", /./);

      // Refresh is shell-state residual — click must not crash the shell.
      await refresh.click();
      await expect(shell).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.list}"]`),
      ).toBeVisible({ timeout: 15_000 });
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "preview opens for a listed page; folders stay disabled",
    { tag: ["@explorer-preview-view", "@preview"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));

      // Use the logged-in page request (session cookies). Isolated
      // APIRequestContext basic-auth is often redirected to login.
      const listed = await findListedPageViaRest(page.request);
      if (!listed) {
        test.skip(true, noListedPageSkipMessage());
        return;
      }

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      await openSitesThenPages(page, listed);

      const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
      await expect(list).toBeVisible({ timeout: 15_000 });

      const previewable = list.locator(
        'tbody tr[data-testid^="detail-row-"][data-previewable="true"]',
      );
      const itemRows = list.locator(
        'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]',
      );
      let pageRow = previewable.first();
      if ((await previewable.count()) === 0) {
        pageRow = itemRows.first();
      }
      if ((await pageRow.count()) === 0) {
        const byName = list
          .locator('tbody tr[data-testid^="detail-row-"]')
          .filter({ hasText: String(listed.name || "") })
          .first();
        if ((await byName.count()) === 0) {
          throw new Error(
            `REST listed page ${listed.name || listed.id} at ${listed.path} ` +
              `but Explorer detail list has no page/item row (listing on tip — do not skip)`,
          );
        }
        pageRow = byName;
      }

      await pageRow.click({ force: true });
      const preview = page.locator(`[data-testid="${TEST_IDS.preview}"]`);
      await expect(preview).toBeEnabled({ timeout: 10_000 });

      const popupPromise = page.waitForEvent("popup", { timeout: 10_000 });
      await preview.click();
      const popup = await popupPromise;
      let popupUrl = popup ? popup.url() : "";
      if (popup && (!popupUrl || /about:blank/i.test(popupUrl))) {
        await popup.waitForLoadState("domcontentloaded").catch(() => {});
        popupUrl = popup.url();
      }
      expect(
        isProductPagePreviewUrl(popupUrl),
        `Preview popup URL should be page render or site-path preview; got ${popupUrl}`,
      ).toBe(true);
      if (popup && !popup.isClosed()) {
        await popup.close().catch(() => {});
      }

      const folderRows = list.locator(
        'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
      );
      if ((await folderRows.count()) > 0) {
        await expect(folderRows.first()).toHaveAttribute(
          "data-previewable",
          "false",
        );
        const selectableFolder = list
          .locator(
            'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
          )
          .first();
        if ((await selectableFolder.count()) > 0) {
          await selectableFolder.click({ force: true });
          await expect(preview).toBeDisabled({ timeout: 10_000 });
        }
      }

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
