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
 * Playwright surface: #3668 / #3639 / parent #2732 / #2400 — Explorer
 * Workflow transition perform HTTP 200 on H2.
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

const { test, expect } = require("@playwright/test");
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
  isSuccessfulTransitionStatus,
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
 * Open Sites → the site that owns {@code listed} → Pages when present.
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

  const wanted = new Set(
    listedPageSiteNames(listed).map((n) => foldSiteName(n)),
  );
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
    const nameMatch =
      wanted.size === 0 || detailRowMatchesFoldedSite(rowText, wanted);
    if (!nameMatch) continue;
    await openDetailFolderRow(row);
    await openPagesFolderIfPresent(page, list);

    const itemRows = list.locator(
      'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]',
    );
    const byName = listedName
      ? list
          .locator('tbody tr[data-testid^="detail-row-"]')
          .filter({ hasText: listedName })
      : itemRows;
    if ((await itemRows.count()) > 0 || (await byName.count()) > 0) {
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
    await openDetailFolderRow(siteRows.first());
    await openPagesFolderIfPresent(page, list);
  }
}

/**
 * Prefer the folder-icon open control (peer #3328); fall back to dblclick.
 * @param {import("@playwright/test").Locator} row
 */
async function openDetailFolderRow(row) {
  const icon = row.locator('[data-testid^="detail-folder-icon-"]');
  if ((await icon.count()) > 0) {
    await icon.click();
  } else {
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
      /Failed to load resource: the server responded with a status of (404|400|500)/i.test(
        text,
      )
    ) {
      // Chrome resource-status noise. Transition HTTP is asserted via
      // {@code transitionStatuses} (must be 200 — #3668).
      return;
    }
    bucket.push(text);
  });
}

test.describe("modern React Content Explorer - workflow transitions (#3668 / #3639 / #2732)", () => {
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

      const expireBtn = page.locator(
        `[data-testid="${TEST_IDS.workflowTransitionPrefix}Expire"]`,
      );
      const anyTransition = page.locator(
        `[data-testid^="${TEST_IDS.workflowTransitionPrefix}"]`,
      );
      const transitionBtn =
        (await expireBtn.count()) > 0 ? expireBtn.first() : anyTransition.first();
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
        isSuccessfulTransitionStatus(status),
        `listed workflow transition HTTP ${status} (expect 200; 500 Expire is #3668)`,
      ).toBe(true);
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);

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

      expect(
        pageErrors,
        `JS page/console errors: ${pageErrors.join(" | ")}`,
      ).toEqual([]);
    },
  );
});
