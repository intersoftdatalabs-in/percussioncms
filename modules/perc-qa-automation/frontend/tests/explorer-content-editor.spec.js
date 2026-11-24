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
 * Preview-first Content Editor host — chrome-less field form, plus
 * Explorer Open/Edit of a selected page on H2 (#3638 / parent #3102).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-content-editor.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS: PREVIEW_IDS,
  explorerEntryUrl,
  encodeCmsRelPath,
  isListedPageRow,
  isPreviewableRow,
  candidateRestFoldersForSites,
  isListedPageKid,
  isRestFolderKid,
  nestedRestFolderRel,
  unwrapPathItems,
  resolveExplorerListPath,
  listedPageSiteNames,
  foldSiteName,
  detailRowHasExactName,
  detailRowHasExactFoldedName,
  pickSiteTreeNodeTestId,
  classifyListSnapshot,
} = require("./helpers/explorer-preview-view");
const {
  TEST_IDS,
  shouldSkipListedPageEditor,
  noListedItemSkipMessage,
  isProductEditorUrl,
  isLeftoverContentEditorUrl,
  isKeywordTrimCrash,
  isEditorStayVisible,
} = require("./helpers/explorer-content-editor");

function editorSpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const params = new URLSearchParams(query.startsWith("?") ? query.slice(1) : query);
  params.set("entry", "editor");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

const PATH_FOLDER = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folder`;
const PATH_PAGED = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/paginatedFolder`;

async function listWaitReady(page) {
  await page.locator(`[data-testid="${PREVIEW_IDS.list}"]`).waitFor({
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
 * Fetch one folder's children and return the listed page when present.
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} folder
 * @returns {Promise<object|null>}
 */
async function fetchListedPageInFolder(request, folder) {
  const kids = await fetchFolderChildren(request, folder);
  return kids.find(isListedPageKid) || null;
}

/**
 * Scan the folder-like children of one folder for a nested listed page.
 * Bounded to the first 12 kids; visited rels go through {@code seen} so
 * the outer walk never re-fetches (#7212 — extracted from
 * {@code findListedPageViaRest}).
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {object[]} kids
 * @param {string} folder
 * @param {Set<string>} seen
 * @returns {Promise<object|null>}
 */
async function findNestedListedPage(request, kids, folder, seen) {
  for (const kid of kids.slice(0, 12)) {
    if (!isRestFolderKid(kid)) continue;
    const nestedRel = nestedRestFolderRel(kid, folder);
    if (!nestedRel || seen.has(nestedRel)) continue;
    seen.add(nestedRel);
    const nestedPage = await fetchListedPageInFolder(request, nestedRel);
    if (nestedPage) return nestedPage;
  }
  return null;
}

/**
 * Walk Sites → site → Pages for a listed page/asset (peer #3627).
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<object|null>}
 */
async function findListedPageViaRest(request) {
  const sites = await fetchFolderChildren(request, "Sites");
  const seen = new Set();
  for (const folder of candidateRestFoldersForSites(sites)) {
    if (!folder || seen.has(folder)) continue;
    seen.add(folder);
    const kids = await fetchFolderChildren(request, folder);
    const page = kids.find(isListedPageKid) || null;
    if (page) return page;
    const nestedPage = await findNestedListedPage(request, kids, folder, seen);
    if (nestedPage) return nestedPage;
  }
  return null;
}

/**
 * Wait for the detail list to navigate away from a previous folder (#7193).
 * After a tree/folder click React may briefly keep rendering the old rows
 * (settled, wrong folder) or a {@code detail-list-loading} placeholder;
 * acting on either lands the test in the wrong directory. The list root
 * carries {@code data-folder-path}, which flips on navigation before the
 * rows arrive. Bounded and non-fatal: when the click re-opens the folder
 * that is already listed the path never changes and the caller falls
 * through to {@code listSettled} on already-correct rows.
 * @param {import("@playwright/test").Page} page
 * @param {string|null} before data-folder-path captured before the click
 * @param {number} [timeout]
 */
async function awaitListPathChange(page, before, timeout = 15_000) {
  if (before == null) return;
  const list = page.locator(`[data-testid="${PREVIEW_IDS.list}"]`);
  // Non-fatal by design: a re-open of the already-listed folder never
  // changes path, and the caller falls through to listSettled (#7193).
  await expect
    .poll(
      async () =>
        ((await list.getAttribute("data-folder-path").catch(() => null)) ===
        before
          ? "same"
          : "changed"),
      { timeout },
    )
    .toMatch(/changed/)
    .then(
      () => true,
      () => false,
    );
}

/**
 * One in-page snapshot of the detail list's rows (#7193). A single
 * evaluate() roundtrip: per-row getAttribute / textContent calls hang
 * without an inner timeout when a row never reaches actionability-stable
 * (churning layout), wedging the whole poll budget inside the first
 * iteration. evaluate() has no actionability waits, so it always resolves.
 * @param {import("@playwright/test").Locator} list
 * @returns {Promise<{loading?: boolean, error?: boolean, rows?: Array<{named: boolean, texted: boolean}>}|null>}
 */
async function snapshotDetailList(list) {
  return list
    .evaluate((el) => {
      if (el.querySelector('[data-testid="detail-list-loading"]')) {
        return { loading: true };
      }
      if (el.querySelector('[data-testid="detail-list-error"]')) {
        return { error: true };
      }
      const rows = [
        ...el.querySelectorAll('tbody tr[data-testid^="detail-row-"]'),
      ];
      return {
        rows: rows.map((tr) => ({
          named: Boolean((tr.getAttribute("data-item-name") || "").trim()),
          texted: Boolean((tr.textContent || "").trim()),
        })),
      };
    })
    .catch(() => null);
}

/**
 * Snapshot the list's current rows signature (#7193): joined row testids,
 * or a marker for the loading/error/empty states.
 * @param {import("@playwright/test").Page} page
 * @param {string} listSel
 * @returns {Promise<string>}
 */
async function listRowsSignature(page, listSel) {
  return page
    .locator(listSel)
    .evaluate((el) => {
      if (el.querySelector('[data-testid="detail-list-loading"]')) {
        return "loading";
      }
      if (el.querySelector('[data-testid="detail-list-error"]')) {
        return "error";
      }
      return [
        ...el.querySelectorAll('tbody tr[data-testid^="detail-row-"]'),
      ]
        .map((tr) => tr.getAttribute("data-testid"))
        .join("|");
    })
    .catch(() => "detached");
}

/**
 * Wait for the detail list to show different rows after a navigation click
 * (#7193 P2). DetailList retains the previous folder's children while the
 * destination fetch runs (a loading placeholder only renders when there
 * are no children at all), so a freshly-flipped data-folder-path with old
 * rows still reads "settled". Bounded and non-fatal: falls through to
 * {@code listSettled} on timeout (re-open of the same folder, or an empty
 * destination whose signature legitimately matches).
 * @param {import("@playwright/test").Page} page
 * @param {string} listSel
 * @param {string} beforeSig rows signature captured before the click
 * @param {number} [timeout]
 */
async function awaitListRowsChanged(page, listSel, beforeSig, timeout = 30_000) {
  await expect
    .poll(async () => {
      const state = await listRowsSignature(page, listSel);
      if (state === "loading" || state === "detached") return "waiting";
      return state === beforeSig ? "same" : "changed";
    }, { timeout })
    .toMatch(/changed/)
    .then(
      () => true,
      () => false,
    );
}

/**
 * element persists across SPA navigations (React re-render), so bare
 * "attached" waits pass while skeleton rows are still loading; acting on
 * those transient rows then hangs until the test timeout. A settled list
 * has zero rows (genuinely empty folder) or every row backed by a real
 * item. Settle-ness reads the {@code data-item-name} attribute (always
 * rendered by DetailList) plus {@code textContent}: {@code innerText} is
 * visibility-dependent and stays empty on hidden/duplicate list nodes,
 * which wedged the first version of this helper in a 60s poll loop. The
 * default 60s budget tolerates slow H2 cells under suite load (#7193:
 * the right-click test's Pages listing needed well over 15s once); the
 * happy path resolves on the first settled poll.
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} list
 * @param {number} [timeout]
 */
async function listSettled(page, list, timeout = 60_000) {
  await list.waitFor({ state: "attached", timeout });
  await expect
    .poll(async () => classifyListSnapshot(await snapshotDetailList(list)), {
      timeout,
    })
    .toMatch(/^(settled|empty)$/);
}

/**
 * True when the detail list already shows site content: page/item rows,
 * or a Pages folder row (#7193). Site roots list their Pages chrome
 * directly after the tree click (#3696), so the caller must skip the
 * site-row open instead of navigating away.
 * @param {import("@playwright/test").Locator} list
 * @returns {Promise<boolean>}
 */
async function listShowsSiteContent(list) {
  const items = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]',
  );
  if ((await items.count().catch(() => 0)) > 0) return true;
  const folders = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
  );
  const n = await folders.count().catch(() => 0);
  for (let i = 0; i < n; i += 1) {
    const name = await folders.nth(i).getAttribute("data-item-name").catch(() => "");
    if (String(name || "") === "Pages") return true;
  }
  return false;
}

/**
 * Prefer the folder-icon open control (peer #3328); fall back to dblclick.
 * @param {import("@playwright/test").Locator} row
 */
async function openDetailFolderRow(row) {
  const icon = row.locator('[data-testid^="detail-folder-icon-"]').first();
  const page = row.page();
  const listSel = `[data-testid="${PREVIEW_IDS.list}"]`;
  const before = await page
    .locator(listSel)
    .getAttribute("data-folder-path")
    .catch(() => null);
  const beforeSig = await listRowsSignature(page, listSel);
  try {
    await icon.waitFor({ state: "attached", timeout: 5_000 });
    // Bounded: an unactionable (stale/disabled) icon must fall through to
    // dblclick instead of burning the whole test timeout (#7193).
    await icon.click({ timeout: 10_000 });
  } catch {
    await row.dblclick({ force: true, timeout: 10_000 });
  }
  await listWaitReady(page);
  // Destination sync (#7193 P2): the click may leave the old folder's rows
  // rendered while the fetch runs; wait for the folder path to flip and
  // the rows to turn over before judging them, or a stale "settled" list
  // sends the test elsewhere.
  await awaitListPathChange(page, before);
  await awaitListRowsChanged(page, listSel, beforeSig);
  await listSettled(page, page.locator(listSel));
}

/**
 * Open the Pages child when present. Match the Name cell (#3463).
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} list
 */
async function openPagesFolderIfPresent(page, list) {
  await listSettled(page, list);
  // Exact data-item-name match (#7193): the old hasText("Pages") filter
  // also matched item rows whose titles merely contain "Pages".
  const folders = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
  );
  const n = await folders.count().catch(() => 0);
  let pagesRow = null;
  for (let i = 0; i < n; i += 1) {
    const row = folders.nth(i);
    const name = await row.getAttribute("data-item-name").catch(() => null);
    if (String(name || "") === "Pages") {
      pagesRow = row;
      break;
    }
  }
  if (!pagesRow) return;
  await openDetailFolderRow(pagesRow);
}

/**
 * Expand the Sites tree node and return the site-child node locator
 * (#7212 — extracted from {@code openSitesThenPages}).
 * @param {import("@playwright/test").Page} page
 * @param {import("@playwright/test").Locator} tree
 * @returns {Promise<import("@playwright/test").Locator>}
 */
async function expandSitesTreeNodes(page, tree) {
  const sitesNode = tree
    .locator(
      '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"]',
    )
    .first();
  await expect(sitesNode).toBeVisible({ timeout: 15_000 });
  await sitesNode.click({ force: true });
  await listWaitReady(page);

  const toggle = sitesNode.locator('[aria-hidden="true"]').first();
  if ((await toggle.count()) > 0) {
    await toggle.click();
  }
  const siteTreeNodes = tree.locator(
    '[data-testid^="tree-node-/Sites/"]:not([data-testid="tree-node-/Sites/"])',
  );
  await expect(siteTreeNodes.first()).toBeVisible({ timeout: 15_000 });
  return siteTreeNodes;
}

/**
 * Click the site tree node matching the REST-listed site names, or the
 * first site when nothing matches (#7212 — extracted from
 * {@code openSitesThenPages}).
 * @param {import("@playwright/test").Locator} tree
 * @param {import("@playwright/test").Locator} siteTreeNodes
 * @param {Set<string>} wanted
 * @returns {Promise<void>}
 */
async function clickPickedSiteNode(tree, siteTreeNodes, wanted) {
  const siteCount = await siteTreeNodes.count();
  const treeTestids = [];
  for (let i = 0; i < siteCount; i += 1) {
    treeTestids.push(
      await siteTreeNodes.nth(i).getAttribute("data-testid").catch(() => ""),
    );
  }
  const pickedTestid = pickSiteTreeNodeTestId(treeTestids, wanted);
  if (pickedTestid) {
    await tree
      .locator(`[data-testid="${pickedTestid}"]`)
      .first()
      .click({ force: true });
  } else if (siteCount > 0) {
    await siteTreeNodes.first().click({ force: true });
  }
}

/**
 * Open the site-root folder row when the tree click did not already list
 * site content (#3696 / #7193 — extracted from {@code openSitesThenPages}).
 * The list already shows site content — e.g. the Pages chrome right after
 * the tree click — when it has item rows or a Pages folder row; only then
 * is the site-row open skipped. The old substring hint otherwise
 * false-matches child folders whose names contain the site name
 * (AboutCorporateInvestments for CorporateInvestments) and navigates away
 * from Pages (#7193).
 * @param {import("@playwright/test").Locator} list
 * @param {Set<string>} wanted
 * @returns {Promise<void>}
 */
async function openSiteRowIfNeeded(list, wanted) {
  if (await listShowsSiteContent(list)) return;
  const siteRows = list.locator(
    'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
  );
  const rowCount = await siteRows.count().catch(() => 0);
  let siteRow = null;
  for (let i = 0; i < rowCount; i += 1) {
    const row = siteRows.nth(i);
    const itemName = await row.getAttribute("data-item-name").catch(() => null);
    if (
      itemName &&
      wanted.size > 0 &&
      detailRowHasExactFoldedName(itemName, wanted)
    ) {
      siteRow = row;
      break;
    }
  }
  if (!siteRow && wanted.size === 0 && rowCount > 0) {
    siteRow = siteRows.first();
  }
  if (siteRow) {
    try {
      await openDetailFolderRow(siteRow);
    } catch {
      // Tree click may already have opened the site listing.
    }
  }
}

/**
 * Wait for an item row matching the REST-listed page; throw (do not skip)
 * when the REST hunt found a page the list never shows (#7212 — extracted
 * from {@code openSitesThenPages}).
 * @param {import("@playwright/test").Locator} list
 * @param {object} [listed]
 * @param {Set<string>} wanted
 * @returns {Promise<void>}
 */
async function awaitListedCandidateRow(list, listed, wanted) {
  const listedName = listed && listed.name ? String(listed.name) : "";
  const candidates = [
    list.locator('tbody tr[data-testid^="detail-row-"][data-row-kind="item"]').first(),
    list
      .locator('tbody tr[data-testid^="detail-row-"][data-previewable="true"]')
      .first(),
  ];
  if (listedName) {
    candidates.push(
      list
        .locator('tbody tr[data-testid^="detail-row-"]')
        .filter({ hasText: listedName })
        .first(),
    );
  }
  if (/\bHome\b/i.test(listedName)) {
    candidates.push(
      list
        .locator('tbody tr[data-testid^="detail-row-"]')
        .filter({ hasText: "Home" })
        .first(),
    );
  }
  for (const loc of candidates) {
    try {
      await loc.waitFor({ state: "attached", timeout: 8_000 });
      return;
    } catch {
      // try the next locator
    }
  }
  if (wanted.size > 0 && listed) {
    throw new Error(
      `REST listed page ${listedName || listed.id} but Explorer list has no ` +
        `item row after opening site among ${[...wanted].join(", ")} — do not skip`,
    );
  }
}

/**
 * Open Sites → owning site → Pages when present (peer #3627).
 * @param {import("@playwright/test").Page} page
 * @param {object} [listed]
 */
async function openSitesThenPages(page, listed) {
  const tree = page.locator(`[data-testid="${PREVIEW_IDS.tree}"]`);
  const listSel = `[data-testid="${PREVIEW_IDS.list}"]`;
  const siteTreeNodes = await expandSitesTreeNodes(page, tree);

  // Exact site-root pick (#7193): a substring match on the full testid
  // false-matches nested folders whose paths contain the site name
  // (AboutCorporateInvestments for CorporateInvestments) and navigates
  // away from the site's Pages listing.
  const wanted = new Set(listedPageSiteNames(listed).map((n) => foldSiteName(n)));
  const beforeSitePath = await page
    .locator(listSel)
    .getAttribute("data-folder-path")
    .catch(() => null);
  const beforeSiteSig = await listRowsSignature(page, listSel);
  await clickPickedSiteNode(tree, siteTreeNodes, wanted);
  await listWaitReady(page);
  // Destination sync: don't judge the rows until the list navigated away
  // from the pre-click folder and turned its rows over (#7193 P2).
  await awaitListPathChange(page, beforeSitePath);
  await awaitListRowsChanged(page, listSel, beforeSiteSig);

  const list = page.locator(listSel);
  await listSettled(page, list);
  await openSiteRowIfNeeded(list, wanted);

  await openPagesFolderIfPresent(page, list);
  await awaitListedCandidateRow(list, listed, wanted);
}

test.describe("modern React Content Editor — first slice", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
  });

  test(
    "editor entry is chrome-less and does not open CM1 editor",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("checkoutedit.xml") ||
          u.includes("contenteditorurls.html") ||
          u.includes("sys_ceSupport") ||
          /view=editor/.test(u)
        ) {
          blocked.push(u);
        }
      });

      await page.goto(editorSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="editor-host"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-overlay"]')).toBeVisible();
      await expect(page.locator('[data-testid="perc-spa-app"]')).toHaveCount(0);
      // Bare entry=editor renders the create panel ("asks for an item"),
      // not editor-error: editor-error is reserved for load failures and
      // linkback warnings (#7193; EditorHost "asks for an item when
      // contentId is missing" Vitest pin).
      await expect(page.locator('[data-testid="editor-create-panel"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="editor-create-empty"]')).toBeVisible();
      expect(blocked, `CM1 / Data Flow editor must not be requested: ${blocked.join(" ")}`).toEqual(
        [],
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="editor-host"]',
      });
    },
  );

  test(
    "Explorer Edit does not open CM1 ?view=editor",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        if (isLeftoverContentEditorUrl(req.url())) {
          blocked.push(req.url());
        }
      });
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 20_000,
      });
      const edit = page.locator('[data-testid="action-toolbar-item-Edit"]');
      if (await edit.isVisible()) {
        await edit.click();
      }
      expect(blocked, `CM1 editor must not be requested: ${blocked.join(" ")}`).toEqual([]);
    },
  );

  test(
    "Explorer New Item does not open leftover Content Editor HTML",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("rx_ce") ||
          u.includes("contenteditorurls.html") ||
          u.includes("checkoutedit.xml")
        ) {
          blocked.push(u);
        }
      });
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
        timeout: 20_000,
      });
      const neu = page.locator('[data-testid="action-toolbar-item-New"]');
      if (await neu.isVisible()) {
        await neu.click();
      }
      expect(blocked, `Data Flow CE HTML must not be requested: ${blocked.join(" ")}`).toEqual([]);
    },
  );

  async function openPercPageTemplatePicker(page, createBodies) {
    await page.route("**/services/contenttypes/**", async (route) => {
      const url = route.request().url();
      if (!/percPage|\/Page(?:\?|$)/i.test(url)) {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ContentTypeDetail: {
            name: "percPage",
            allowedTemplates: [
              { name: "t1", label: "Home", guid: { stringValue: "tpl-home" } },
              { name: "t2", label: "Interior", guid: { stringValue: "tpl-in" } },
            ],
          },
        }),
      });
    });
    await page.route("**/services/itemmanagement/item/create**", async (route) => {
      createBodies.push(route.request().postData() || "");
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemCreateResult: {
            itemId: "1-101-1",
            folderPath: "//Sites/Demo",
            name: "New-percPage.html",
            contentType: "percPage",
          },
        }),
      });
    });

    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
    await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
      timeout: 20_000,
    });

    const tree = page.locator('[data-testid="explorer-tree"]');
    if ((await tree.count()) > 0) {
      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
        )
        .first();
      if ((await sitesNode.count()) > 0) {
        // Best-effort focus: the Sites node may already be selected or may
        // detach during tree refresh; either rejection is safe to ignore.
        // mkd-code-review[patterns.hard_gate]: adjudicated intentional tests.empty-catch (issue #7211) — optional Sites node focus; rejection means already selected or detached.
        await sitesNode.click({ force: true, timeout: 10_000 }).catch(() => {});
      }
    }

    const neu = page.locator('[data-testid="action-toolbar-item-New"]');
    if ((await neu.count()) === 0 || !(await neu.isVisible())) {
      return { skipped: "New menu is not visible" };
    }
    await neu.click();
    const percPage = page.locator(
      '[data-testid="action-toolbar-item-percPage"], [data-testid="action-toolbar-item-Page"]',
    );
    if ((await percPage.count()) === 0) {
      return { skipped: "percPage is not listed under New" };
    }
    await percPage.first().click();
    const picker = page.locator('[data-testid="explorer-template-picker"]');
    await expect(picker).toBeVisible({ timeout: 10_000 });
    return { picker };
  }

  test(
    "Explorer New percPage offers a template picker and does not create on cancel",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const createBodies = [];
      const opened = await openPercPageTemplatePicker(page, createBodies);
      if (opened.skipped) {
        test.skip(true, opened.skipped);
        return;
      }
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-template-picker"]',
      });
      await page.locator('[data-testid="explorer-template-picker-cancel"]').click();
      await expect(opened.picker).toHaveCount(0);
      expect(createBodies, "Cancel must not POST create").toEqual([]);
    },
  );

  test(
    "Explorer New percPage posts the picked templateId",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      const createBodies = [];
      const opened = await openPercPageTemplatePicker(page, createBodies);
      if (opened.skipped) {
        test.skip(true, opened.skipped);
        return;
      }
      await page.locator('[data-testid="explorer-template-picker-select"]').selectOption("tpl-in");
      await page.locator('[data-testid="explorer-template-picker-ok"]').click();
      await expect.poll(() => createBodies.length).toBe(1);
      expect(createBodies[0]).toMatch(/"templateId"\s*:\s*"tpl-in"/);
    },
  );

  test(
    "Open/Edit selected page lands React editor; folders stay non-editable",
    { tag: ["@explorer-content-editor", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      const leftover = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("request", (req) => {
        if (isLeftoverContentEditorUrl(req.url())) {
          leftover.push(req.url());
        }
      });

      await page.goto(explorerEntryUrl(BASE_URL, { cacheBuster: Date.now() }));

      const listed = await findListedPageViaRest(page.request);

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });
      await expect(page.locator(`[data-testid="${TEST_IDS.reducedActions}"]`)).toBeVisible();
      await listWaitReady(page);

      await openSitesThenPages(page, listed);

      const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
      await expect(list).toBeVisible({ timeout: 15_000 });

      const listedName = listed && listed.name ? String(listed.name) : "";
      let pageRow = listedName
        ? list
            .locator('tbody tr[data-testid^="detail-row-"]')
            .filter({ hasText: listedName })
            .first()
        : list
            .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="item"]')
            .first();
      try {
        await pageRow.waitFor({ state: "attached", timeout: 5_000 });
      } catch {
        pageRow = list
          .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="item"]')
          .first();
        try {
          await pageRow.waitFor({ state: "attached", timeout: 5_000 });
        } catch {
          if (
            shouldSkipListedPageEditor({
              listedPage: listed,
              itemRowCount: 0,
            })
          ) {
            test.skip(true, noListedItemSkipMessage());
            return;
          }
          throw new Error(
            `REST listed page ${listedName || (listed && listed.id) || "unknown"} ` +
              `but Explorer detail list has no page/item row (H2 — do not skip)`,
          );
        }
      }

      await pageRow.click({ force: true });
      const openBtn = page.locator(`[data-testid="${TEST_IDS.open}"]`);
      await expect(openBtn).toBeEnabled({ timeout: 10_000 });

      const popupPromise = page.waitForEvent("popup", { timeout: 15_000 });
      const editorDocPromise = page
        .waitForResponse(
          (res) =>
            isProductEditorUrl(res.url()) &&
            res.request().resourceType() === "document",
          { timeout: 15_000 },
        )
        .catch(() => null);
      await openBtn.click();
      let popup = await popupPromise.catch(() => null);
      if (!popup) {
        const edit = page.locator(`[data-testid="${TEST_IDS.edit}"]`);
        await expect(edit).toBeVisible({ timeout: 10_000 });
        const editPopup = page.waitForEvent("popup", { timeout: 15_000 });
        await edit.click();
        popup = await editPopup;
      }
      const editorDoc = await editorDocPromise;
      if (editorDoc) {
        expect(
          editorDoc.status(),
          `React editor document should be HTTP 200; got ${editorDoc.status()} ${editorDoc.url()}`,
        ).toBe(200);
      }

      let popupUrl = popup ? popup.url() : "";
      if (popup && (!popupUrl || /about:blank/i.test(popupUrl))) {
        // Best-effort settle wait: the popup may finish loading (or close)
        // before the wait attaches; the URL assertion below is the hard gate.
        // mkd-code-review[patterns.hard_gate]: adjudicated intentional tests.empty-catch (issue #7211) — optional popup load-state wait; rejection means already settled or closed.
        await popup.waitForLoadState("domcontentloaded").catch(() => {});
        popupUrl = popup.url();
      }
      expect(
        isProductEditorUrl(popupUrl),
        `Open/Edit popup URL should be spa.jsp?entry=editor; got ${popupUrl}`,
      ).toBe(true);
      if (popup && !popup.isClosed()) {
        await expect(
          popup.locator(`[data-testid="${TEST_IDS.editorHost}"]`),
        ).toBeVisible({ timeout: 20_000 });
        // Best-effort cleanup: the popup may already be closed by the app;
        // rejection means there is nothing left to close.
        // mkd-code-review[patterns.hard_gate]: adjudicated intentional tests.empty-catch (issue #7211) — popup cleanup close; rejection means already closed.
        await popup.close().catch(() => {});
      }

      const selectableFolder = list
        .locator(
          'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
        )
        .first();
      let folderAttached = false;
      try {
        await selectableFolder.waitFor({ state: "attached", timeout: 2_000 });
        folderAttached = true;
      } catch {
        folderAttached = false;
      }
      if (folderAttached) {
        await selectableFolder.click({ force: true });
        await expect(page.locator(`[data-testid="${TEST_IDS.edit}"]`)).toHaveCount(
          0,
          { timeout: 5_000 },
        );
        const folderPopup = page
          .waitForEvent("popup", { timeout: 2_000 })
          .then((win) => win)
          .catch(() => null);
        await openBtn.click();
        const maybeEditor = await folderPopup;
        if (maybeEditor && !maybeEditor.isClosed()) {
          const folderUrl = maybeEditor.url();
          expect(
            isProductEditorUrl(folderUrl),
            `Folder Open must not land the React editor; got ${folderUrl}`,
          ).toBe(false);
          // Best-effort cleanup: the folder popup may already be closed by
          // the app; rejection means there is nothing left to close.
          // mkd-code-review[patterns.hard_gate]: adjudicated intentional tests.empty-catch (issue #7211) — folder popup cleanup close; rejection means already closed.
          await maybeEditor.close().catch(() => {});
        }
      }

      expect(
        leftover,
        `Data Flow CE HTML must not be requested: ${leftover.join(" ")}`,
      ).toEqual([]);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Explorer right-click Edit stays on the React editor (#3968)",
    { tag: ["@explorer-content-editor", "@explorer"], timeout: 180_000 },
    async ({ page }) => {
      test.setTimeout(180_000);
      const pageErrors = [];
      const leftover = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("request", (req) => {
        if (isLeftoverContentEditorUrl(req.url())) {
          leftover.push(req.url());
        }
      });

      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "domcontentloaded" });

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });
      await listWaitReady(page);

      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"]',
        )
        .first();
      await expect(sitesNode).toBeVisible({ timeout: 15_000 });
      await sitesNode.click({ force: true });
      await listWaitReady(page);

      const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
      await expect(list).toBeVisible({ timeout: 15_000 });
      const siteFolder = list
        .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]')
        .first();
      await expect(siteFolder).toBeVisible({ timeout: 15_000 });
      await openDetailFolderRow(siteFolder);
      await openPagesFolderIfPresent(page, list);

      const itemRows = list.locator(
        'tbody tr[data-testid^="detail-row-"]:not([data-row-kind="folder"])',
      );
      if (!(await itemRows.first().isVisible().catch(() => false))) {
        const nestedFolder = list
          .locator('tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]')
          .first();
        if (await nestedFolder.isVisible().catch(() => false)) {
          await openDetailFolderRow(nestedFolder);
        }
      }
      const pageRow = itemRows.first();
      await expect(pageRow).toBeVisible({ timeout: 20_000 });

      await pageRow.click({ button: "right", force: true });
      const contextMenu = page.locator('[data-testid="context-menu"]');
      // Best-effort visibility probe: the context menu may not appear on this
      // build; the contextEdit/toolbarEdit branch below is the hard gate.
      // mkd-code-review[patterns.hard_gate]: adjudicated intentional tests.empty-catch (issue #7211) — optional context-menu wait; rejection falls through to toolbar Edit.
      await contextMenu.waitFor({ state: "visible", timeout: 5_000 }).catch(() => {});
      const contextEdit = page.locator(`[data-testid="${TEST_IDS.contextEdit}"]`);
      const toolbarEdit = page.locator(`[data-testid="${TEST_IDS.edit}"]`);
      const popupPromise = page.waitForEvent("popup", { timeout: 20_000 });
      if (await contextEdit.isVisible().catch(() => false)) {
        await contextEdit.click();
      } else {
        await pageRow.click({ force: true });
        await expect(toolbarEdit).toBeVisible({ timeout: 10_000 });
        await toolbarEdit.click();
      }
      const popup = await popupPromise;
      popup.on("pageerror", (err) => pageErrors.push(String(err)));
      popup.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      // Best-effort settle wait: the Edit popup may finish loading (or close)
      // before the wait attaches; the product-editor URL assertion below is
      // the hard gate.
      // mkd-code-review[patterns.hard_gate]: adjudicated intentional tests.empty-catch (issue #7211) — optional popup load-state wait; rejection means already settled or closed.
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      let popupUrl = popup.url();
      if (!popupUrl || /about:blank/i.test(popupUrl)) {
        // Best-effort re-wait for a slow blank popup; the URL assertion below
        // still gates on the product editor URL.
        // mkd-code-review[patterns.hard_gate]: adjudicated intentional tests.empty-catch (issue #7211) — optional blank-popup re-wait; rejection means already settled or closed.
        await popup.waitForLoadState("domcontentloaded").catch(() => {});
        popupUrl = popup.url();
      }
      expect(
        isProductEditorUrl(popupUrl),
        `right-click Edit popup URL should be spa.jsp?entry=editor; got ${popupUrl}`,
      ).toBe(true);

      const host = popup.locator(`[data-testid="${TEST_IDS.editorHost}"]`);
      const overlay = popup.locator(`[data-testid="${TEST_IDS.editorOverlay}"]`);
      await expect(host).toBeVisible({ timeout: 20_000 });
      await expect(overlay).toBeVisible({ timeout: 10_000 });

      const form = popup.locator(`[data-testid="${TEST_IDS.editorForm}"]`);
      const editorError = popup.locator(`[data-testid="${TEST_IDS.editorError}"]`);
      const loading = popup.locator(`[data-testid="${TEST_IDS.editorLoading}"]`);
      const empty = popup.locator(`[data-testid="${TEST_IDS.editorEmpty}"]`);
      await expect(form.or(editorError).or(loading).or(empty)).toBeVisible({
        timeout: 30_000,
      });

      const stay = isEditorStayVisible({
        host: await host.isVisible(),
        overlay: await overlay.isVisible(),
        form: await form.isVisible().catch(() => false),
        error: await editorError.isVisible().catch(() => false),
        loading: await loading.isVisible().catch(() => false),
        empty: await empty.isVisible().catch(() => false),
      });
      expect(stay, "Edit must stay on the editor host, not a blank page").toBe(true);

      const trimCrashes = pageErrors.filter((msg) => isKeywordTrimCrash(msg));
      expect(
        trimCrashes,
        `KeywordFieldWidget .trim crash: ${trimCrashes.join(" | ")}`,
      ).toEqual([]);
      expect(
        leftover,
        `Data Flow CE HTML must not be requested: ${leftover.join(" ")}`,
      ).toEqual([]);

      if (popup && !popup.isClosed()) {
        // Best-effort cleanup: the popup may close itself between the guard
        // and the close call; rejection means there is nothing left to close.
        // mkd-code-review[patterns.hard_gate]: adjudicated intentional tests.empty-catch (issue #7211) — popup cleanup close; rejection means already closed.
        await popup.close().catch(() => {});
      }
    },
  );
});
