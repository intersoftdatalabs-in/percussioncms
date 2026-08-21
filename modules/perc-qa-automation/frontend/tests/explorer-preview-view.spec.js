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
 * Playwright surface: #2733 / #3456 / #3463 / #3627 / #3688 / #3696 — Explorer
 * preview for a listed page on {@code spa.jsp?entry=explorer}.
 *
 * <p>Verifies product shell chrome for Preview + Refresh, then opens
 * product preview for a listed page row (HTTP 200 / preview host).
 * Folders stay Preview-disabled. Do <strong>not</strong> soft-skip when
 * H2 demo-sites list a previewable row (or REST finds a page). Fail
 * instead of skip on H2 when no previewable row exists (#3627).</p>
 *
 * <p>Tags: {@code @explorer-preview-view} {@code @preview} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-preview-view.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect, errors } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
  noListedPageSkipMessage,
  shouldSkipListedPagePreview,
  encodeCmsRelPath,
  isListedPageRow,
  isPreviewableRow,
  unwrapPathItems,
  resolveExplorerListPath,
  isProductPagePreviewUrl,
  isAssembledPreviewHtml,
  listedPagePreviewCmsPath,
  cmsSitePathPreviewGetUrl,
  listedPageSiteNames,
  foldSiteName,
  detailRowHasExactName,
  detailRowMatchesFoldedSite,
  treeNodeMatchesFoldedSite,
  isExplorerSiteRootTestId,
  workflowCheckInPath,
  numericContentIdFromItemId,
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
  const rel = encodeCmsRelPath(cmsPath);
  if (!rel) {
    return [];
  }
  const paged = await request.get(
    `${PATH_PAGED}/${rel}?startIndex=0&maxResults=50`,
    { headers },
  );
  if (paged.status() === 200) {
    const kids = unwrapPathItems(await paged.json());
    if (kids.length > 0) {
      return kids;
    }
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
    const page = kids.find((k) => isListedPageRow(k) || isPreviewableRow(k));
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
      const nestedPage = nested.find(
        (k) => isListedPageRow(k) || isPreviewableRow(k),
      );
      if (nestedPage) return nestedPage;
    }
  }
  return null;
}

/**
 * Expand the Sites tree node (tree-toggle testid, else aria-hidden peer).
 * @param {import("@playwright/test").Locator} sitesNode
 */
async function expandSitesTreeNode(sitesNode) {
  const treeitem = sitesNode.locator('[role="treeitem"]').first();
  const expanded = await treeitem.getAttribute("aria-expanded");
  if (expanded === "true") {
    return;
  }
  const toggle = sitesNode
    .locator(
      '[data-testid="tree-toggle-/Sites/"], [data-testid="tree-toggle-/Sites"]',
    )
    .first();
  if ((await toggle.count()) > 0) {
    await toggle.click();
    return;
  }
  const ariaToggle = sitesNode.locator('[aria-hidden="true"]').first();
  if ((await ariaToggle.count()) > 0) {
    await ariaToggle.click();
  }
}

/**
 * True when the detail list shows the listed page or any content item row.
 * @param {import("@playwright/test").Locator} list
 * @param {string} listedName
 * @returns {Promise<boolean>}
 */
async function listHasListedOrItemRow(list, listedName) {
  const previewable = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-previewable="true"]',
  );
  if ((await previewable.count()) > 0) {
    return true;
  }
  const itemRows = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]',
  );
  if ((await itemRows.count()) > 0) {
    return true;
  }
  if (!listedName) {
    return false;
  }
  const aliases = [listedName];
  if (/\bHome\b/i.test(listedName) && listedName !== "Home") {
    aliases.push("Home");
  }
  const rows = list.locator('tbody tr[data-testid^="detail-row-"]');
  const rowCount = await rows.count();
  for (let i = 0; i < rowCount; i += 1) {
    const row = rows.nth(i);
    const itemName = (await row.getAttribute("data-item-name")) || "";
    const text = ((await row.innerText().catch(() => "")) || "").trim();
    if (
      aliases.some(
        (alias) => itemName === alias || detailRowHasExactName(text, alias),
      )
    ) {
      return true;
    }
  }
  return false;
}

/**
 * After opening a site, succeed if items (or the listed page) are visible,
 * otherwise open Pages chrome and re-check.
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} list
 * @param {string} listedName
 * @returns {Promise<boolean>}
 */
async function siteListingHasContent(page, list, listedName) {
  if (await listHasListedOrItemRow(list, listedName)) {
    return true;
  }
  await openPagesFolderIfPresent(page, list);
  return listHasListedOrItemRow(list, listedName);
}

/**
 * Open Sites → the site that owns {@code listed} via the explorer tree
 * (peer #3575 / #3684 / #3696). Product site-select lists Pages chrome.
 * GUID path testids still match when {@code data-node-name} /
 * {@code data-folder-path} / visible label fold to Corporate_Investments.
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
  await expandSitesTreeNode(sitesNode);

  const siteTreeNodes = tree.locator(
    '[data-testid^="tree-node-/Sites/"]:not([data-testid="tree-node-/Sites/"])',
  );
  await expect(siteTreeNodes.first()).toBeVisible({ timeout: 15_000 });

  const wanted = new Set(
    listedPageSiteNames(listed).map((n) => foldSiteName(n)),
  );
  const listedName = listed && listed.name ? String(listed.name) : "";
  const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
  const seen = [];
  const siteCount = await siteTreeNodes.count();

  const trySiteNode = async (node) => {
    await node.click({ force: true });
    await listWaitReady(page);
    await page.waitForLoadState("networkidle").catch(() => {});
    return siteListingHasContent(page, list, listedName);
  };

  /** @type {string[]} */
  const matchingTestIds = [];
  /** @type {string[]} */
  const allSiteTestIds = [];
  for (let i = 0; i < siteCount; i += 1) {
    const node = siteTreeNodes.nth(i);
    const testid = (await node.getAttribute("data-testid")) || "";
    if (!isExplorerSiteRootTestId(testid)) {
      continue;
    }
    const nodeName = (await node.getAttribute("data-node-name")) || "";
    const folderPath = (await node.getAttribute("data-folder-path")) || "";
    const label = ((await node.innerText().catch(() => "")) || "").trim();
    seen.push(`${testid}|${nodeName || label}|${folderPath}`);
    allSiteTestIds.push(testid);
    const nameMatch =
      wanted.size === 0 ||
      treeNodeMatchesFoldedSite(
        testid,
        label,
        nodeName,
        wanted,
        folderPath,
      );
    if (nameMatch) {
      matchingTestIds.push(testid);
    }
  }

  for (const testid of matchingTestIds) {
    const node = tree.locator(`[data-testid="${testid}"]`).first();
    if (await trySiteNode(node)) {
      return;
    }
  }

  const siteRows = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
  );
  await sitesNode.click({ force: true });
  await listWaitReady(page);
  const rowCount = await siteRows.count();
  for (let i = 0; i < rowCount; i += 1) {
    const row = siteRows.nth(i);
    const rowText = ((await row.innerText().catch(() => "")) || "").trim();
    const itemName = (await row.getAttribute("data-item-name")) || "";
    const nameMatch =
      wanted.size === 0 ||
      detailRowMatchesFoldedSite(rowText, wanted) ||
      treeNodeMatchesFoldedSite("", rowText, itemName, wanted);
    if (!nameMatch) continue;
    await openDetailFolderRow(row);
    if (await siteListingHasContent(page, list, listedName)) {
      return;
    }
    await sitesNode.click({ force: true });
    await listWaitReady(page);
  }

  for (const testid of allSiteTestIds) {
    if (matchingTestIds.includes(testid)) continue;
    const node = tree.locator(`[data-testid="${testid}"]`).first();
    if (await trySiteNode(node)) {
      return;
    }
  }

  if (wanted.size > 0 && listed) {
    throw new Error(
      `REST listed page ${listedName || listed.id} but Explorer list has no ` +
        `previewable row after opening site among ${[...wanted].join(", ")} ` +
        `(tree=${seen.join("; ") || "none"})`,
    );
  }
}

/**
 * Prefer the folder-icon open control (peer #3328); fall back to dblclick.
 * @param {import("@playwright/test").Locator} row
 */
async function openDetailFolderRow(row) {
  const icon = row.locator('[data-testid^="detail-folder-icon-"]').first();
  try {
    await icon.waitFor({ state: "attached", timeout: 1_000 });
    await icon.click();
  } catch (err) {
    if (!(err instanceof errors.TimeoutError)) {
      throw err;
    }
    await row.dblclick({ force: true });
  }
  const page = row.page();
  await listWaitReady(page);
  await page.waitForLoadState("networkidle").catch(() => {});
}

/**
 * Open the Pages child when present. Match Name cell or data-item-name
 * ({@code /^Pages$/} fails on Type+Path columns — #3463).
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} list
 */
async function openPagesFolderIfPresent(page, list) {
  const folderRows = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
  );
  const folderCount = await folderRows.count();
  for (let i = 0; i < folderCount; i += 1) {
    const folder = folderRows.nth(i);
    const text = ((await folder.innerText().catch(() => "")) || "").trim();
    const itemName = (await folder.getAttribute("data-item-name")) || "";
    if (!detailRowHasExactName(text, "Pages") && itemName !== "Pages") {
      continue;
    }
    await openDetailFolderRow(folder);
    return;
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
      const checkInFailures = [];
      page.on("response", (res) => {
        const u = res.url();
        if (
          /\/itemmanagement\/workflow\/checkIn\//i.test(u) &&
          res.status() >= 500
        ) {
          checkInFailures.push(`${res.status()} ${u}`);
        }
      });

      // Use the logged-in page request (session cookies). Isolated
      // APIRequestContext basic-auth is often redirected to login.
      const listed = await findListedPageViaRest(page.request);

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      await openSitesThenPages(page, listed);

      const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
      await expect(list).toBeVisible({ timeout: 15_000 });

      const previewable = list.locator(
        'tbody tr[data-testid^="detail-row-"][data-previewable="true"]',
      );
      const previewableCount = await previewable.count();
      if (
        shouldSkipListedPagePreview({
          listedPage: listed,
          previewableRowCount: previewableCount,
        })
      ) {
        test.skip(true, noListedPageSkipMessage());
        return;
      }

      const itemRows = list.locator(
        'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]',
      );
      let pageRow = previewable.first();
      if (previewableCount === 0) {
        pageRow = itemRows.first();
      }
      if ((await pageRow.count()) === 0) {
        if (!listed) {
          throw new Error(
            "H2 / product route has no previewable Explorer page row — do not skip (#3627)",
          );
        }
        const byName = list
          .locator('tbody tr[data-testid^="detail-row-"]')
          .filter({ hasText: String(listed.name || "") })
          .first();
        if ((await byName.count()) === 0) {
          throw new Error(
            `REST listed page ${listed.name || listed.id} at ${listed.path} ` +
              `but Explorer detail list has no page/item row (H2 / listing on tip — do not skip)`,
          );
        }
        pageRow = byName;
      }

      await pageRow.click({ force: true });
      const preview = page.locator(`[data-testid="${TEST_IDS.preview}"]`);
      await expect(preview).toBeEnabled({ timeout: 10_000 });

      const popupPromise = page.waitForEvent("popup", { timeout: 15_000 });
      const previewResponsePromise = page
        .waitForResponse(
          (res) =>
            isProductPagePreviewUrl(res.url()) &&
            res.request().resourceType() === "document",
          { timeout: 15_000 },
        )
        .catch(() => null);
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
      const previewRes = await previewResponsePromise;
      let previewBody = "";
      if (previewRes) {
        expect(
          previewRes.status(),
          `Preview host ${previewRes.url()} should be HTTP 200`,
        ).toBe(200);
        previewBody = await previewRes.text();
      } else {
        const probe = await page.request.get(popupUrl);
        expect(
          probe.status(),
          `Preview host ${popupUrl} should be HTTP 200`,
        ).toBe(200);
        previewBody = await probe.text();
      }
      if (
        /percmobilepreview=|\/assembler\/render|\/pagemanagement\/render\/page\//i.test(
          popupUrl,
        )
      ) {
        expect(
          isAssembledPreviewHtml(previewBody),
          `Assembled preview must be HTML, not NPE/JSP error; body=${String(previewBody).slice(0, 240)}`,
        ).toBe(true);
      }

      const listedPath = listedPagePreviewCmsPath(listed);
      const siteGet = cmsSitePathPreviewGetUrl(BASE_URL, listedPath);
      if (siteGet) {
        const asm = await page.request.get(siteGet);
        const asmBody = await asm.text();
        expect(
          asm.status(),
          `Site-path assembly ${siteGet} should be HTTP 200; body=${asmBody.slice(0, 240)}`,
        ).toBe(200);
        expect(
          isAssembledPreviewHtml(asmBody),
          `rffHome site-path must assemble HTML not NPE (#3719); body=${asmBody.slice(0, 240)}`,
        ).toBe(true);
      }
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
      expect(
        checkInFailures,
        `checkIn must not 500 for listed page (#3688): ${checkInFailures.join(" | ")}`,
      ).toEqual([]);
    },
  );

  test(
    "numeric checkIn for listed page content id does not 500 (#3688)",
    { tag: ["@explorer-preview-view", "@preview"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const auth = {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
      };
      const listed = await findListedPageViaRest(request);
      const numericIds = [
        "594",
        numericContentIdFromItemId(listed && listed.id),
      ].filter((id, i, all) => id && all.indexOf(id) === i);
      for (const numericId of numericIds) {
        const checkInUrl = `${BASE_URL}${workflowCheckInPath(
          "/Rhythmyx/services",
          numericId,
        )}`;
        const res = await request.get(checkInUrl, { headers: auth });
        const body = (await res.text()).slice(0, 400);
        expect(
          res.status(),
          `GET ${checkInUrl} must not 500 for bare numeric content id; body=${body}`,
        ).toBeLessThan(500);
      }
    },
  );
});
