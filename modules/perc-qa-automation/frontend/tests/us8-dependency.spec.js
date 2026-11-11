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
 * Playwright spec: US8 dependency API surface (T095) — 8.2 parity gate evidence.
 *
 * <p>Drives the DependencyViewer / RelationshipsView on
 * <code>us7AdvancedModern.jsp</code> (which mounts both viewers with the
 * parseable pilot item <code>1-101-708</code> + <code>aaLinkCount: 3</code>).
 * The deterministic tests stub the consolidated
 * <code>/rest/content-explorer/relationships/{id}/summary</code> endpoint
 * with a server-authoritative payload and assert all 6 dimension rows
 * render counts — never the pre-US8 <code>"—"</code> placeholder and never
 * the removed <code>client-side-preview</code> banner. The live test drops
 * the stub so the React component performs a real server round-trip and
 * asserts the viewer settles (ok / auth / missing / error) with no legacy
 * banner; full per-dimension live assertions run on candidate builds whose
 * sample content carries relationship data.</p>
 *
 * <p>Run from <code>modules/perc-qa-automation/frontend</code>:</p>
 * <pre>
 *   npm test -- tests/us8-dependency.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const US8_URL = `${BASE_URL}/Rhythmyx/cm/app/us7AdvancedModern.jsp?_=${Date.now()}`;

/** Server-authoritative stub (same contract as `us7-advanced.spec.js`). */
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

test.describe("US8 dependency API surface (T095)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("DependencyViewer renders all 6 dimensions authoritatively, no banner", async ({
    page,
  }) => {
    await stubRelationshipSummary(page);
    await page.goto(US8_URL, { waitUntil: "networkidle" });
    const viewer = page.locator('[data-testid="dependency-viewer"]');
    await expect(viewer).toBeVisible({ timeout: 15_000 });
    await expect(viewer).toHaveAttribute("data-testid-state", "ok", {
      timeout: 15_000,
    });
    for (const dim of [
      "outgoing",
      "incoming",
      "aa",
      "taxonomy",
      "local",
      "reverse",
    ]) {
      const row = page.locator(`[data-testid="dependency-row-${dim}"]`);
      await expect(row).toBeVisible();
      await expect(row).not.toContainText("—");
    }
    await expect(
      page.locator('[data-testid="dependency-total"]'),
    ).toContainText("Known edges: 12");
    await expect(
      page.locator('[data-testid="dependency-client-side-preview"]'),
    ).toHaveCount(0);
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="dependency-viewer"]',
    });
  });

  test("RelationshipsView renders primary + supplementary rows authoritatively, no banner", async ({
    page,
  }) => {
    await stubRelationshipSummary(page);
    await page.goto(US8_URL, { waitUntil: "networkidle" });
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
    await expect(
      page.locator('[data-testid="relationships-client-side-preview"]'),
    ).toHaveCount(0);
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="relationships-view"]',
    });
  });

  test("live server round-trip renders authoritative counts, no legacy banner", async ({
    page,
  }) => {
    // No stub: the viewers call the real consolidated summary endpoint
    // for candidate content id 1 (fresh H2 install: all dimensions 0 —
    // the T095 '"0 (no links)" (NOT "—")' shape). Exercises the true
    // client ↔ server wire contract, including the @JsonRootName
    // envelope the JAX-RS resource emits.
    await page.goto(`${US8_URL}&itemId=1-101-1`, {
      waitUntil: "networkidle",
    });
    const viewer = page.locator('[data-testid="dependency-viewer"]');
    await expect(viewer).toBeVisible({ timeout: 15_000 });
    // The viewer must leave `loading` for the settled ok state with
    // server-authoritative (possibly zero) counts.
    await expect(viewer).toHaveAttribute("data-testid-state", "ok", {
      timeout: 30_000,
    });
    for (const dim of [
      "outgoing",
      "incoming",
      "aa",
      "taxonomy",
      "local",
      "reverse",
    ]) {
      const row = page.locator(`[data-testid="dependency-row-${dim}"]`);
      await expect(row).toBeVisible();
      await expect(row).not.toContainText("—");
    }
    await expect(
      page.locator('[data-testid="dependency-client-side-preview"]'),
    ).toHaveCount(0);
    await expect(
      page.locator('[data-testid="relationships-client-side-preview"]'),
    ).toHaveCount(0);
  });
});
