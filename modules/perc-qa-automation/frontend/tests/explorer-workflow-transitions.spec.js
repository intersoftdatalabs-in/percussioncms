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
 * Playwright surface: #3639 / parent #3102 / #2732 / #3684 — Explorer
 * Workflow transition no-skip on H2.
 *
 * <p>Selecting a content row on {@code spa.jsp?entry=explorer} must show
 * {@code action-toolbar-group-workflow} and an invokable
 * {@code workflow-transition:*} control. Folder rows must not show
 * Workflow transitions. Do not fixture-skip when REST lists an eligible
 * item, or on H2 QA (demo-sites default).</p>
 *
 * <p>Tags: {@code @explorer-workflow} {@code @workflow} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-workflow-transitions.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect, errors } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerEntryUrl,
  workflowTransitionsUrl,
  unwrapItemStateTransition,
  listedItemContentId,
  isWorkflowEligibleRow,
  shouldSkipWorkflowTransitionProof,
  noEligibleItemSkipMessage,
  h2MissingEligibleMessage,
  isHonestTransitionStatus,
  isWorkflowTransitionInvokeUrl,
  JSON_ACCEPT_HEADERS,
} = require("./helpers/explorer-workflow-transitions");
const {
  isListedPageRow,
  unwrapPathItems,
  resolveExplorerListPath,
  listedPageSiteNames,
  foldSiteName,
  detailRowHasExactName,
  detailRowMatchesFoldedSite,
  treeNodeMatchesFoldedSite,
  isExplorerSiteRootTestId,
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
  const rel = String(cmsPath || "")
    .replace(/^\/+/, "")
    .replace(/\/+$/, "");
  const paged = await request.get(
    `${PATH_PAGED}/${rel}?startIndex=0&maxResults=50`,
    { headers: JSON_ACCEPT_HEADERS },
  );
  if (paged.status() === 200) {
    return unwrapPathItems(await paged.json());
  }
  const folder = await request.get(`${PATH_FOLDER}/${rel}`, {
    headers: JSON_ACCEPT_HEADERS,
  });
  if (folder.status() === 200) {
    return unwrapPathItems(await folder.json());
  }
  return [];
}

/**
 * Walk Sites → site → Pages (and one more folder level) for listed pages.
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<object[]>}
 */
async function listPagesViaRest(request) {
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
  const pages = [];
  const seenIds = new Set();
  for (const folder of candidateFolders) {
    if (!folder || seen.has(folder)) continue;
    seen.add(folder);
    const kids = await fetchFolderChildren(request, folder);
    const consider = (kid) => {
      if (!isListedPageRow(kid) || !isWorkflowEligibleRow(kid)) return;
      const id = listedItemContentId(kid);
      const key = id || `${kid.path || ""}|${kid.name || ""}`;
      if (seenIds.has(key)) return;
      seenIds.add(key);
      pages.push(kid);
    };
    kids.forEach(consider);
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
      nested.forEach(consider);
    }
  }
  return pages;
}

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {object} listed
 * @returns {Promise<{triggers: string[], stateName?: string}>}
 */
async function fetchTransitions(request, listed) {
  const id = listedItemContentId(listed);
  if (!id) {
    return { triggers: [] };
  }
  const res = await request.get(workflowTransitionsUrl(BASE_URL, id), {
    headers: JSON_ACCEPT_HEADERS,
  });
  if (res.status() !== 200) {
    return { triggers: [] };
  }
  const body = unwrapItemStateTransition(await res.json().catch(() => null));
  return { triggers: body.transitionTriggers || [], stateName: body.stateName };
}

/**
 * Prefer a listed page whose getTransitions payload has at least one trigger.
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<{item: object, triggers: string[]}|null>}
 */
async function findEligibleWorkflowItemViaRest(request) {
  const pages = await listPagesViaRest(request);
  let fallback = null;
  for (const page of pages) {
    const trans = await fetchTransitions(request, page);
    if (!fallback) {
      fallback = { item: page, triggers: trans.triggers };
    }
    if (trans.triggers.length > 0) {
      return { item: page, triggers: trans.triggers };
    }
  }
  return fallback;
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
 * (peer #3575 / #3684). GUID path testids still match when
 * {@code data-node-name} / {@code data-folder-path} / visible label fold
 * to Corporate_Investments. If names do not match, try every site node
 * until the listed page (or any item row) is visible — FastForward has
 * two sample sites.
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

  // GUID-only tree labels: still open each sample site until the listed
  // page appears (peer explorer-content-editor, #3684 Cycle Verify).
  for (const testid of allSiteTestIds) {
    if (matchingTestIds.includes(testid)) continue;
    const node = tree.locator(`[data-testid="${testid}"]`).first();
    if (await trySiteNode(node)) {
      return;
    }
  }

  throw new Error(
    `REST listed page ${listedName || (listed && listed.id)} but UI did ` +
      `not open a matching site among ${[...wanted].join(", ") || "none"} ` +
      `(tree=${seen.join("; ") || "none"})`,
  );
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
 * Open the Pages child when present.
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
    if (!detailRowHasExactName(text, "Pages")) continue;
    await openDetailFolderRow(folder);
    return;
  }
}

function attachConsoleGuard(page, bucket, transitionStatuses) {
  page.on("pageerror", (err) => {
    bucket.push(String(err && err.message ? err.message : err));
  });
  page.on("response", (res) => {
    if (isWorkflowTransitionInvokeUrl(res.url())) {
      transitionStatuses.push(res.status());
    }
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") return;
    const text = msg.text();
    if (
      /Failed to load resource: the server responded with a status of (404|400)/i.test(
        text,
      )
    ) {
      return;
    }
    bucket.push(text);
  });
}

test.describe("modern React Content Explorer - workflow transitions (#3639 / #2732)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(explorerEntryUrl(BASE_URL, { cacheBuster: Date.now() }));
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts server action toolbar (workflow surface host)",
    { tag: ["@explorer-workflow", "@workflow", "@smoke"] },
    async ({ page }) => {
      const pageErrors = [];
      attachConsoleGuard(page, pageErrors, []);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible({
        timeout: 15_000,
      });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.serverActions}"]`),
      ).toBeVisible();
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.shell}"]`,
      });
      expect(pageErrors, `JS page/console errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "selecting a content item shows invokable Workflow transitions; folders do not",
    { tag: ["@explorer-workflow", "@workflow"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      const transitionStatuses = [];
      attachConsoleGuard(page, pageErrors, transitionStatuses);

      const found = await findEligibleWorkflowItemViaRest(page.request);
      const listed = found && found.item ? found.item : null;
      const restEligibleCount =
        listed && (found.triggers || []).length > 0 ? 1 : 0;

      if (
        shouldSkipWorkflowTransitionProof({
          restEligibleCount,
          dbType: process.env.TEST_DB_TYPE,
        })
      ) {
        test.skip(true, noEligibleItemSkipMessage());
        return;
      }

      if (!listed || restEligibleCount === 0) {
        throw new Error(h2MissingEligibleMessage());
      }

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      await openSitesThenPages(page, listed);

      const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
      await expect(list).toBeVisible({ timeout: 15_000 });

      const itemRows = list.locator(
        'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]',
      );
      const listedName = listed.name ? String(listed.name) : "";
      let pageRow = itemRows.first();
      if (listedName) {
        const byName = list
          .locator(
            'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]',
          )
          .filter({ hasText: listedName });
        if ((await byName.count()) > 0) {
          pageRow = byName.first();
        }
      }
      if ((await pageRow.count()) === 0) {
        throw new Error(
          `REST listed page ${listedName || listed.id} at ${listed.path} ` +
            `but Explorer detail list has no item row — do not skip (#3639)`,
        );
      }

      const folderRows = list.locator(
        'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
      );
      if ((await folderRows.count()) > 0) {
        await folderRows.first().click({ force: true });
        await expect(
          page.locator(`[data-testid="${TEST_IDS.workflowGroup}"]`),
        ).toHaveCount(0, { timeout: 10_000 });
      }

      await pageRow.click({ force: true });

      const group = page.locator(`[data-testid="${TEST_IDS.workflowGroup}"]`);
      await expect(
        group,
        "content item row must show action-toolbar-group-workflow (#3639)",
      ).toBeVisible({ timeout: 15_000 });

      const transitionBtn = page
        .locator(`[data-testid^="${TEST_IDS.workflowTransitionPrefix}"]`)
        .first();
      await expect(
        transitionBtn,
        "Workflow group must include a workflow-transition:* control",
      ).toBeVisible({ timeout: 10_000 });

      await transitionBtn.click({ force: true, timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.actionToolbar}"]`),
      ).toBeVisible();

      await expect
        .poll(() => transitionStatuses.length, { timeout: 10_000 })
        .toBeGreaterThan(0);
      const status = transitionStatuses[transitionStatuses.length - 1];
      expect(
        isHonestTransitionStatus(status),
        `workflow transition HTTP ${status} (expect 200, 4xx, or workflow 500 with error chrome)`,
      ).toBe(true);
      if (status !== 200) {
        await expect(
          page.locator('[data-testid="explorer-server-actions-error"]'),
        ).toBeVisible({ timeout: 10_000 });
      }

      if ((await folderRows.count()) > 0) {
        await folderRows.first().click({ force: true });
        await expect(
          page.locator(`[data-testid="${TEST_IDS.workflowGroup}"]`),
        ).toHaveCount(0, { timeout: 10_000 });
      } else {
        const sitesNode = page
          .locator(
            `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], ` +
              `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
          )
          .first();
        await sitesNode.click({ force: true });
        await listWaitReady(page);
        const siteFolders = list.locator(
          'tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]',
        );
        if ((await siteFolders.count()) > 0) {
          await siteFolders.first().click({ force: true });
          await expect(
            page.locator(`[data-testid="${TEST_IDS.workflowGroup}"]`),
          ).toHaveCount(0, { timeout: 10_000 });
        }
      }

      const unexpected = pageErrors.filter((text) => {
        if (
          /Failed to load resource: the server responded with a status of 500/i.test(
            text,
          ) &&
          transitionStatuses.includes(500)
        ) {
          return false;
        }
        return true;
      });
      expect(
        unexpected,
        `JS page/console errors: ${unexpected.join(" | ")}`,
      ).toEqual([]);
    },
  );
});
