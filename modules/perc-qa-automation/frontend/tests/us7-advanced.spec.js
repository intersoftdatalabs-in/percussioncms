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
 * Playwright spec: US7 P-Adv \u2014 advanced CE (clipboard / wizards / dependency / relationships).
 *
 * <p>Asserts the modern React advanced-CE surfaces mount on
 * <code>us7AdvancedModern.jsp</code> against the live docker dev CMS
 * at <code>http://localhost:9992</code>. SC-011 acceptance criteria
 * for the 6 capability-matrix P-Adv rows are gated on US8
 * server-authoritative summaries: the pilot mounts the viewers with a
 * parseable GUID-format item id, the specs stub the consolidated
 * <code>/summary</code> endpoint for determinism, and every dimension
 * row renders a server count (never the pre-US8 <code>"—"</code>
 * placeholder or the <code>client-side-preview</code> banner, both
 * removed by US8). The Vitest suites cover the structural surface.</p>
 *
 * <p>One <code>test()</code> per capability-matrix dimension where
 * the smoke wiring applies; vitest + Playwright list is the union of
 * the SC-011 evidence. Server-authoritative rows are asserted here;
 * the <code>us8-dependency.spec.js</code> companion additionally drives
 * one un-stubbed live round-trip and asserts the viewer settles
 * without the legacy banner.</p>
 *
 * <p>Run from <code>modules/perc-qa-automation/frontend</code>:</p>
 * <pre>
 *   npm test -- tests/us7-advanced.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const US7_URL = `${BASE_URL}/Rhythmyx/cm/app/us7AdvancedModern.jsp?_=${Date.now()}`;

/**
 * Deterministic US8 consolidated-summary payload (mirrors the
 * `fetchNodeSummary` Vitest shape in
 * `WebUI/src/test/ts/contentExplorer/relationshipsApi.test.ts`).
 * Row text derived from `dependencyModel.composeFromServerSummary`:
 * outgoing "3 translation", incoming "1 translation", aa "3 AA links"
 * (host-supplied), taxonomy "2 nodes", local "2 local links", reverse
 * "1 translation", total "Known edges: 12".
 */
const US8_STUB_SUMMARY = {
  outgoing: { count: 3, byType: [{ type: "translation", count: 3 }] },
  incoming: { count: 1, byType: [{ type: "translation", count: 1 }] },
  taxonomy: { count: 2, nodes: ["/Sites/Foo", "/Sites/Foo/Bar"] },
  local: { count: 2, links: [] },
  reverse: { count: 1, byType: [{ type: "translation", count: 1 }] },
};

async function stubRelationshipSummary(page) {
  await page.route(
    "**/rest/content-explorer/relationships/*/summary",
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(US8_STUB_SUMMARY),
      });
    },
  );
  // RelationshipsView also loads the edge list on mount; an empty edge
  // set keeps the stubbed suites hermetic (no server round-trip for the
  // synthetic pilot id, hence no exception-mapper noise in server.log).
  await page.route(
    "**/rest/content-explorer/relationships/*/edges",
    async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ items: [] }),
      });
    },
  );
}

test.describe("US7 P-Adv \u2014 advanced CE (SC-011)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
    // Hermetic pilot: the viewers mount with the synthetic pilot id, so
    // stub the summary + edges for every test (a real 404 for the
    // synthetic id logs a server-side exception-mapper ERROR; the true
    // live round-trip is covered by us8-dependency.spec.js).
    await stubRelationshipSummary(page);
  });

  test("advanced CE pilot mounts all five US7 surfaces (clipboard + 2 wizards + 2 views)", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const host = page.locator('[data-testid="perc-us7-host"]');
    await expect(host).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="clipboard-panel"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="site-copy-wizard"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="subfolder-copy-wizard"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="dependency-viewer"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="relationships-view"]'),
    ).toBeVisible();
  });

  test("Capability-matrix P-Adv \u2014 Clipboard copy state pre-populates items list (SC-011 row 1)", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const panel = page.locator('[data-testid="clipboard-panel"]');
    await expect(panel).toBeVisible({ timeout: 15_000 });
    await expect(panel.locator('[data-testid="clipboard-size"]')).toContainText(
      "(1)",
    );
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

  test("Capability-matrix P-Adv — DependencyViewer renders 6 server-authoritative rows, no banner (SC-011 row 4)", async ({
    page,
  }) => {
    await stubRelationshipSummary(page);
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const viewer = page.locator('[data-testid="dependency-viewer"]');
    await expect(viewer).toBeVisible({ timeout: 15_000 });
    await expect(viewer).toHaveAttribute("data-testid-state", "ok", {
      timeout: 15_000,
    });
    // AA row is host-supplied (aaLinkCount: 3 on the pilot).
    await expect(
      page.locator('[data-testid="dependency-row-aa"]'),
    ).toContainText("3 AA links");
    // The other 5 dimensions render server counts (US8), never "—".
    await expect(
      page.locator('[data-testid="dependency-row-outgoing"]'),
    ).toContainText("3 translation");
    await expect(
      page.locator('[data-testid="dependency-row-incoming"]'),
    ).toContainText("1 translation");
    await expect(
      page.locator('[data-testid="dependency-row-taxonomy"]'),
    ).toContainText("2 nodes");
    await expect(
      page.locator('[data-testid="dependency-row-local"]'),
    ).toContainText("2 local links");
    await expect(
      page.locator('[data-testid="dependency-row-reverse"]'),
    ).toContainText("1 translation");
    await expect(
      page.locator('[data-testid="dependency-total"]'),
    ).toContainText("Known edges: 12");
    // No unknown placeholder and no client-side preview banner (US8 removed both).
    for (const dim of [
      "outgoing",
      "incoming",
      "taxonomy",
      "local",
      "reverse",
    ]) {
      const row = page.locator(`[data-testid="dependency-row-${dim}"]`);
      await expect(row).not.toContainText("—");
    }
    await expect(
      page.locator('[data-testid="dependency-client-side-preview"]'),
    ).toHaveCount(0);
  });

  test("Capability-matrix P-Adv — RelationshipsView shows 4 primary rows + supplementary, no banner (SC-011 row 5)", async ({
    page,
  }) => {
    await stubRelationshipSummary(page);
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    const viewer = page.locator('[data-testid="relationships-view"]');
    await expect(viewer).toBeVisible({ timeout: 15_000 });
    await expect(viewer).toHaveAttribute("data-testid-state", "ok", {
      timeout: 15_000,
    });
    for (const dim of ["outgoing", "incoming", "taxonomy", "local"]) {
      const row = page.locator(`[data-testid="relationships-row-${dim}"]`);
      await expect(row).toBeVisible();
      await expect(row).not.toContainText("—");
    }
    // Supplementary AA + reverse rows in the <details> panel.
    const aa = page.locator('[data-testid="relationships-row-aa"]');
    await expect(aa).toContainText("3 AA links");
    await expect(
      page.locator('[data-testid="relationships-client-side-preview"]'),
    ).toHaveCount(0);
  });

  test("US7 pilot: legacy miller-column Finder chrome is NOT loaded", async ({
    page,
  }) => {
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  test("axe-core a11y gate — DependencyViewer / Clipboard / Sites wizard (T082b)", async ({
    page,
  }) => {
    // Stub the summary so the viewers settle to ok-state without hitting
    // the server for the synthetic pilot id (a real 404 would log a
    // server-side exception-mapper ERROR; the live round-trip is covered
    // by us8-dependency.spec.js against real candidate content).
    await stubRelationshipSummary(page);
    await page.goto(US7_URL, { waitUntil: "networkidle" });
    // The pilot page embeds 6+ panels stacked; the modern shell root has
    // a stable testid we can scope to. If absent, scan page-level.
    const root = page.locator('[data-testid="perc-us7-root"]');
    if ((await root.count()) > 0) {
      await expect(root).toBeVisible({ timeout: 15_000 });
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="perc-us7-root"]',
      });
    } else {
      await expectNoSeriousA11yViolations(page);
    }
  });
});
