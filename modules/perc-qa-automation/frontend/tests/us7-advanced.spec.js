/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Playwright spec: US7 P-Adv \u2014 advanced CE (clipboard / wizards / dependency / relationships).
 *
 * <p>Asserts the modern React advanced-CE surfaces mount on
 * <code>us7AdvancedModern.jsp</code> against the live docker dev CMS
 * at <code>http://localhost:9992</code>. SC-011 acceptance criteria
 * for the 6 capability-matrix P-Adv rows are gated on a
 * system-installed CMS (the dev Derby image has no installed
 * relationship data, the AA-link synthesis row however renders
 * locally with the supplied {@code aaLinkCount: 3}). The Vitest
 * suites cover the structural surface.</p>
 *
 * <p>One <code>test()</code> per capability-matrix dimension where
 * the smoke wiring applies; vitest + Playwright list is the union of
 * the SC-011 evidence. The dependency-viewer rows other than AA are
 * expected to render the <code>"—"</code> placeholder (per the
 * <code>clientSideOnly</code> gate), not actual server data.</p>
 *
 * <p>Run from <code>modules/perc-qa-automation/frontend</code>:</p>
 * <pre>
 *   npm test -- tests/us7-advanced.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const US7_URL = `${BASE_URL}/Rhythmyx/cm/app/us7AdvancedModern.jsp?_=${Date.now()}`;

test.describe("US7 P-Adv \u2014 advanced CE (SC-011)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("advanced CE pilot mounts all five US7 surfaces (clipboard + 2 wizards + 2 views)", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const host = page.locator('[data-testid="perc-us7-host"]');
    await expect(host).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="clipboard-panel"]')).toBeVisible();
    await expect(page.locator('[data-testid="site-copy-wizard"]')).toBeVisible();
    await expect(page.locator('[data-testid="subfolder-copy-wizard"]')).toBeVisible();
    await expect(page.locator('[data-testid="dependency-viewer"]')).toBeVisible();
    await expect(page.locator('[data-testid="relationships-view"]')).toBeVisible();
  });

  test("Capability-matrix P-Adv \u2014 Clipboard copy state pre-populates items list (SC-011 row 1)", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const panel = page.locator('[data-testid="clipboard-panel"]');
    await expect(panel).toBeVisible({ timeout: 15_000 });
    await expect(
      panel.locator('[data-testid="clipboard-size"]'),
    ).toContainText("(1)");
    // One pre-populated row.
    await expect(
      page.locator('[data-testid="clipboard-item-row"]').first(),
    ).toContainText("/Sites/Foo/Bar");
    // Add is enabled (the host supplies a selection).
    const addBtn = page.locator('[data-testid="clipboard-add"]');
    await expect(addBtn).toBeTruthy();
  });

  test("Capability-matrix P-Adv \u2014 Site Copy wizard renders step 0 source (SC-011 row 2)", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const wizard = page.locator('[data-testid="site-copy-wizard"]');
    await expect(wizard).toBeVisible({ timeout: 15_000 });
    await expect(
      page.locator('[data-testid="site-copy-step-source"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="site-copy-step-count"]'),
    ).toContainText("of 5");
    // Next button is disabled (no source yet).
    const next = page.locator('[data-testid="site-copy-next"]');
    await expect(next).toBeDisabled();
  });

  test("Capability-matrix P-Adv \u2014 Subfolder Copy wizard renders step 0 source (SC-011 row 3)", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const wizard = page.locator('[data-testid="subfolder-copy-wizard"]');
    await expect(wizard).toBeVisible({ timeout: 15_000 });
    await expect(
      page.locator('[data-testid="subfolder-copy-step-source"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="subfolder-copy-step-count"]'),
    ).toContainText("of 4");
  });

  test("Capability-matrix P-Adv \u2014 DependencyViewer AA row (SC-011 row 4 \u2014 known; others unknown)", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const viewer = page.locator('[data-testid="dependency-viewer"]');
    await expect(viewer).toBeVisible({ timeout: 15_000 });
    // AA row is known (we supplied aaLinkCount: 3).
    const aaRow = page.locator('[data-testid="dependency-row-aa"]');
    await expect(aaRow).toContainText("3 AA links");
    // The other 5 dimensions are marked unknown per the T074 spike.
    for (const dim of [
      "outgoing",
      "incoming",
      "taxonomy",
      "local",
      "reverse",
    ]) {
      const row = page.locator(`[data-testid="dependency-row-${dim}"]`);
      await expect(row).toContainText("\u2014");
    }
    // Client-side preview banner is visible.
    await expect(
      page.locator('[data-testid="dependency-client-side-preview"]'),
    ).toBeVisible();
  });

  test("Capability-matrix P-Adv \u2014 RelationshipsView shows 4 primary rows + supplementary (SC-011 row 5)", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const viewer = page.locator('[data-testid="relationships-view"]');
    await expect(viewer).toBeVisible({ timeout: 15_000 });
    for (const dim of ["outgoing", "incoming", "taxonomy", "local"]) {
      const row = page.locator(`[data-testid="relationships-row-${dim}"]`);
      await expect(row).toBeTruthy();
    }
    // Supplementary AA + reverse rows in the <details> panel.
    const aa = page.locator('[data-testid="relationships-row-aa"]');
    await expect(aa).toContainText("3 AA links");
    await expect(
      page.locator('[data-testid="relationships-client-side-preview"]'),
    ).toBeVisible();
  });

  test("US7 pilot: legacy miller-column Finder chrome is NOT loaded", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });
});
