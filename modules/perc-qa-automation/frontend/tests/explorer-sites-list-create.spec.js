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
 * Playwright surface: #3003 / parent #2989 — Explorer Sites list + Create Site.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: {@code path/folder/Sites} is 200; non-empty when fixture has sites</li>
 *   <li>REST: expanding a sample site (folderPath / sitename) lists children (#3326)</li>
 *   <li>UI: Sites tree expands to child site nodes when list is non-empty</li>
 *   <li>UI: selecting a sample site shows folder children, not LIST_EMPTY (#3326)</li>
 *   <li>Create Site: Content menu always exposes Create Site; wizard details →
 *       template → confirm chrome; optional live submit when affordance present</li>
 * </ul>
 *
 * <p><strong>Soft-skip policy (acceptance #3003):</strong> empty Sites list may
 * soft-skip <em>list</em> assertions only when Create Site path is covered in this
 * surface. Set {@code EXPECT_DEMO_SITES=1} to hard-fail empty sample Sites
 * (peers {@code bug-1750}). Create Site missing (pre-#3002 image) skips with
 * BUG + durable issue URL.</p>
 *
 * <p>Tags: {@code @explorer-sites-list-create} {@code @explorer} {@code @sites}
 * {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-sites-list-create.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const {
  pathItemNames,
  hasAnyExpectedSampleSite,
  hasAllExpectedSampleSites,
  shouldEnforceDemoSites,
  EXPECTED_SAMPLE_SITE_NAMES,
} = require("./helpers/demo-sites");
const {
  TEST_IDS,
  explorerSpaUrl,
  sitesFolderUrl,
  siteChildListPath,
  folderChildrenUrl,
  openContentMenu,
  sitesTreeRootLocator,
  sitesTreeDescendantsLocator,
  siteChildNamesFromTreeTestIds,
  uniqueQaSiteName,
  createSiteMissingSkipReason,
  emptySitesSoftSkipNote,
} = require("./helpers/explorer-sites-list-create");

const SITES_URL = sitesFolderUrl(BASE_URL);

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<string[]>}
 */
async function fetchSitesChildNames(request) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(SITES_URL, { headers });
  expect(
    res.status(),
    `GET ${SITES_URL} must be 200 (path valid even when empty)`,
  ).toBe(200);
  return pathItemNames(await res.json());
}

/**
 * Soft-skip list assertions when Sites empty (create path still covered elsewhere).
 * Hard-fail empty when EXPECT_DEMO_SITES=1 (demo seed regression gate).
 *
 * @param {string[]} names
 * @returns {"ok" | "soft-empty"}
 */
function gateSitesListOrSoftSkip(names) {
  if ((names || []).length > 0) {
    return "ok";
  }
  if (shouldEnforceDemoSites()) {
    // Hard regression: demo seed expected.
    expect(
      names,
      `Sites must list children after demo-sites install; expected samples: ${EXPECTED_SAMPLE_SITE_NAMES.join(", ")}`,
    ).not.toEqual([]);
    return "ok";
  }
  test.info().annotations.push({
    type: "soft-skip",
    description: emptySitesSoftSkipNote(),
  });
  return "soft-empty";
}

/**
 * Skip create path when #3002 UI is not deployed in the image under test.
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<boolean>} true when Create Site menu is present
 */
async function ensureCreateSiteMenuOrSkip(page) {
  await openContentMenu(page);
  const menuItem = page.locator(`[data-testid="${TEST_IDS.createSiteMenu}"]`);
  if ((await menuItem.count()) === 0) {
    test.skip(createSiteMissingSkipReason());
    return false;
  }
  await expect(menuItem).toBeVisible({ timeout: 5_000 });
  return true;
}

test.describe("Explorer Sites list + Create Site (#3003 / #2989)", () => {
  test(
    "REST: path/folder/Sites returns 200 and lists sites when seeded",
    { tag: ["@explorer-sites-list-create", "@explorer", "@sites", "@smoke"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const names = await fetchSitesChildNames(request);
      const gate = gateSitesListOrSoftSkip(names);
      if (gate === "soft-empty") {
        // Soft-skip list content only — create path covered by other tests.
        return;
      }
      expect(names.length, `Sites children: ${JSON.stringify(names)}`).toBeGreaterThan(
        0,
      );
      // When stock demo samples are present, assert both; otherwise any non-empty is OK.
      if (hasAnyExpectedSampleSite(names)) {
        expect(
          hasAllExpectedSampleSites(names),
          `partial sample set under Sites: ${JSON.stringify(names)}`,
        ).toBe(true);
      }
    },
  );

  test(
    "REST: sample site folder children are non-empty (#3326)",
    { tag: ["@explorer-sites-list-create", "@explorer", "@sites", "@smoke"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const headers = adminBasicAuthHeaders();
      const sitesRes = await request.get(SITES_URL, { headers });
      expect(sitesRes.status()).toBe(200);
      const body = await sitesRes.json();
      const items = Array.isArray(body.PathItem)
        ? body.PathItem
        : Array.isArray(body)
          ? body
          : [];
      const names = pathItemNames(body);
      if (gateSitesListOrSoftSkip(names) === "soft-empty") {
        return;
      }
      const sample =
        items.find((it) =>
          hasAnyExpectedSampleSite([
            it && typeof it.name === "string" ? it.name : "",
          ]),
        ) || items[0];
      expect(sample, "expected at least one site PathItem").toBeTruthy();
      const listPath = siteChildListPath(sample);
      const childUrl = folderChildrenUrl(BASE_URL, listPath);
      const childRes = await request.get(childUrl, { headers });
      expect(childRes.status(), `GET ${childUrl}`).toBe(200);
      const childNames = pathItemNames(await childRes.json());
      expect(
        childNames.length,
        `site children at ${childUrl}: ${JSON.stringify(childNames)}`,
      ).toBeGreaterThan(0);
    },
  );

  test(
    "UI: Content Explorer Sites expands to child site nodes when non-empty",
    { tag: ["@explorer-sites-list-create", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(90_000);

      const probe = await page.request.get(SITES_URL, {
        headers: adminBasicAuthHeaders(),
      });
      expect(probe.status()).toBe(200);
      const restNames = pathItemNames(await probe.json());
      if (gateSitesListOrSoftSkip(restNames) === "soft-empty") {
        return;
      }

      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });
      const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
      await expect(tree).toBeVisible({ timeout: 15_000 });

      const sitesRoot = sitesTreeRootLocator(page);
      await expect(sitesRoot.first()).toBeVisible({ timeout: 20_000 });
      await sitesRoot.first().click();

      const descendants = sitesTreeDescendantsLocator(page);
      await expect(descendants.first()).toBeVisible({ timeout: 20_000 });

      const nodeTestIds = await tree
        .locator('[data-testid^="tree-node-"]')
        .evaluateAll((els) =>
          els.map((el) => el.getAttribute("data-testid") || ""),
        );
      const childNames = siteChildNamesFromTreeTestIds(nodeTestIds);
      expect(
        childNames.length,
        `tree under /Sites should list children; testids=${JSON.stringify(nodeTestIds)}`,
      ).toBeGreaterThan(0);

      if (hasAnyExpectedSampleSite(restNames)) {
        expect(
          hasAllExpectedSampleSites(childNames),
          `explorer tree missing sample sites; children=${JSON.stringify(childNames)}`,
        ).toBe(true);
      }
    },
  );

  test(
    "UI: expanding a sample site shows folder children not LIST_EMPTY (#3326)",
    { tag: ["@explorer-sites-list-create", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const probe = await page.request.get(SITES_URL, {
        headers: adminBasicAuthHeaders(),
      });
      expect(probe.status()).toBe(200);
      const restNames = pathItemNames(await probe.json());
      if (gateSitesListOrSoftSkip(restNames) === "soft-empty") {
        return;
      }

      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });

      const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
      await expect(tree).toBeVisible({ timeout: 20_000 });
      const sitesRoot = sitesTreeRootLocator(page);
      await expect(sitesRoot.first()).toBeVisible({ timeout: 20_000 });
      await sitesRoot.first().click();

      const descendants = sitesTreeDescendantsLocator(page);
      await expect(descendants.first()).toBeVisible({ timeout: 20_000 });
      await descendants.first().locator('[role="treeitem"]').first().click();

      const detail = page.locator(`[data-testid="${TEST_IDS.detailList}"]`);
      await expect(detail).toBeVisible({ timeout: 15_000 });
      await expect(detail.locator('[data-testid="detail-list-empty"]')).toHaveCount(
        0,
        { timeout: 20_000 },
      );
      await expect(detail.locator('[data-testid^="detail-row-"]').first()).toBeVisible({
        timeout: 20_000,
      });
      expect(jsErrors, `console/page errors: ${jsErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Content menu exposes Create Site (always enabled; no site context)",
    { tag: ["@explorer-sites-list-create", "@explorer", "@sites", "@smoke"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });

      const present = await ensureCreateSiteMenuOrSkip(page);
      if (!present) {
        return;
      }

      const menuItem = page.locator(
        `[data-testid="${TEST_IDS.createSiteMenu}"]`,
      );
      await expect(menuItem).toHaveAttribute("role", "menuitemcheckbox");
      // Create Site does not require site context (#3002) — must be enabled at root.
      await expect(menuItem).toBeEnabled();
    },
  );

  test(
    "Create Site wizard: details → template → confirm chrome",
    { tag: ["@explorer-sites-list-create", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });

      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });

      const present = await ensureCreateSiteMenuOrSkip(page);
      if (!present) {
        return;
      }

      await page.locator(`[data-testid="${TEST_IDS.createSiteMenu}"]`).click();
      const panel = page.locator(`[data-testid="${TEST_IDS.createSitePanel}"]`);
      const wizard = page.locator(`[data-testid="${TEST_IDS.wizard}"]`);
      await expect(panel.or(wizard)).toBeVisible({ timeout: 10_000 });
      await expect(wizard).toBeVisible({ timeout: 10_000 });

      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepDetails}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.traditionalNote}"]`),
      ).toBeVisible();
      const managedNav = page.locator(
        `[data-testid="${TEST_IDS.managedNav}"]`,
      );
      await expect(managedNav).toBeVisible();
      await expect(managedNav).toBeChecked();
      await managedNav.uncheck();

      const siteName = uniqueQaSiteName("QaListCreate");
      await page.locator(`[data-testid="${TEST_IDS.siteName}"]`).fill(siteName);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.templateName}"]`),
      ).not.toHaveValue("");

      const next = page.locator(`[data-testid="${TEST_IDS.next}"]`);
      await expect(next).toBeEnabled({ timeout: 5_000 });
      await next.click();

      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepTemplate}"]`),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.baseTemplate}"]`),
      ).toBeVisible({ timeout: 15_000 });

      await next.click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepConfirm}"]`),
      ).toBeVisible({ timeout: 10_000 });
      const summary = page.locator(
        `[data-testid="${TEST_IDS.confirmSummary}"]`,
      );
      await expect(summary).toBeVisible();
      await expect(summary).toContainText(siteName);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmManagedNav}"]`),
      ).toContainText(/No/i);

      // Do not submit here — live create is the next test (avoids double create).
      await page.locator(`[data-testid="${TEST_IDS.cancel}"]`).click();
    },
  );

  test(
    "Create Site happy path: submit traditional site and open under /Sites",
    { tag: ["@explorer-sites-list-create", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });

      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });

      const present = await ensureCreateSiteMenuOrSkip(page);
      if (!present) {
        return;
      }

      await page.locator(`[data-testid="${TEST_IDS.createSiteMenu}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.wizard}"]`),
      ).toBeVisible({ timeout: 10_000 });

      const siteName = uniqueQaSiteName("QaCreate");
      await page.locator(`[data-testid="${TEST_IDS.siteName}"]`).fill(siteName);

      const next = page.locator(`[data-testid="${TEST_IDS.next}"]`);
      await expect(next).toBeEnabled({ timeout: 5_000 });
      await next.click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepTemplate}"]`),
      ).toBeVisible({ timeout: 10_000 });
      // Wait for base template control (list load or fallback input).
      await expect(
        page.locator(`[data-testid="${TEST_IDS.baseTemplate}"]`),
      ).toBeVisible({ timeout: 20_000 });
      await next.click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepConfirm}"]`),
      ).toBeVisible({ timeout: 10_000 });

      const run = page.locator(`[data-testid="${TEST_IDS.run}"]`);
      const createByName = page.getByRole("button", { name: /^create site$/i });
      const nextOnConfirm = page.locator(`[data-testid="${TEST_IDS.next}"]`);
      if ((await run.count()) > 0) {
        await expect(run).toBeEnabled({ timeout: 5_000 });
        await run.click();
      } else if ((await createByName.count()) > 0) {
        await expect(createByName).toBeEnabled({ timeout: 5_000 });
        await createByName.click();
      } else {
        // Older wizard: Confirm is step 3/4; Next opens Progress, then Create site.
        await expect(nextOnConfirm).toBeEnabled({ timeout: 5_000 });
        await nextOnConfirm.click();
        const createAfterNext = page.getByRole("button", {
          name: /^create site$/i,
        });
        await expect(createAfterNext).toBeEnabled({ timeout: 10_000 });
        await createAfterNext.click();
      }

      const progress = page.locator(
        `[data-testid="${TEST_IDS.stepProgress}"]`,
      );
      if ((await progress.count()) > 0) {
        await expect(progress).toBeVisible({ timeout: 10_000 });
      }
      // Success: site folder exists under /Sites (REST). Wizard panel
      // testids differ by WebUI vintage; do not treat a missing panel
      // id as success. HTTP 500 on create is the #3364 failure.
      await expect
        .poll(
          async () => {
            const errText = await page
              .locator(`[data-testid="${TEST_IDS.wizard}"] [role="status"]`)
              .allInnerTexts()
              .catch(() => []);
            const failed = errText.some((t) =>
              /error|failed|invalid|500|rolled back/i.test(String(t || "")),
            );
            if (failed) {
              return "error";
            }
            const names = await fetchSitesChildNames(page.request);
            const hit = names.some(
              (n) => String(n).toLowerCase() === siteName.toLowerCase(),
            );
            return hit ? "ok" : "pending";
          },
          { timeout: 60_000 },
        )
        .toBe("ok");

      // REST: site folder should now exist under Sites.
      const after = await fetchSitesChildNames(page.request);
      const hit = after.some(
        (n) => String(n).toLowerCase() === siteName.toLowerCase(),
      );
      expect(
        hit,
        `Created site ${siteName} should appear under path/folder/Sites; got ${JSON.stringify(after)}`,
      ).toBe(true);

      // Tree/list: selection path under /Sites/<name> when panel closed.
      const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
      if ((await tree.count()) > 0) {
        const pathSeg = siteName.replace(/\s+/g, " ");
        const siteNode = page.locator(
          `[data-testid="tree-node-/Sites/${pathSeg}/"], ` +
            `[data-testid="tree-node-/Sites/${pathSeg}"], ` +
            `[data-testid*="tree-node-/Sites/"][data-testid*="${pathSeg}"]`,
        );
        // Soft: navigation may use encoded path variants; REST assert is primary.
        if ((await siteNode.count()) > 0) {
          await expect(siteNode.first()).toBeVisible({ timeout: 15_000 });
        }
      }
    },
  );
});
