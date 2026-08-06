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
 * Residual automation: Sample Site(s) visible under Sites after demo install
 * (GH-1750 / residual #2194; product seed fix #2192).
 *
 * <p>Peers of {@code bug-1622-explorer-root-folders.spec.js}: REST
 * pathmanagement {@code path/folder/Sites} plus modern Explorer tree UI.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: Sites children include Corporate / Enterprise Investments</li>
 *   <li>UI: expand Sites in Content Explorer; sample site tree nodes present</li>
 * </ul>
 *
 * <p><strong>Env gate</strong> — default H2 {@code qa-up} images often omit
 * demo-sites seed (or lack #2192 until merge). Without enforcement:</p>
 * <ul>
 *   <li>If Sites is empty / missing samples → {@code test.skip} with BUG +
 *       durable issue URL (skip-with-BUG; not a silent flake)</li>
 *   <li>If samples already present → hard assert (green)</li>
 * </ul>
 * <p>Set {@code EXPECT_DEMO_SITES=1} (alias {@code TEST_EXPECT_DEMO_SITES})
 * after a demo-sites install so empty Sites <strong>fails</strong> (original
 * #1750 regression shape).</p>
 *
 * <p>Recipe (after #2192 is in the image / installer under test):</p>
 * <pre>
 *   # Silent H2 install with sample sites (product modules / distribution)
 *   java -jar &lt;installer&gt;.jar --install-dir=&lt;path&gt; --silent --db.type=h2 --demo-sites
 *   # or interactive wizard: Yes to Install sample sites …
 *
 *   # Bring CMS up (host install or Docker cell that uses that install)
 *   # Then Playwright QA mode:
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \\
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=... \\
 *     EXPECT_DEMO_SITES=1 \\
 *     npm test -- tests/bugs/bug-1750-demo-sites-sample-site.spec.js
 *
 *   # Optional: standard H2 stack without demo-sites — soft skip when empty:
 *   python docker/scripts/perc-devctl.py qa-up
 *   TEST_CMS_URL=… ADMIN_PASSWORD=… npm test -- tests/bugs/bug-1750-demo-sites-sample-site.spec.js
 * </pre>
 *
 * @see helpers/demo-sites.js
 * @see docs/ai-generated/issue-2191-demo-sites-empty-sites-repro.md
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("../helpers/auth");
const {
  EXPECTED_SAMPLE_SITE_NAMES,
  pathItemNames,
  hasAllExpectedSampleSites,
  hasAnyExpectedSampleSite,
  shouldEnforceDemoSites,
  demoSitesSkipReason,
  normalizeSiteName,
} = require("../helpers/demo-sites");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
const PATH_FOLDER = `${BASE_URL}/Rhythmyx/services/pathmanagement/path/folder`;

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<string[]>}
 */
async function fetchSitesChildNames(request) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(`${PATH_FOLDER}/Sites`, { headers });
  expect(
    res.status(),
    `GET ${PATH_FOLDER}/Sites must be 200 (path valid even when empty)`,
  ).toBe(200);
  const body = await res.json();
  return pathItemNames(body);
}

/**
 * Soft gate: skip with BUG when samples missing and not enforcing.
 * Hard fail when EXPECT_DEMO_SITES=1 (regression gate).
 *
 * @param {string[]} names
 */
function gateSampleSitesOrSkip(names) {
  if (hasAllExpectedSampleSites(names)) {
    return;
  }
  // Empty / no samples: stock non-demo or pre-#2192 image — skip-with-BUG
  // unless operator set EXPECT_DEMO_SITES=1 (hard regression gate).
  if (!shouldEnforceDemoSites() && !hasAnyExpectedSampleSite(names)) {
    // Message-only skip so the BUG reason is the reported description (not "true").
    test.skip(demoSitesSkipReason());
    return;
  }
  expect(
    names,
    `Sites must list sample sites after demo-sites install; got [${names.join(", ")}]. ` +
      `Expected: ${EXPECTED_SAMPLE_SITE_NAMES.join(", ")}`,
  ).not.toEqual([]);
  expect(
    hasAllExpectedSampleSites(names),
    `Sites children missing Corporate/Enterprise Investments; got [${names.join(", ")}]`,
  ).toBe(true);
}

test.describe("GH-1750 demo-sites Sample Site under Sites (#2194 residual)", () => {
  test("REST: path/folder/Sites lists Corporate + Enterprise Investments", async ({
    request,
  }) => {
    test.setTimeout(30_000);
    const names = await fetchSitesChildNames(request);
    gateSampleSitesOrSkip(names);

    for (const expected of EXPECTED_SAMPLE_SITE_NAMES) {
      const hit = names.some(
        (n) => normalizeSiteName(n) === normalizeSiteName(expected),
      );
      expect(
        hit,
        `Sites should include ${expected}; got [${names.join(", ")}]`,
      ).toBe(true);
    }
  });

  test("UI: Content Explorer Sites expands to sample site nodes", async ({
    page,
  }) => {
    test.setTimeout(90_000);

    // REST probe first — same soft/hard gate as pure API test (cheap skip).
    const probe = await page.request.get(`${PATH_FOLDER}/Sites`, {
      headers: adminBasicAuthHeaders(),
    });
    expect(probe.status()).toBe(200);
    const restNames = pathItemNames(await probe.json());
    gateSampleSitesOrSkip(restNames);

    await loginAsAdmin(page);
    await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });

    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 20_000 });

    const tree = page.locator('[data-testid="explorer-tree"]');
    await expect(tree).toBeVisible({ timeout: 15_000 });

    const treeErr = page.locator(
      '[data-testid="explorer-tree-error"], [data-testid="explorer-tree"] [role="alert"]',
    );
    if ((await treeErr.count()) > 0 && (await treeErr.first().isVisible())) {
      const text = await treeErr.first().innerText();
      throw new Error(`Explorer tree failed to load: ${text}`);
    }

    // Root Sites node only — exact testids (avoid *= which can match SitesArchive).
    // Peers: bug-1622 / bug-2094 use tree-node-/Sites[/].
    const sitesRoot = tree.locator(
      '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"]',
    );
    await expect(sitesRoot.first()).toBeVisible({ timeout: 20_000 });
    await sitesRoot.first().click();

    // Wait for at least one child under Sites (sample sites or nested folders).
    const childUnderSites = tree.locator(
      '[data-testid^="tree-node-/Sites/"]:not([data-testid="tree-node-/Sites/"])',
    );
    await expect(childUnderSites.first()).toBeVisible({ timeout: 20_000 });

    const nodeTestIds = await tree
      .locator('[data-testid^="tree-node-"]')
      .evaluateAll((els) =>
        els.map((el) => el.getAttribute("data-testid") || ""),
      );

    // Immediate children of /Sites only: tree-node-/Sites/<name>[/...]
    const sitesChildNames = nodeTestIds
      .map((id) => {
        const m = /^tree-node-\/Sites\/([^/]+)/.exec(id);
        return m ? m[1] : null;
      })
      .filter((n) => typeof n === "string" && n.length > 0);

    // REST already gated samples; UI tree should expose the same set (exact path segment).
    expect(
      hasAllExpectedSampleSites(sitesChildNames),
      `explorer tree under /Sites missing sample sites; children=${JSON.stringify(sitesChildNames)}; testids=${JSON.stringify(nodeTestIds)}`,
    ).toBe(true);

    for (const expected of EXPECTED_SAMPLE_SITE_NAMES) {
      const hasNode = sitesChildNames.some(
        (n) => normalizeSiteName(n) === normalizeSiteName(expected),
      );
      expect(
        hasNode,
        `expected explorer tree node for ${expected}; children=${JSON.stringify(sitesChildNames)}`,
      ).toBe(true);
    }

    // Belt-and-suspenders: REST already had samples; UI must not show empty Sites.
    const childCount = await childUnderSites.count();
    expect(
      childCount,
      "Sites expansion should show sample site children after demo-sites seed",
    ).toBeGreaterThan(0);
  });
});
